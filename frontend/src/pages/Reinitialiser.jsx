import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import ChampMotDePasse from '../components/ChampMotDePasse.jsx'
import Logo from '../components/Logo.jsx'
import { regleRespectee } from '../motDePasse.js'

// Arrivee depuis le lien recu par email : /reinitialiser?jeton=...
export default function Reinitialiser() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const jeton = params.get('jeton') ?? ''
  const [form, setForm] = useState({ nouveau: '', confirmation: '' })
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const [fait, setFait] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    if (!regleRespectee(form.nouveau)) {
      setErreur('Mot de passe trop faible : 8 caractères minimum, avec au moins une lettre et un chiffre.')
      return
    }
    if (form.nouveau !== form.confirmation) {
      setErreur('Les deux mots de passe ne sont pas identiques.')
      return
    }
    setChargement(true)
    try {
      await api.reinitialiser(jeton, form.nouveau)
      setFait(true)
      setTimeout(() => navigate('/login'), 2500)
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
        <h2 className="titre-auth">Nouveau mot de passe</h2>
        {!jeton ? (
          <>
            <p className="erreur">Ce lien est incomplet. Fais une nouvelle demande.</p>
            <Link to="/mot-de-passe-oublie" className="btn primary">Mot de passe oublié</Link>
          </>
        ) : fait ? (
          <p className="succes-bloc">
            ✅ Mot de passe modifié. Tes autres sessions ont été déconnectées. Redirection vers la connexion…
          </p>
        ) : (
          <>
            <label>
              Nouveau mot de passe
              <ChampMotDePasse name="nouveau" value={form.nouveau} onChange={onChange} required avecForce
                autoComplete="new-password" autoFocus />
            </label>
            <label>
              Confirme le mot de passe
              <ChampMotDePasse name="confirmation" value={form.confirmation} onChange={onChange} required
                autoComplete="new-password" />
            </label>
            {erreur && (
              <p className="erreur">
                {erreur} {erreur.includes('expire') && <Link to="/mot-de-passe-oublie">Nouvelle demande</Link>}
              </p>
            )}
            <button className="btn primary" disabled={chargement}>{chargement ? '…' : 'Enregistrer'}</button>
          </>
        )}
      </form>
    </div>
  )
}
