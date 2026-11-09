import { describe, expect, it } from 'vitest'
import { projetEnDoublon } from '../cvDoublons.js'

const experiences = [
  { poste: 'Développeur fullstack (stage)', entreprise: 'DITS Group' },
  { poste: 'Développeur fullstack (stage)', entreprise: 'Yamo Group Sarl' },
]

describe('projetEnDoublon', () => {
  it("repère un projet qui reprend le travail d'une expérience", () => {
    expect(projetEnDoublon({ nom: 'Portal replacement - site vitrine (DITS)', description: '' }, experiences)).toBe(true)
    expect(projetEnDoublon({ nom: 'Moteur de recherche', description: 'Réalisé chez Yamo' }, experiences)).toBe(true)
  })

  it('garde les projets personnels ou académiques', () => {
    expect(projetEnDoublon({ nom: 'ShoeManager - interface de gestion (Django)', description: 'Application web' }, experiences))
      .toBe(false)
    // "Group" seul ne suffit pas à conclure à un doublon
    expect(projetEnDoublon({ nom: 'Projet de groupe', description: 'Group project' }, experiences)).toBe(false)
  })
})
