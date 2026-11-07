import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth.jsx'
import { aujourdhui } from '../labels'

export default function Compte() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="compte">
      <div className="page-head">
        <div>
          <h1>Mon compte</h1>
          <p className="muted">{user.nom} · {user.email}</p>
        </div>
      </div>
      <Coordonnees />
      <MotDePasse />
      <Export />
      <Suppression onSupprime={() => { logout(); navigate('/accueil') }} />
    </div>
  )
}

function Coordonnees() {
  const { majUtilisateur } = useAuth()
  const [form, setForm] = useState(null)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api.profil()
      .then((p) => setForm({ nom: p.nom ?? '', telephone: p.telephone ?? '', adresse: p.adresse ?? '', ville: p.ville ?? '' }))
      .catch((e) => setErreur(e.message))
  }, [])

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setErreur('')
    try {
      const p = await api.modifierProfil(form)
      majUtilisateur({ nom: p.nom })
      setMessage('Coordonnées enregistrées ✓')
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <form className="card" onSubmit={onSubmit}>
      <h3>📇 Mes coordonnées</h3>
      <p className="muted small">Elles s'affichent en haut à gauche de tes lettres de motivation.</p>
      {!form ? <p className="muted">Chargement…</p> : (
        <>
          <div className="grid-2">
            <label>
              Nom et prénom
              <input name="nom" value={form.nom} onChange={onChange} required maxLength={100} autoComplete="name" />
            </label>
            <label>
              Téléphone
              <input name="telephone" type="tel" value={form.telephone} onChange={onChange} maxLength={30}
                placeholder="06 12 34 56 78" autoComplete="tel" />
            </label>
          </div>
          <label>
            Adresse <span className="muted small">(rue, puis code postal et ville sur la ligne suivante)</span>
            <textarea name="adresse" value={form.adresse} onChange={onChange} rows={2} maxLength={300}
              placeholder={'12 rue des Lilas\n75011 Paris'} autoComplete="street-address" />
          </label>
          <label>
            Ville <span className="muted small">(pour « À Paris, le … » sous le destinataire)</span>
            <input name="ville" value={form.ville} onChange={onChange} maxLength={100} placeholder="Paris" />
          </label>
        </>
      )}
      {erreur && <p className="erreur">{erreur}</p>}
      {message && <p className="succes">{message}</p>}
      {form && <button className="btn primary">Enregistrer</button>}
    </form>
  )
}

function MotDePasse() {
  const [form, setForm] = useState({ actuel: '', nouveau: '', confirmation: '' })
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setErreur('')
    if (form.nouveau !== form.confirmation) {
      setErreur('Les deux nouveaux mots de passe ne sont pas identiques.')
      return
    }
    try {
      await api.changerMotDePasse({ actuel: form.actuel, nouveau: form.nouveau })
      setForm({ actuel: '', nouveau: '', confirmation: '' })
      setMessage('Mot de passe modifié ✓')
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <form className="card" onSubmit={onSubmit}>
      <h3>🔑 Changer mon mot de passe</h3>
      <label>
        Mot de passe actuel
        <input name="actuel" type="password" value={form.actuel} onChange={onChange} required autoComplete="current-password" />
      </label>
      <label>
        Nouveau mot de passe <span className="muted small">(6 caractères minimum)</span>
        <input name="nouveau" type="password" minLength={6} value={form.nouveau} onChange={onChange} required autoComplete="new-password" />
      </label>
      <label>
        Confirme le nouveau mot de passe
        <input name="confirmation" type="password" minLength={6} value={form.confirmation} onChange={onChange} required autoComplete="new-password" />
      </label>
      {erreur && <p className="erreur">{erreur}</p>}
      {message && <p className="succes">{message}</p>}
      <button className="btn primary">Enregistrer</button>
    </form>
  )
}

function Export() {
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')

  const exporter = async () => {
    setErreur('')
    setChargement(true)
    try {
      const donnees = await api.exporterCompte()
      const url = URL.createObjectURL(new Blob([JSON.stringify(donnees, null, 2)], { type: 'application/json' }))
      const lien = document.createElement('a')
      lien.href = url
      lien.download = `taguora-export-${aujourdhui()}.json`
      lien.click()
      setTimeout(() => URL.revokeObjectURL(url), 10000)
    } catch (err) {
      setErreur(err.message)
    } finally {
      setChargement(false)
    }
  }

  return (
    <div className="card">
      <h3>📦 Exporter mes données</h3>
      <p className="muted small">
        Télécharge toutes tes candidatures, leurs étapes et notes, et la liste de tes documents dans un fichier JSON.
        Tes CV et lettres se téléchargent un par un depuis « CV & lettres ».
      </p>
      {erreur && <p className="erreur">{erreur}</p>}
      <button className="btn ghost" onClick={exporter} disabled={chargement}>
        {chargement ? 'Préparation…' : 'Télécharger mes données'}
      </button>
    </div>
  )
}

function Suppression({ onSupprime }) {
  const [ouvert, setOuvert] = useState(false)
  const [motDePasse, setMotDePasse] = useState('')
  const [erreur, setErreur] = useState('')

  const supprimer = async (e) => {
    e.preventDefault()
    setErreur('')
    if (!confirm('Supprimer définitivement ton compte, tes candidatures et tes documents ? Cette action est irréversible.')) return
    try {
      await api.supprimerCompte(motDePasse)
      onSupprime()
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <div className="card zone-danger">
      <h3>Supprimer mon compte</h3>
      <p className="muted small">
        Toutes tes candidatures, étapes, CV et lettres seront effacés définitivement. Pense à exporter tes données avant.
      </p>
      {!ouvert ? (
        <button className="btn danger" onClick={() => setOuvert(true)}>Supprimer mon compte…</button>
      ) : (
        <form onSubmit={supprimer}>
          <label>
            Confirme avec ton mot de passe
            <input type="password" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} required autoComplete="current-password" />
          </label>
          {erreur && <p className="erreur">{erreur}</p>}
          <div className="actions">
            <button type="button" className="btn ghost" onClick={() => setOuvert(false)}>Annuler</button>
            <button className="btn danger">Supprimer définitivement</button>
          </div>
        </form>
      )}
    </div>
  )
}
