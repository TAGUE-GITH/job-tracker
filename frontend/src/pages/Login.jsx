import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function Login() {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ nom: '', email: '', password: '' })
  const [erreur, setErreur] = useState('')
  const [chargement, setChargement] = useState(false)

  if (user) return <Navigate to="/" replace />

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    setChargement(true)
    try {
      if (mode === 'login') await login({ email: form.email, password: form.password })
      else await register(form)
      navigate('/')
    } catch (err) {
      setErreur(err.message)
    } finally {
      setChargement(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit}>
        <h1>Job Tracker</h1>
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
          <input name="password" type="password" minLength={6} value={form.password} onChange={onChange} required />
        </label>

        {erreur && <p className="erreur">{erreur}</p>}
        <button className="btn primary" disabled={chargement}>
          {chargement ? '…' : mode === 'login' ? 'Se connecter' : 'Créer mon compte'}
        </button>
      </form>
    </div>
  )
}
