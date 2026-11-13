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

// Bulle d'aide flottante : ORVYNA repond aux questions sur l'application et la recherche d'emploi.
// visiteur : sur la page d'accueil sans etre connecte, ORVYNA invite a se connecter pour discuter.
export default function OrvynaWidget({ visiteur = false }) {
  const [ouvert, setOuvert] = useState(false)
  const [parle, setParle] = useState(false)
  const [messages, setMessages] = useState([])
  const [saisie, setSaisie] = useState('')
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const finRef = useRef(null)

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, chargement, ouvert])

  // Pendant quelques secondes apres une reponse, l'avatar "parle"
  useEffect(() => {
    if (!parle) return undefined
    const t = setTimeout(() => setParle(false), 2600)
    return () => clearTimeout(t)
  }, [parle])

  const demander = async (texte) => {
    if (visiteur || !texte.trim() || chargement) return
    const historique = [...messages, { role: 'user', content: texte.trim() }]
    setMessages(historique)
    setSaisie('')
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.aide({ messages: historique })
      setMessages([...historique, { role: 'assistant', content: reponse }])
      setParle(true)
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

  // Ouverture depuis un bouton de la page (ex. page d'accueil)
  const demanderRef = useRef(demander)
  useEffect(() => {
    demanderRef.current = demander
  })
  useEffect(() => {
    const ouvrir = (e) => {
      setOuvert(true)
      if (e.detail) demanderRef.current(e.detail)
    }
    window.addEventListener('orvyna:ouvrir', ouvrir)
    return () => window.removeEventListener('orvyna:ouvrir', ouvrir)
  }, [])

  const etat = chargement ? 'reflechit' : parle ? 'parle' : 'repos'

  return (
    <div className={`orvyna ${ouvert ? 'orvyna-ouvert' : ''}`}>
      {ouvert && (
        <section className="orvyna-panel" aria-label="Discuter avec ORVYNA">
          <header className="orvyna-head">
            <OrvynaAvatar size={44} etat={etat} />
            <div>
              <strong>ORVYNA</strong>
              <small><span className="orvyna-dot" /> {chargement ? 'Réfléchit…' : visiteur ? 'Assistante IA de Postulyn' : 'Ton assistante Postulyn'}</small>
            </div>
            <button className="orvyna-fermer" onClick={() => setOuvert(false)} aria-label="Fermer"><X size={20} /></button>
          </header>

          {visiteur ? (
            <div className="orvyna-messages orvyna-invitation">
              <div className="bulle assistant">
                Bonjour, je suis <b>ORVYNA</b>, l’assistante IA de Postulyn. Connectez-vous pour discuter avec moi :
                je réponds à vos questions sur l’application et sur votre recherche d’emploi, et je vous aide à
                préparer chacune de vos candidatures.
              </div>
              <div className="orvyna-invitation-actions">
                <Link to="/login" className="btn primary">Se connecter</Link>
                <Link to="/login?mode=inscription" className="btn ghost">Créer un compte gratuit</Link>
              </div>
            </div>
          ) : (
            <>
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
            </>
          )}
        </section>
      )}

      <button className="orvyna-bouton" onClick={() => setOuvert(!ouvert)}
        aria-label={ouvert ? 'Fermer ORVYNA' : 'Ouvrir ORVYNA, l’assistante IA'}>
        <OrvynaAvatar size={50} etat={etat} />
        {!ouvert && <span className="orvyna-label">{visiteur ? 'Découvrir ORVYNA' : "Besoin d'aide ?"}</span>}
      </button>
    </div>
  )
}
