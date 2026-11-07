const TOKEN_KEY = 'jt_token'

// En local et avec Docker, /api passe par le proxy (Vite ou nginx).
// Sur Render, le backend a son propre domaine : VITE_API_HOST est injecte au build.
const host = import.meta.env.VITE_API_HOST
const API_URL = host ? `${host.startsWith('http') ? host : `https://${host}`}/api` : '/api'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))

async function request(path, { method = 'GET', body, raw = false } = {}) {
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

export const api = {
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
  ouvrirDocument: async (id) => {
    const res = await request(`/documents/${id}/fichier`, { raw: true })
    const url = URL.createObjectURL(await res.blob())
    window.open(url, '_blank')
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
