import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import OrvynaAvatar from './OrvynaAvatar.jsx'
import { LifeBuoy, Send, X } from 'lucide-react'
import { Link } from 'react-router-dom'

const SUGGESTIONS = [
  'Comment ajouter une candidature ?',
  'Où en sont mes candidatures ?',
  'Comment préparer un entretien avec toi ?',
  'Quand relancer un recruteur ?',
  'J’ai un problème avec l’application',
]

// Les liens internes proposes par ORVYNA (ex. /contact) restent dans l'application
function LienMarkdown({ href = '', children }) {
  if (href.startsWith('/')) return <Link to={href}>{children}</Link>
  return <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>
}

// Bulle d'aide flottante : ORVYNA repond aux questions sur l'application, sur toutes les pages
export default function OrvynaWidget() {
  const [ouvert, setOuvert] = useState(false)
  const [messages, setMessages] = useState([])
  const [saisie, setSaisie] = useState('')
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const finRef = useRef(null)

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, chargement, ouvert])

  const demander = async (texte) => {
    if (!texte.trim() || chargement) return
    const historique = [...messages, { role: 'user', content: texte.trim() }]
    setMessages(historique)
    setSaisie('')
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.aide({ messages: historique })
      setMessages([...historique, { role: 'assistant', content: reponse }])
    } catch (e) {
      setErreur(e.message)
    } finally {
      setChargement(false)
    }
  }

  const onSubmit = (e) => {
    e.preventDefault()
    demander(saisie)
  }

  return (
    <div className={`orvyna ${ouvert ? 'orvyna-ouvert' : ''}`}>
      {ouvert && (
        <section className="orvyna-panel" aria-label="Discuter avec ORVYNA">
          <header className="orvyna-head">
            <OrvynaAvatar size={40} />
            <div>
              <strong>ORVYNA</strong>
              <small><span className="orvyna-dot" /> Ton assistant Postulyn</small>
            </div>
            <button className="orvyna-fermer" onClick={() => setOuvert(false)} aria-label="Fermer"><X size={20} /></button>
          </header>

          <div className="orvyna-messages">
            <div className="bulle assistant">
              Bonjour, je suis <b>ORVYNA</b>. Pose-moi une question sur Postulyn, sur tes candidatures
              ou sur ta recherche d'emploi.
            </div>
            {messages.length === 0 && (
              <div className="orvyna-suggestions">
                {SUGGESTIONS.map((s) => (
                  <button key={s} type="button" onClick={() => demander(s)}>{s}</button>
                ))}
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} className={`bulle ${m.role}`}>
                {m.role === 'assistant' ? <Markdown remarkPlugins={[remarkGfm]} components={{ a: LienMarkdown }}>{m.content}</Markdown> : m.content}
              </div>
            ))}
            {chargement && <div className="bulle assistant orvyna-ecrit"><span /><span /><span /></div>}
            {erreur && <p className="erreur">{erreur}</p>}
            <div ref={finRef} />
          </div>

          <Link to="/contact" className="orvyna-support" onClick={() => setOuvert(false)}>
            <LifeBuoy size={14} /> Un problème non résolu ? Contacter l'équipe
          </Link>
          <form className="orvyna-saisie" onSubmit={onSubmit}>
            <input value={saisie} onChange={(e) => setSaisie(e.target.value)}
              placeholder="Écris ta question…" disabled={chargement} autoFocus />
            <button className="btn primary" disabled={chargement || !saisie.trim()} aria-label="Envoyer"><Send size={16} /></button>
          </form>
        </section>
      )}

      <button className="orvyna-bouton" onClick={() => setOuvert(!ouvert)}
        aria-label={ouvert ? 'Fermer ORVYNA' : 'Ouvrir ORVYNA, ton assistant'}>
        <OrvynaAvatar size={46} />
        {!ouvert && <span className="orvyna-label">Besoin d'aide ?</span>}
      </button>
    </div>
  )
}
