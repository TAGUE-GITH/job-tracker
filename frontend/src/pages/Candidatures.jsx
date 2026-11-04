import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ETAPES, STATUTS, formatDate } from '../labels'
import StatutBadge from '../components/StatutBadge.jsx'

export default function Candidatures() {
  const [liste, setListe] = useState(null)
  const [statut, setStatut] = useState('')
  const [q, setQ] = useState('')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    const t = setTimeout(() => {
      api.candidatures({ statut, q }).then(setListe).catch((e) => setErreur(e.message))
    }, 250)
    return () => clearTimeout(t)
  }, [statut, q])

  return (
    <>
      <div className="page-head">
        <h1>Mes candidatures</h1>
        <Link to="/candidatures/nouvelle" className="btn primary">+ Nouvelle candidature</Link>
      </div>

      <div className="filtres">
        <input placeholder="Rechercher une entreprise ou un poste…" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={statut} onChange={(e) => setStatut(e.target.value)}>
          <option value="">Tous les statuts</option>
          {Object.entries(STATUTS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </select>
      </div>

      {erreur && <p className="erreur">{erreur}</p>}
      {!liste ? (
        <p className="muted">Chargement…</p>
      ) : liste.length === 0 ? (
        <div className="card vide"><p className="muted">Aucune candidature trouvée.</p></div>
      ) : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Entreprise</th>
                <th>Poste</th>
                <th>Date</th>
                <th>Dernière étape</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              {liste.map((c) => {
                const derniere = c.etapes[c.etapes.length - 1]
                return (
                  <tr key={c.id}>
                    <td><Link to={`/candidatures/${c.id}`}><strong>{c.nomEntreprise}</strong></Link></td>
                    <td>{c.poste}</td>
                    <td>{formatDate(c.dateCandidature)}</td>
                    <td>{derniere ? `${ETAPES[derniere.type]} · ${formatDate(derniere.date)}` : '—'}</td>
                    <td><StatutBadge statut={c.statut} /></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
