import { describe, expect, it } from 'vitest'
import { forceMotDePasse, regleRespectee } from '../motDePasse.js'

describe('mot de passe', () => {
  it('applique la meme regle que le serveur', () => {
    expect(regleRespectee('court1')).toBe(false)
    expect(regleRespectee('motdepasse')).toBe(false)
    expect(regleRespectee('12345678')).toBe(false)
    expect(regleRespectee('secret123')).toBe(true)
    expect(regleRespectee('élève2026')).toBe(true)
  })

  it('estime la force', () => {
    expect(forceMotDePasse('abc').niveau).toBe(0)
    expect(forceMotDePasse('secret123').niveau).toBeLessThan(forceMotDePasse('Secret-123-Postilyn!').niveau)
    expect(forceMotDePasse('Secret-123-Postilyn!').libelle).toBe('Excellent')
  })
})
