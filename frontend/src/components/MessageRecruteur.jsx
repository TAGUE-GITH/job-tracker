import { useState } from 'react'
import { Check, Copy, Mail, Send } from 'lucide-react'
import { api } from '../api'

/** ORVYNA redige les messages a envoyer au recruteur : email de candidature, message et invitation LinkedIn. */
export default function MessageRecruteur({ candidature, cvId }) {
  const [consignes, setConsignes] = useState('')
  const [messages, setMessages] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')

  const rediger = async () => {
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.messageRecruteur(candidature.id, { cvId: cvId || null, consignes })
      setMessages(JSON.parse(reponse))
    } catch (e) {
      setErreur(e instanceof SyntaxError ? 'ORVYNA a renvoyé un message illisible, réessaie.' : e.message)
    } finally {
      setChargement(false)
    }
  }

  const changer = (cle, valeur) => setMessages((m) => ({ ...m, [cle]: valeur }))
  const mailto = messages
    ? `mailto:?subject=${encodeURIComponent(messages.objet ?? '')}&body=${encodeURIComponent(messages.email ?? '')}`
    : ''

  return (
    <div className="message-recruteur">
      {!messages && !chargement && (
        <p className="muted small">
          ORVYNA rédige un email de candidature à joindre avec ton CV et ta lettre, un message LinkedIn pour
          contacter directement le recruteur et une courte note d'invitation. Tout reste modifiable.
        </p>
      )}
      <label>
        Consignes pour ORVYNA <span className="muted small">(facultatif)</span>
        <input value={consignes} onChange={(e) => setConsignes(e.target.value)} maxLength={1000}
          placeholder="ex. le recruteur s'appelle Mme Durand, je suis disponible en septembre…" />
      </label>
      <button className="btn primary" onClick={rediger} disabled={chargement}>
        <Send size={16} /> {chargement ? 'ORVYNA rédige…' : messages ? 'Réécrire les messages' : 'Rédiger les messages'}
      </button>
      {erreur && <p className="erreur">{erreur}</p>}

      {messages && (
        <div className="messages-generes">
          <Bloc titre="Email de candidature" icone={<Mail size={16} />}
            texte={`Objet : ${messages.objet ?? ''}\n\n${messages.email ?? ''}`}
            actions={<a className="btn ghost small" href={mailto}><Mail size={14} /> Ouvrir dans ma messagerie</a>}>
            <label>Objet<input value={messages.objet ?? ''} onChange={(e) => changer('objet', e.target.value)} /></label>
            <textarea rows={16} value={messages.email ?? ''} onChange={(e) => changer('email', e.target.value)} />
          </Bloc>
          <Bloc titre="Message LinkedIn" texte={messages.linkedin ?? ''}>
            <textarea rows={9} value={messages.linkedin ?? ''} onChange={(e) => changer('linkedin', e.target.value)} />
          </Bloc>
          <Bloc titre="Note d'invitation LinkedIn" texte={messages.noteConnexion ?? ''}
            info={`${(messages.noteConnexion ?? '').length} / 300 caractères`}>
            <textarea rows={3} maxLength={300} value={messages.noteConnexion ?? ''}
              onChange={(e) => changer('noteConnexion', e.target.value)} />
          </Bloc>
        </div>
      )}
    </div>
  )
}

function Bloc({ titre, icone, texte, info, actions, children }) {
  const [copie, setCopie] = useState(false)
  const copier = async () => {
    try {
      await navigator.clipboard.writeText(texte)
      setCopie(true)
      setTimeout(() => setCopie(false), 2000)
    } catch {
      setCopie(false)
    }
  }
  return (
    <section className="bloc-message">
      <header>
        <h4>{icone}{titre}</h4>
        {info && <span className="muted small">{info}</span>}
      </header>
      {children}
      <div className="row">
        <button type="button" className="btn ghost small" onClick={copier}>
          {copie ? <><Check size={14} /> Copié</> : <><Copy size={14} /> Copier</>}
        </button>
        {actions}
      </div>
    </section>
  )
}
