import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend,
} from 'recharts'
import { api } from '../api'
import { STATUTS } from '../labels'
import ARelancer from '../components/ARelancer.jsx'

// Pas d'animation des graphiques si l'utilisateur a demande a reduire les animations (accessibilite)
const ANIMER = !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

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
        {stats.delaiMoyenReponse != null && (
          <Kpi label="Délai moyen de réponse" valeur={`${stats.delaiMoyenReponse} j`} />
        )}
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
              <Bar dataKey="count" name="Candidatures" fill="#6366f1" radius={[4, 4, 0, 0]} isAnimationActive={ANIMER} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3>Répartition par statut</h3>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={parStatut} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2} isAnimationActive={ANIMER}>
                {parStatut.map((s) => <Cell key={s.name} fill={s.couleur} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid-2">
        <Groupes titre="📄 Quel CV marche le mieux ?" groupes={stats.parCv}
          aide="Taux de réponse selon le CV envoyé. Compare deux versions de ton CV sur plusieurs candidatures." />
        <Groupes titre="🌐 D'où viennent tes réponses ?" groupes={stats.parSource}
          aide="Taux de réponse selon le site de l'offre (d'après le lien de l'offre)." />
      </div>

      <div className="card">
        <h3>Entonnoir de recrutement</h3>
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={stats.entonnoir} layout="vertical" margin={{ left: 30 }}>
            <CartesianGrid strokeDasharray="3 3" horizontal={false} />
            <XAxis type="number" allowDecimals={false} />
            <YAxis type="category" dataKey="etape" width={110} />
            <Tooltip />
            <Bar dataKey="count" name="Candidatures" fill="#22c55e" radius={[0, 4, 4, 0]} isAnimationActive={ANIMER} />
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

function Groupes({ titre, groupes, aide }) {
  if (!groupes?.length) return null
  // Le badge n'a de sens que s'il y a au moins deux groupes comparables (2 candidatures ou plus)
  const comparables = groupes.filter((g) => g.total >= 2)
  const meilleur = comparables.length >= 2 ? Math.max(...comparables.map((g) => g.tauxReponse)) : -1
  return (
    <div className="card groupes">
      <h3>{titre}</h3>
      <p className="muted small">{aide}</p>
      <ul>
        {groupes.map((g) => (
          <li key={g.nom}>
            <div className="groupe-ligne">
              <span className="groupe-nom" title={g.nom}>
                {g.nom}
                {g.total >= 2 && g.tauxReponse === meilleur && meilleur > 0 && <span className="badge vert">🏆 meilleur</span>}
              </span>
              <span className="muted small nowrap">{g.reponses}/{g.total} · <strong>{g.tauxReponse} %</strong></span>
            </div>
            <div className="jauge"><span style={{ width: `${Math.max(g.tauxReponse, 2)}%` }} /></div>
          </li>
        ))}
      </ul>
    </div>
  )
}
