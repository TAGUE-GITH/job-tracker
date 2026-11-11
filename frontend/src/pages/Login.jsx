import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import Logo from '../components/Logo.jsx'
import { importEnAttente } from '../importOffre.js'
import ChampMotDePasse from '../components/ChampMotDePasse.jsx'
import { regleRespectee } from '../motDePasse.js'

export default function Login() {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [mode, setMode] = useState(params.get('mode') === 'inscription' ? 'register' : 'login')
  const [form, setForm] = useState({ nom: '', email: '', password: '' })
  const [erreur, setErreur] = useState('')
  const [chargement, setChargement] = useState(false)

  // Apres une connexion demandee par le favori "Ajouter a Postulyn", on reprend l'import
  const suite = importEnAttente() ? '/importer' : '/'
  if (user) return <Navigate to={suite} replace />

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    if (mode === 'register' && !regleRespectee(form.password)) {
      setErreur('Mot de passe trop faible : 8 caractères minimum, avec au moins une lettre et un chiffre.')
      return
    }
    setChargement(true)
    try {
      if (mode === 'login') await login({ email: form.email, password: form.password })
      else await register(form)
      navigate(suite)
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
        <p className="muted">Suis tes candidatures et prépare tes entretiens.</p>

        <div className="tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
            Connexion
          </button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
            Inscription
          </button>
        </div>

        {mode === 'register' && (
          <label>
            Nom
            <input name="nom" value={form.nom} onChange={onChange} required />
          </label>
        )}
        <label>
          Email
          <input name="email" type="email" value={form.email} onChange={onChange} required />
        </label>
        <label>
          Mot de passe
          <ChampMotDePasse name="password" value={form.password} onChange={onChange} required
            avecForce={mode === 'register'} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
        </label>
        <Link to="/mot-de-passe-oublie" className="small lien-oubli">Mot de passe oublié ?</Link>

        {erreur && <p className="erreur">{erreur}</p>}
        <button className="btn primary" disabled={chargement}>
          {chargement ? '…' : mode === 'login' ? 'Se connecter' : 'Créer mon compte'}
        </button>
        <p className="muted small centre-texte">
          Juste curieux ? <Link to="/demo">Essaie la démo sans t'inscrire</Link>
        </p>
      </form>
    </div>
  )
}
