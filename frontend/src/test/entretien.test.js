import { describe, expect, it } from 'vitest'
import { exercice, pourLaVoix, sansMarqueur } from '../entretien.js'

const MESSAGE = `Ce qui est bien : vous connaissez Spring.

**Exercice** : écrivez une méthode qui inverse une chaîne.

\`\`\`java
public String inverser(String s) {
    // à compléter
}
\`\`\`

EXERCICE_CODE: Java`

describe('exercices de code de l’entretien', () => {
  it('détecte l’exercice, son langage et le code de départ', () => {
    const ex = exercice(MESSAGE)
    expect(ex.langage).toBe('java')
    expect(ex.code).toContain('public String inverser(String s)')
    expect(exercice('Parlez-moi de vous.')).toBeNull()
    expect(exercice('EXERCICE_CODE: C#').langage).toBe('csharp')
  })

  it('retire le marqueur de l’affichage et le code de la lecture à voix haute', () => {
    expect(sansMarqueur(MESSAGE)).not.toContain('EXERCICE_CODE')
    const voix = pourLaVoix(MESSAGE)
    expect(voix).not.toContain('public String')
    expect(voix).not.toContain('**')
    expect(voix).toContain('le code est affiché à l’écran')
  })
})
