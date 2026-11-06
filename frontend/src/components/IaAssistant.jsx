import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ETAPES } from '../labels'
import IvoAvatar from './IvoAvatar.jsx'

const ETAPES_ENTRETIEN = ['ECHANGE_RH', 'TEST_TECHNIQUE', 'ENTRETIEN_TECHNIQUE', 'ENTRETIEN_MANAGER']

export default function IaAssistant({ candidature }) {
  const [typeEtape, setTypeEtape] = useState('ENTRETIEN_TECHNIQUE')
  const [messages, setMessages] = useState([])
  const [saisie, setSaisie] = useState('')
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const finRef = useRef(null)

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, chargement])

  const envoyer = async (historique) => {
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.ia(candidature.id, { typeEtape, messages: historique })
      setMessages([...historique, { role: 'assistant', content: reponse }])
    } catch (e) {
      setErreur(e.message)
    } finally {
      setChargement(false)
    }
  }

  const preparer = () => {
    setMessages([])
    envoyer([])
  }

  const simuler = () => {
    const h = [{ role: 'user', content: "Simule l'entretien : pose-moi la première question." }]
    setMessages(h)
    envoyer(h)
  }

  const onSubmit = (e) => {
    e.preventDefault()
    if (!saisie.trim()) return
    const h = [...messages, { role: 'user', content: saisie.trim() }]
    setMessages(h)
    setSaisie('')
    envoyer(h)
  }

  return (
    <div className="card ia">
      <div className="ia-head">
        <IvoAvatar size={44} />
        <div>
          <h3>Ivo · coach d'entretien</h3>
          <p className="muted small">
            Ivo s'appuie sur le texte de l'offre et tes notes d'étapes pour te préparer.
          </p>
        </div>
      </div>

      <div className="row">
        <select value={typeEtape} onChange={(e) => setTypeEtape(e.target.value)}>
          {ETAPES_ENTRETIEN.map((t) => <option key={t} value={t}>{ETAPES[t]}</option>)}
        </select>
        <button className="btn primary" onClick={preparer} disabled={chargement}>Préparer</button>
        <button className="btn ghost" onClick={simuler} disabled={chargement}>Simuler</button>
      </div>

      <div className="chat">
        {messages.map((m, i) => (
          <div key={i} className={`bulle ${m.role}`}>
            {m.role === 'assistant' ? <Markdown remarkPlugins={[remarkGfm]}>{m.content}</Markdown> : m.content}
          </div>
        ))}
        {chargement && <div className="bulle assistant muted">Ivo réfléchit…</div>}
        {erreur && <p className="erreur">{erreur}</p>}
        <div ref={finRef} />
      </div>

      {messages.length > 0 && (
        <form className="row" onSubmit={onSubmit}>
          <input value={saisie} onChange={(e) => setSaisie(e.target.value)}
            placeholder="Ta réponse ou ta question…" disabled={chargement} />
          <button className="btn primary" disabled={chargement}>Envoyer</button>
        </form>
      )}
    </div>
  )
}
