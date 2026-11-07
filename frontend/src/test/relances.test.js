import { describe, expect, it } from 'vitest'
import { aRelancer } from '../relances.js'

const jour = (decalage) => {
  const d = new Date()
  d.setDate(d.getDate() - decalage)
  return d.toISOString().slice(0, 10)
}

const candidature = (id, statut, ...dates) => ({
  id, statut, nomEntreprise: `Entreprise ${id}`, etapes: dates.map((date) => ({ type: 'ECHANGE_RH', date })),
})

describe('aRelancer', () => {
  it('garde les candidatures en attente ou en cours sans nouvelles depuis 7 jours, les plus anciennes d\'abord', () => {
    const liste = aRelancer([
      candidature(1, 'EN_ATTENTE', jour(8)),
      candidature(2, 'EN_COURS', jour(20), jour(12)),
      candidature(3, 'EN_COURS', jour(3)),
      candidature(4, 'REFUSEE', jour(30)),
      candidature(5, 'OFFRE_RECUE', jour(30)),
    ])
    expect(liste.map((r) => r.c.id)).toEqual([2, 1])
    expect(liste[0].jours).toBe(12)
  })

  it('ignore une candidature dont la derniere etape est dans le futur (entretien prevu)', () => {
    expect(aRelancer([candidature(1, 'EN_COURS', jour(10), jour(-2))])).toEqual([])
  })
})
