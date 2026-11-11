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
