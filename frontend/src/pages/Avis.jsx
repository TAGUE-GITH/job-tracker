import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CircleCheck, Star } from 'lucide-react'
import { api } from '../api'

const LIBELLES = ['', 'Très décevant', 'Décevant', 'Correct', 'Bien', 'Excellent']

/** Les utilisateurs notent Postulyn ; les avis sont lus dans Administration > Avis. */
export default function Avis() {
  const [note, setNote] = useState(0)
  const [survol, setSurvol] = useState(0)
  const [commentaire, setCommentaire] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState('')
  const [merci, setMerci] = useState(false)

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!note) {
      setErreur('Choisis une note de 1 à 5 étoiles.')
      return
    }
    setErreur('')
    setEnvoi(true)
    try {
      await api.donnerAvis({ note, commentaire })
      setMerci(true)
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  const affichee = survol || note

  return (
    <div className="avis-page">
      <div className="page-head">
        <div>
          <h1>Donner mon avis</h1>
          <p className="muted">Ton retour nous aide à améliorer Postulyn. Il est lu par l'équipe.</p>
        </div>
      </div>

      {merci ? (
        <div className="card contact-merci">
          <CircleCheck size={40} />
          <h2>Merci pour ton avis !</h2>
          <p className="muted">Une question ou un problème précis ? Écris-nous depuis la page <Link to="/contact">Aide et contact</Link>.</p>
          <button className="btn ghost" onClick={() => { setMerci(false); setNote(0); setCommentaire('') }}>Donner un autre avis</button>
        </div>
      ) : (
        <form className="card avis-form" onSubmit={onSubmit}>
          <fieldset className="etoiles" onMouseLeave={() => setSurvol(0)}>
            <legend>Que penses-tu de Postulyn ?</legend>
            <div className="etoiles-ligne">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} type="button" className={n <= affichee ? 'pleine' : ''} onClick={() => setNote(n)}
                  onMouseEnter={() => setSurvol(n)} aria-label={`${n} étoile${n > 1 ? 's' : ''} sur 5`} aria-pressed={note === n}>
                  <Star size={34} strokeWidth={1.6} />
                </button>
              ))}
              <span className="etoiles-libelle">{LIBELLES[affichee]}</span>
            </div>
          </fieldset>
          <label>
            Ton commentaire <span className="muted small">(facultatif)</span>
            <textarea value={commentaire} onChange={(e) => setCommentaire(e.target.value)} rows={5} maxLength={2000}
              placeholder="Ce que tu apprécies, ce qui te manque, ce qu'on pourrait améliorer…" />
          </label>
          {erreur && <p className="erreur">{erreur}</p>}
          <button className="btn primary" disabled={envoi}>{envoi ? 'Envoi…' : 'Envoyer mon avis'}</button>
        </form>
      )}
    </div>
  )
}
