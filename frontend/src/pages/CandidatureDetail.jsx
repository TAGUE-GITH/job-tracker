import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { ETAPES, aujourdhui, formatDate } from '../labels'
import StatutBadge from '../components/StatutBadge.jsx'
import IaAssistant from '../components/IaAssistant.jsx'
import CvLettre from '../components/CvLettre.jsx'

export default function CandidatureDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [c, setC] = useState(null)
  const [erreur, setErreur] = useState('')
  const [afficherOffre, setAfficherOffre] = useState(false)

  useEffect(() => {
    api.candidature(id).then(setC).catch((e) => setErreur(e.message))
  }, [id])

  const supprimer = async () => {
    if (!confirm(`Supprimer la candidature chez ${c.nomEntreprise} ?`)) return
    await api.supprimerCandidature(id)
    navigate('/candidatures')
  }

  const supprimerEtape = async (etapeId) => {
    if (!confirm('Supprimer cette étape ?')) return
    setC(await api.supprimerEtape(id, etapeId))
  }

  if (erreur) return <p className="erreur">{erreur}</p>
  if (!c) return <p className="muted">Chargement…</p>

  return (
    <>
      <div className="page-head">
        <div>
          <Link to="/candidatures" className="muted">← Candidatures</Link>
          <h1>{c.nomEntreprise} <StatutBadge statut={c.statut} /></h1>
          <p className="muted">{c.poste} · candidature du {formatDate(c.dateCandidature)}</p>
        </div>
        <div className="actions">
          <Link to={`/candidatures/${id}/modifier`} className="btn ghost">Modifier</Link>
          <button className="btn danger" onClick={supprimer}>Supprimer</button>
        </div>
      </div>

      <div className="grid-2 align-start">
        <div className="col">
          <div className="card">
            <h3>Informations</h3>
            <dl className="infos">
              <dt>Lien</dt>
              <dd>{c.lienOffre ? <a href={c.lienOffre} target="_blank" rel="noreferrer">Voir l'offre ↗</a> : '—'}</dd>
              <dt>CV</dt>
              <dd><DocLien doc={c.cv} /></dd>
              <dt>Lettre</dt>
              <dd><DocLien doc={c.lettre} /></dd>
            </dl>
            {c.texteOffre && (
              <>
                <button className="btn ghost small" onClick={() => setAfficherOffre(!afficherOffre)}>
                  {afficherOffre ? 'Masquer' : 'Afficher'} le texte de l'offre
                </button>
                {afficherOffre && <pre className="texte-offre">{c.texteOffre}</pre>}
              </>
            )}
          </div>

          <CvLettre candidature={c} onChange={setC} />
          <div className="card">
            <h3>Évolution</h3>
            <ol className="timeline">
              {c.etapes.map((e) => (
                <li key={e.id} className={`etape-${e.type}`}>
                  <div className="timeline-head">
                    <strong>{ETAPES[e.type]}</strong>
                    <span className="muted">{formatDate(e.date)}</span>
                    <button className="icon" title="Supprimer" onClick={() => supprimerEtape(e.id)}>×</button>
                  </div>
                  {e.notes && <p className="notes">{e.notes}</p>}
                </li>
              ))}
            </ol>
            <NouvelleEtape onAjout={async (data) => setC(await api.ajouterEtape(id, data))} />
          </div>
        </div>

        <IaAssistant candidature={c} />
      </div>
    </>
  )
}

function DocLien({ doc }) {
  if (!doc) return '—'
  return <button className="link" onClick={() => api.ouvrirDocument(doc.id)}>{doc.nomFichier}</button>
}

function NouvelleEtape({ onAjout }) {
  const [form, setForm] = useState({ type: 'ECHANGE_RH', date: aujourdhui(), notes: '' })
  const [erreur, setErreur] = useState('')

  const onSubmit = async (e) => {
    e.preventDefault()
    try {
      await onAjout(form)
      setForm({ ...form, notes: '' })
      setErreur('')
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <form className="nouvelle-etape" onSubmit={onSubmit}>
      <h4>Ajouter une étape</h4>
      <div className="row">
        <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
          {Object.entries(ETAPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
      </div>
      <textarea rows={3} placeholder="Notes : questions posées, ressenti, prochaine étape…"
        value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
      {erreur && <p className="erreur">{erreur}</p>}
      <button className="btn primary">Ajouter</button>
    </form>
  )
}
