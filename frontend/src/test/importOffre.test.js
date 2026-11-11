import { describe, expect, it } from 'vitest'
import { codeFavori, importEnAttente, lireOffreImportee, mettreOffreDeCote, oublierOffre } from '../importOffre.js'

const hash = (o) => `#${encodeURIComponent(JSON.stringify(o))}`

describe('favori Ajouter a Postilyn', () => {
  it('produit un lien javascript: qui ouvre la page /importer du site', () => {
    const code = codeFavori('https://taguora.vercel.app')
    expect(code.startsWith('javascript:')).toBe(true)
    expect(decodeURIComponent(code)).toContain('https://taguora.vercel.app/importer#')
  })

  it("relit l'offre transmise dans l'URL", () => {
    const offre = lireOffreImportee(hash({ poste: 'Dev Java', entreprise: 'Liksi', lien: 'https://x.fr/1', texte: 'Lieu : Nantes' }))
    expect(offre).toEqual({ nomEntreprise: 'Liksi', poste: 'Dev Java', lienOffre: 'https://x.fr/1', texteOffre: 'Lieu : Nantes' })
  })

  it('garde une offre de cote pendant la connexion', () => {
    mettreOffreDeCote(hash({ poste: 'Alternant', entreprise: 'Nordlys' }))
    expect(importEnAttente()).toBe(true)
    expect(lireOffreImportee('').poste).toBe('Alternant')
    oublierOffre()
    expect(importEnAttente()).toBe(false)
  })

  it('ignore une URL illisible', () => {
    expect(lireOffreImportee('#pas-du-json')).toBeNull()
  })
})
