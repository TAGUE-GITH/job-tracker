import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import Logo from '../components/Logo.jsx'

export default function MotDePasseOublie() {
  const [email, setEmail] = useState('')
  const [envoye, setEnvoye] = useState(false)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')

  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    setChargement(true)
    try {
      await api.motDePasseOublie(email)
      setEnvoye(true)
    } catch (err) {
      setErreur(err.message)
    } finally {
      setChargement(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit}>
        <Link to="/accueil" className="auth-logo"><Logo size={40} /></Link>
        <h2 className="titre-auth">Mot de passe oublié</h2>
        {envoye ? (
          <>
            <p className="succes-bloc">
              📧 Si un compte existe avec <strong>{email}</strong>, un email vient de t'être envoyé avec un lien
              pour choisir un nouveau mot de passe. Il est valable <strong>30 minutes</strong>.
            </p>
            <p className="muted small">Pense à regarder dans tes spams.</p>
            <Link to="/login" className="btn primary">Retour à la connexion</Link>
          </>
        ) : (
          <>
            <p className="muted small">
              Indique l'email de ton compte : tu recevras un lien pour choisir un nouveau mot de passe.
            </p>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required
                autoComplete="email" autoFocus />
            </label>
            {erreur && <p className="erreur">{erreur}</p>}
            <button className="btn primary" disabled={chargement}>{chargement ? 'Envoi…' : 'Recevoir le lien'}</button>
            <Link to="/login" className="small centre-texte">← Retour à la connexion</Link>
          </>
        )}
      </form>
    </div>
  )
}
