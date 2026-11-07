import { aujourdhui } from './labels'

export const DELAI_RELANCE = 7

const JOUR = 24 * 60 * 60 * 1000

/** Candidatures en attente ou en cours dont la derniere etape date d'au moins 7 jours. */
export function aRelancer(candidatures) {
  const maintenant = new Date(`${aujourdhui()}T00:00:00`)
  return candidatures
    .filter((c) => c.statut === 'EN_ATTENTE' || c.statut === 'EN_COURS')
    .map((c) => {
      const derniere = [...c.etapes].sort((a, b) => a.date.localeCompare(b.date)).at(-1)
      const jours = derniere ? Math.round((maintenant - new Date(`${derniere.date}T00:00:00`)) / JOUR) : 0
      return { c, derniere, jours }
    })
    .filter((r) => r.jours >= DELAI_RELANCE)
    .sort((a, b) => b.jours - a.jours)
}
