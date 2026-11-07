import { describe, expect, it } from 'vitest'
import { texteAParler } from '../voix.js'

describe('texteAParler', () => {
  it('retire la mise en forme Markdown et les emojis avant la lecture a voix haute', () => {
    const md = '## Bilan 🎉\n**Note : 7/10**\n- Clair et [structuré](https://x.fr)\n`code`'
    expect(texteAParler(md)).toBe('Bilan Note : 7/10 Clair et structuré code')
  })
})
