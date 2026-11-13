import { useRef } from 'react'
import { Code2, RotateCcw, Send, X } from 'lucide-react'
import { LANGAGES } from '../codeLangages.js'

/**
 * Espace de code de l'entretien : le candidat ecrit sa solution a l'exercice propose par ORVYNA.
 * Numeros de ligne, tabulation sur 4 espaces, choix du langage.
 */
export default function EspaceCode({ langage, onLangage, code, onCode, codeDepart, onEnvoyer, onFermer, desactive }) {
  const zone = useRef(null)
  const numeros = useRef(null)
  const lignes = Math.max(code.split('\n').length, 12)

  const touche = (e) => {
    if (e.key === 'Tab') {
      e.preventDefault()
      const el = e.target
      const debut = el.selectionStart
      const fin = el.selectionEnd
      const nouveau = `${code.slice(0, debut)}    ${code.slice(fin)}`
      onCode(nouveau)
      requestAnimationFrame(() => { el.selectionStart = el.selectionEnd = debut + 4 })
    } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter' && code.trim() && !desactive) {
      e.preventDefault()
      onEnvoyer()
    }
  }

  return (
    <section className="espace-code" aria-label="Espace de code">
      <header>
        <span className="espace-code-titre"><Code2 size={16} /> Espace de code</span>
        <select value={langage} onChange={(e) => onLangage(e.target.value)} aria-label="Langage">
          {LANGAGES.map(([c, nom]) => <option key={c} value={c}>{nom}</option>)}
        </select>
        <button type="button" className="icon" onClick={onFermer} title="Fermer l'espace de code" aria-label="Fermer l'espace de code"><X size={16} /></button>
      </header>
      <div className="espace-code-editeur">
        <pre ref={numeros} aria-hidden="true">
          {Array.from({ length: lignes }, (_, i) => i + 1).join('\n')}
        </pre>
        <textarea ref={zone} value={code} onChange={(e) => onCode(e.target.value)} onKeyDown={touche}
          onScroll={(e) => { if (numeros.current) numeros.current.scrollTop = e.target.scrollTop }}
          spellCheck={false} autoCapitalize="off" autoCorrect="off" wrap="off"
          placeholder="Écris ta solution ici…" aria-label="Ton code" />
      </div>
      <footer>
        <span className="muted small">Tab : indenter · Ctrl + Entrée : envoyer</span>
        <div className="row">
          {codeDepart !== undefined && code !== codeDepart && (
            <button type="button" className="btn ghost small" onClick={() => onCode(codeDepart)}>
              <RotateCcw size={14} /> Repartir du code de départ
            </button>
          )}
          <button type="button" className="btn primary small" onClick={onEnvoyer} disabled={desactive || !code.trim()}>
            <Send size={14} /> Envoyer mon code à ORVYNA
          </button>
        </div>
      </footer>
    </section>
  )
}
