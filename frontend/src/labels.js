export const STATUTS = {
  EN_ATTENTE: { label: 'En attente', couleur: 'gris' },
  EN_COURS: { label: 'En cours', couleur: 'bleu' },
  OFFRE_RECUE: { label: 'Offre reçue', couleur: 'vert' },
  REFUSEE: { label: 'Refusée', couleur: 'rouge' },
  ABANDONNEE: { label: 'Abandonnée', couleur: 'orange' },
}

export const ETAPES = {
  CANDIDATURE_ENVOYEE: 'Candidature envoyée',
  ECHANGE_RH: 'Premier échange RH',
  TEST_TECHNIQUE: 'Test technique',
  ENTRETIEN_TECHNIQUE: 'Entretien technique',
  ENTRETIEN_MANAGER: 'Entretien manager',
  OFFRE: 'Offre',
  REFUS: 'Refus',
  RELANCE: 'Relance envoyée',
}

export const formatDate = (iso) =>
  iso ? new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : ''

export const aujourdhui = () => new Date().toISOString().slice(0, 10)

/** "Franck Tague" -> "FT" (pastille de profil) */
export const initiales = (nom) => (nom ?? '?').split(/\s+/).filter(Boolean).slice(0, 2)
  .map((m) => m[0].toUpperCase()).join('') || '?'

/** "Aujourd'hui", "Hier", "Il y a 3 jours", "Il y a 2 semaines"... */
export function depuis(iso) {
  if (!iso) return null
  const jours = Math.round((Date.now() - new Date(`${iso}T12:00:00`).getTime()) / 86400000)
  if (jours <= 0) return "Aujourd'hui"
  if (jours === 1) return 'Hier'
  if (jours < 7) return `Il y a ${jours} jours`
  if (jours < 30) return `Il y a ${Math.round(jours / 7)} semaine${jours >= 11 ? 's' : ''}`
  return `Il y a ${Math.round(jours / 30)} mois`
}
