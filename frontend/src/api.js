const TOKEN_KEY = 'jt_token'

// En local et avec Docker, /api passe par le proxy (Vite ou nginx).
// Sur Render, le backend a son propre domaine : VITE_API_HOST est injecte au build.
const host = import.meta.env.VITE_API_HOST
const API_URL = host ? `${host.startsWith('http') ? host : `https://${host}`}/api` : '/api'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))

// Requetes lentes : le serveur gratuit s'endort apres 15 min et met jusqu'a une minute a se reveiller.
// On previent l'interface (BandeauReveil) quand une requete depasse 4 s (sauf l'IA, lente par nature).
const DELAI_LENT = 4000
let requetesLentes = 0
const signalerLenteur = (delta) => {
  requetesLentes += delta
  window.dispatchEvent(new CustomEvent('postilyn:lent', { detail: requetesLentes > 0 }))
}

async function request(path, options = {}) {
  const surveiller = !path.includes('/ia')
  let lente = false
  const minuteur = surveiller ? setTimeout(() => { lente = true; signalerLenteur(1) }, DELAI_LENT) : null
  try {
    return await envoyer(path, options)
  } finally {
    clearTimeout(minuteur)
    if (lente) signalerLenteur(-1)
  }
}

async function envoyer(path, { method = 'GET', body, raw = false } = {}) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'

  const res = await fetch(`${API_URL}${path}`, {
    method,
    headers,
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401 && !path.startsWith('/auth')) {
    setToken(null)
    window.location.href = '/login'
    throw new Error('Session expirée')
  }
  if (!res.ok) {
    let message = `Erreur ${res.status}`
    try {
      const pb = await res.json()
      message = pb.erreurs ? Object.values(pb.erreurs).join(', ') : pb.detail || message
    } catch {
      // réponse sans JSON
    }
    throw new Error(message)
  }
  if (raw) return res
  return res.status === 204 ? null : res.json()
}

/** Declenche l'enregistrement d'un fichier sur l'appareil, sous le nom donne. */
export function enregistrerFichier(url, nomFichier) {
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nomFichier || 'document.pdf'
  document.body.appendChild(lien)
  lien.click()
  lien.remove()
}

export const api = {
  demo: () => request('/auth/demo', { method: 'POST' }),
  motDePasseOublie: (email) => request('/auth/mot-de-passe-oublie', { method: 'POST', body: { email } }),
  reinitialiser: (jeton, nouveau) => request('/auth/reinitialiser', { method: 'POST', body: { jeton, nouveau } }),
  deconnexionPartout: () => request('/compte/deconnexion-partout', { method: 'POST' }),
  emailTest: () => request('/compte/email-test', { method: 'POST' }),
  adminStatistiques: () => request('/admin/statistiques'),
  adminUtilisateurs: () => request('/admin/utilisateurs'),
  adminDeconnecter: (id) => request(`/admin/utilisateurs/${id}/deconnexion`, { method: 'POST' }),
  adminSupprimer: (id) => request(`/admin/utilisateurs/${id}`, { method: 'DELETE' }),
  login: (data) => request('/auth/login', { method: 'POST', body: data }),
  register: (data) => request('/auth/register', { method: 'POST', body: data }),

  candidatures: (params = {}) => {
    const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v)).toString()
    return request(`/candidatures${qs ? `?${qs}` : ''}`)
  },
  candidature: (id) => request(`/candidatures/${id}`),
  creerCandidature: (data) => request('/candidatures', { method: 'POST', body: data }),
  modifierCandidature: (id, data) => request(`/candidatures/${id}`, { method: 'PUT', body: data }),
  supprimerCandidature: (id) => request(`/candidatures/${id}`, { method: 'DELETE' }),

  ajouterEtape: (id, data) => request(`/candidatures/${id}/etapes`, { method: 'POST', body: data }),
  modifierEtape: (id, etapeId, data) =>
    request(`/candidatures/${id}/etapes/${etapeId}`, { method: 'PUT', body: data }),
  supprimerEtape: (id, etapeId) => request(`/candidatures/${id}/etapes/${etapeId}`, { method: 'DELETE' }),

  documents: () => request('/documents'),
  uploadDocument: (fichier, type) => {
    const fd = new FormData()
    fd.append('fichier', fichier)
    fd.append('type', type)
    return request('/documents', { method: 'POST', body: fd })
  },
  supprimerDocument: (id) => request(`/documents/${id}`, { method: 'DELETE' }),
  ouvrirDocument: async (id, nomFichier) => {
    // L'onglet est ouvert tout de suite (au clic) : sinon les navigateurs mobiles le bloquent
    const onglet = window.open('', '_blank')
    let url
    try {
      const res = await request(`/documents/${id}/fichier`, { raw: true })
      url = URL.createObjectURL(await res.blob())
    } catch (e) {
      onglet?.close()
      throw e
    }
    if (onglet) onglet.location.href = url
    else enregistrerFichier(url, nomFichier)
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  },
  /** Enregistre le fichier sur l'appareil (dossier Telechargements sur PC, Fichiers sur mobile). */
  telechargerDocument: async (id, nomFichier) => {
    const res = await request(`/documents/${id}/fichier`, { raw: true })
    const url = URL.createObjectURL(await res.blob())
    enregistrerFichier(url, nomFichier)
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  },

  profil: () => request('/compte/profil'),
  modifierProfil: (data) => request('/compte/profil', { method: 'PUT', body: data }),
  changerMotDePasse: (data) => request('/compte/mot-de-passe', { method: 'PUT', body: data }),
  exporterCompte: () => request('/compte/export'),
  supprimerCompte: (motDePasse) => request('/compte/suppression', { method: 'POST', body: { motDePasse } }),
  stats: () => request('/stats'),
  ia: (id, data) => request(`/candidatures/${id}/ia`, { method: 'POST', body: data }),
  compatibilite: (id, data) => request(`/candidatures/${id}/ia/compatibilite`, { method: 'POST', body: data }),
  relance: (id) => request(`/candidatures/${id}/ia/relance`, { method: 'POST' }),
  cvIa: (id, data) => request(`/candidatures/${id}/ia/cv`, { method: 'POST', body: data }),
  enregistrerCv: (id, cv) => request(`/candidatures/${id}/cv`, { method: 'POST', body: cv }),
  lettreIa: (id, data) => request(`/candidatures/${id}/ia/lettre`, { method: 'POST', body: data }),
  enregistrerLettre: (id, lettre) => request(`/candidatures/${id}/lettre`, { method: 'POST', body: lettre }),
  aide: (data) => request('/ia/aide', { method: 'POST', body: data }),
  offres: ({ q = '', lieu = '', contrat = '', source = '', page = 0 } = {}) =>
    request(`/offres?${new URLSearchParams({ q, lieu, contrat, source, page })}`),
}
