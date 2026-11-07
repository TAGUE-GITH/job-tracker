import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend,
} from 'recharts'
import { api } from '../api'
import { STATUTS } from '../labels'
import ARelancer from '../components/ARelancer.jsx'

const COULEURS = { gris: '#94a3b8', bleu: '#3b82f6', vert: '#22c55e', rouge: '#ef4444', orange: '#f59e0b' }

const formatMois = (m) => {
  const [annee, mois] = m.split('-')
  return new Date(annee, mois - 1).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' })
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api.stats().then(setStats).catch((e) => setErreur(e.message))
  }, [])

  if (erreur) return <p className="erreur">{erreur}</p>
  if (!stats) return <p className="muted">Chargement…</p>

  if (stats.total === 0) {
    return (
      <div className="card vide">
        <h2>Bienvenue 👋</h2>
        <p className="muted">Ajoute ta première candidature pour voir tes statistiques.</p>
        <Link to="/candidatures/nouvelle" className="btn primary">+ Nouvelle candidature</Link>
      </div>
    )
  }

  const parStatut = Object.entries(stats.parStatut)
    .filter(([, n]) => n > 0)
    .map(([statut, n]) => ({ name: STATUTS[statut].label, value: n, couleur: COULEURS[STATUTS[statut].couleur] }))

  return (
    <>
      <div className="page-head">
        <h1>Tableau de bord</h1>
        <Link to="/candidatures/nouvelle" className="btn primary">+ Nouvelle candidature</Link>
      </div>

      <div className="kpis">
        <Kpi label="Candidatures" valeur={stats.total} />
        <Kpi label="En cours" valeur={stats.enCours} />
        <Kpi label="Taux de réponse" valeur={`${stats.tauxReponse} %`} />
        <Kpi label="Taux d'entretien" valeur={`${stats.tauxEntretien} %`} />
        <Kpi label="Taux d'offre" valeur={`${stats.tauxOffre} %`} />
      </div>

      <ARelancer />

      <div className="grid-2">
        <div className="card">
          <h3>Candidatures par mois</h3>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={stats.parMois.map((m) => ({ ...m, mois: formatMois(m.mois) }))}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="mois" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" name="Candidatures" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3>Répartition par statut</h3>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={parStatut} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                {parStatut.map((s) => <Cell key={s.name} fill={s.couleur} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card">
        <h3>Entonnoir de recrutement</h3>
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={stats.entonnoir} layout="vertical" margin={{ left: 30 }}>
            <CartesianGrid strokeDasharray="3 3" horizontal={false} />
            <XAxis type="number" allowDecimals={false} />
            <YAxis type="category" dataKey="etape" width={110} />
            <Tooltip />
            <Bar dataKey="count" name="Candidatures" fill="#22c55e" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </>
  )
}

function Kpi({ label, valeur }) {
  return (
    <div className="card kpi">
      <span className="muted">{label}</span>
      <strong>{valeur}</strong>
    </div>
  )
}
