import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ETAPES, STATUTS, formatDate } from '../labels'
import StatutBadge from '../components/StatutBadge.jsx'
import BoutonFavori from '../components/BoutonFavori.jsx'
import Kanban from '../components/Kanban.jsx'
import { Columns3, List, Plus } from 'lucide-react'

const lireVue = () => {
  // ?vue=kanban dans l'URL l'emporte (lien direct vers la vue Kanban)
  const demandee = new URLSearchParams(window.location.search).get('vue')
  if (demandee === 'kanban' || demandee === 'liste') return demandee
  try {
    return localStorage.getItem('jt_vue') || 'liste'
  } catch {
    return 'liste'
  }
}

export default function Candidatures() {
  const [liste, setListe] = useState(null)
  const [statut, setStatut] = useState('')
  const [q, setQ] = useState('')
  const [erreur, setErreur] = useState('')
  const [vue, setVue] = useState(lireVue)

  const changerVue = (v) => {
    setVue(v)
    try {
      localStorage.setItem('jt_vue', v)
    } catch {
      // stockage indisponible : la vue n'est simplement pas memorisee
    }
  }

  useEffect(() => {
    const t = setTimeout(() => {
      api.candidatures({ statut: vue === 'kanban' ? '' : statut, q }).then(setListe).catch((e) => setErreur(e.message))
    }, 250)
    return () => clearTimeout(t)
  }, [statut, q, vue])

  return (
    <>
      <div className="page-head">
        <h1>Mes candidatures</h1>
        <div className="actions">
          <div className="tabs vues" role="tablist">
            <button type="button" className={vue === 'liste' ? 'active' : ''} onClick={() => changerVue('liste')}><List size={16} /> Liste</button>
            <button type="button" className={vue === 'kanban' ? 'active' : ''} onClick={() => changerVue('kanban')}><Columns3 size={16} /> Kanban</button>
          </div>
          <Link to="/candidatures/nouvelle" className="btn primary"><Plus size={16} /> Nouvelle candidature</Link>
        </div>
      </div>

      <div className="filtres">
        <input placeholder="Rechercher une entreprise ou un poste…" value={q} onChange={(e) => setQ(e.target.value)} />
        {vue === 'liste' && <select value={statut} onChange={(e) => setStatut(e.target.value)}>
          <option value="">Tous les statuts</option>
          {Object.entries(STATUTS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </select>}
      </div>

      {erreur && <p className="erreur">{erreur}</p>}
      {!liste ? (
        <p className="muted">Chargement…</p>
      ) : vue === 'kanban' ? (
        <Kanban liste={liste} onMaj={(maj) => setListe((l) => l.map((c) => (c.id === maj.id ? maj : c)))} />
      ) : liste.length === 0 ? (
        <div className="card vide"><p className="muted">Aucune candidature trouvée.</p></div>
      ) : (
        <div className="card table-wrap table-cards">
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
                    <td data-label="Entreprise"><Link to={`/candidatures/${c.id}`}><strong>{c.nomEntreprise}</strong></Link></td>
                    <td data-label="Poste">{c.poste}</td>
                    <td data-label="Date">{formatDate(c.dateCandidature)}</td>
                    <td data-label="Dernière étape">{derniere ? `${ETAPES[derniere.type]} · ${formatDate(derniere.date)}` : '—'}</td>
                    <td data-label="Statut"><StatutBadge statut={c.statut} /></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <BoutonFavori />
    </>
  )
}
