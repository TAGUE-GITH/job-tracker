import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import IvoAvatar from './IvoAvatar.jsx'

const SUGGESTIONS = [
  'Comment ajouter une candidature ?',
  'Où en sont mes candidatures ?',
  'Comment préparer un entretien avec toi ?',
  'Quand relancer un recruteur ?',
]

// Bulle d'aide flottante : Ivo repond aux questions sur l'application, sur toutes les pages
export default function IvoWidget() {
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
    <div className={`ivo ${ouvert ? 'ivo-ouvert' : ''}`}>
      {ouvert && (
        <section className="ivo-panel" aria-label="Discuter avec Ivo">
          <header className="ivo-head">
            <IvoAvatar size={40} />
            <div>
              <strong>Ivo</strong>
              <small><span className="ivo-dot" /> Ton assistant Taguora</small>
            </div>
            <button className="ivo-fermer" onClick={() => setOuvert(false)} aria-label="Fermer">×</button>
          </header>

          <div className="ivo-messages">
            <div className="bulle assistant">
              Salut 👋 Je suis <b>Ivo</b>. Pose-moi une question sur Taguora, sur tes candidatures
              ou sur ta recherche d'emploi.
            </div>
            {messages.length === 0 && (
              <div className="ivo-suggestions">
                {SUGGESTIONS.map((s) => (
                  <button key={s} type="button" onClick={() => demander(s)}>{s}</button>
                ))}
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} className={`bulle ${m.role}`}>
                {m.role === 'assistant' ? <Markdown remarkPlugins={[remarkGfm]}>{m.content}</Markdown> : m.content}
              </div>
            ))}
            {chargement && <div className="bulle assistant ivo-ecrit"><span /><span /><span /></div>}
            {erreur && <p className="erreur">{erreur}</p>}
            <div ref={finRef} />
          </div>

          <form className="ivo-saisie" onSubmit={onSubmit}>
            <input value={saisie} onChange={(e) => setSaisie(e.target.value)}
              placeholder="Écris ta question…" disabled={chargement} autoFocus />
            <button className="btn primary" disabled={chargement || !saisie.trim()} aria-label="Envoyer">➤</button>
          </form>
        </section>
      )}

      <button className="ivo-bouton" onClick={() => setOuvert(!ouvert)}
        aria-label={ouvert ? 'Fermer Ivo' : 'Ouvrir Ivo, ton assistant'}>
        <IvoAvatar size={46} />
        {!ouvert && <span className="ivo-label">Besoin d'aide ?</span>}
      </button>
    </div>
  )
}
