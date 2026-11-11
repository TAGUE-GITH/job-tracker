import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { Briefcase, FileText, LogOut, RefreshCw, Search, Trash2, UserPlus, Users } from 'lucide-react'
import { api } from '../api'
import { useAuth } from '../auth.jsx'
import { STATUTS, initiales } from '../labels'

const ANIMER = typeof window === 'undefined' || !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
const dateCourte = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '—')
const semaine = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })

export default function Administration() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [utilisateurs, setUtilisateurs] = useState([])
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')
  const [filtre, setFiltre] = useState('')
  const [avecDemo, setAvecDemo] = useState(false)
  const [chargement, setChargement] = useState(true)

  const recuperer = () => Promise.all([api.adminStatistiques(), api.adminUtilisateurs()])
    .then(([s, u]) => { setStats(s); setUtilisateurs(u); setErreur('') })
    .catch((e) => setErreur(e.message))
    .finally(() => setChargement(false))

  const charger = () => {
    setChargement(true)
    return recuperer()
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { if (user?.admin) recuperer() }, [user?.admin])

  const visibles = useMemo(() => {
    const q = filtre.trim().toLowerCase()
    return utilisateurs
      .filter((u) => avecDemo || !u.demo)
      .filter((u) => !q || u.nom.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
  }, [utilisateurs, filtre, avecDemo])

  if (!user?.admin) return <Navigate to="/" replace />

  const deconnecter = async (u) => {
    if (!confirm(`Déconnecter toutes les sessions de ${u.nom} (${u.email}) ?`)) return
    try {
      await api.adminDeconnecter(u.id)
      setMessage(`Les sessions de ${u.nom} ont été fermées.`)
    } catch (e) {
      setErreur(e.message)
    }
  }

  const supprimer = async (u) => {
    if (!confirm(`Supprimer définitivement le compte de ${u.nom} (${u.email}) et toutes ses données ? Cette action est irréversible.`)) return
    try {
      await api.adminSupprimer(u.id)
      setMessage(`Le compte de ${u.nom} a été supprimé.`)
      await charger()
    } catch (e) {
      setErreur(e.message)
    }
  }

  const statuts = stats ? Object.entries(stats.candidaturesParStatut) : []
  const maxStatut = Math.max(1, ...statuts.map(([, n]) => n))

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Administration</h1>
          <p className="muted">Vue d'ensemble de la plateforme et gestion des comptes.</p>
        </div>
        <button className="btn ghost" onClick={charger} disabled={chargement}>
          <RefreshCw size={16} /> Actualiser
        </button>
      </div>

      {erreur && <p className="erreur">{erreur}</p>}
      {message && <p className="succes">{message}</p>}

      {stats && (
        <>
          <div className="admin-kpis">
            <Kpi icone={Users} titre="Utilisateurs" valeur={stats.utilisateurs}
              detail={`Hors ${stats.comptesDemo} compte(s) de démonstration`} />
            <Kpi icone={UserPlus} titre="Nouveaux inscrits" valeur={stats.inscrits7Jours} detail={`${stats.inscrits30Jours} sur 30 jours`} />
            <Kpi icone={Briefcase} titre="Candidatures suivies" valeur={stats.candidatures}
              detail={stats.utilisateurs > 0
                ? `${(stats.candidatures / stats.utilisateurs).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} par compte en moyenne`
                : '—'} />
            <Kpi icone={FileText} titre="Documents" valeur={stats.documents} detail="CV et lettres enregistrés" />
          </div>

          <div className="admin-grille">
            <div className="card">
              <h3>Activité des 8 dernières semaines</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={stats.activite.map((s) => ({ ...s, semaine: semaine(s.debut) }))}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="semaine" tick={{ fontSize: 12 }} stroke="var(--muted)" />
                  <YAxis allowDecimals={false} tick={{ fontSize: 12 }} stroke="var(--muted)" width={32} />
                  <Tooltip />
                  <Legend wrapperStyle={{ fontSize: 13 }} />
                  <Bar dataKey="inscriptions" name="Inscriptions" fill="#1e3a8a" radius={[3, 3, 0, 0]} isAnimationActive={ANIMER} />
                  <Bar dataKey="candidatures" name="Candidatures" fill="#60a5fa" radius={[3, 3, 0, 0]} isAnimationActive={ANIMER} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="card groupes">
              <h3>Candidatures par statut</h3>
              <ul className="statuts-liste">
                {statuts.map(([statut, n]) => (
                  <li key={statut}>
                    <div className="groupe-ligne">
                      <span className={`badge ${STATUTS[statut]?.couleur ?? 'gris'}`}>{STATUTS[statut]?.label ?? statut}</span>
                      <strong>{n}</strong>
                    </div>
                    <div className="jauge"><span style={{ width: `${(n / maxStatut) * 100}%` }} /></div>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </>
      )}

      <div className="card table-wrap table-cards">
        <div className="admin-outils">
          <h3>Comptes ({visibles.length})</h3>
          <label className="checkbox">
            <input type="checkbox" checked={avecDemo} onChange={(e) => setAvecDemo(e.target.checked)} />
            Afficher les comptes de démonstration
          </label>
          <div className="recherche">
            <Search size={16} />
            <input type="search" value={filtre} onChange={(e) => setFiltre(e.target.value)}
              placeholder="Rechercher un nom ou un email" aria-label="Rechercher un compte" />
          </div>
        </div>
        {chargement && !stats ? <p className="muted vide">Chargement…</p> : (
          <table>
            <thead>
              <tr><th>Utilisateur</th><th>Inscription</th><th>Candidatures</th><th>Documents</th><th>Rôle</th><th /></tr>
            </thead>
            <tbody>
              {visibles.map((u) => (
                <tr key={u.id}>
                  <td data-label="Utilisateur">
                    <div className="cellule-user">
                      <span className="avatar-initiales" aria-hidden="true">{initiales(u.nom)}</span>
                      <div><strong>{u.nom}</strong><small>{u.email}</small></div>
                    </div>
                  </td>
                  <td data-label="Inscription">{dateCourte(u.dateCreation)}</td>
                  <td data-label="Candidatures">{u.candidatures}</td>
                  <td data-label="Documents">{u.documents}</td>
                  <td data-label="Rôle">
                    {u.admin ? <span className="badge bleu">Administrateur</span>
                      : u.demo ? <span className="badge orange">Démo</span>
                        : <span className="badge gris">Utilisateur</span>}
                  </td>
                  <td className="td-actions">
                    <div className="row">
                      <button className="btn ghost small" onClick={() => deconnecter(u)} title="Fermer toutes ses sessions">
                        <LogOut size={14} /> Déconnecter
                      </button>
                      {!u.admin && (
                        <button className="btn danger small" onClick={() => supprimer(u)} title="Supprimer le compte et ses données">
                          <Trash2 size={14} /> Supprimer
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {visibles.length === 0 && (
                <tr><td colSpan={6} className="muted">Aucun compte ne correspond.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}

function Kpi({ icone: Icone, titre, valeur, detail }) {
  return (
    <div className="card">
      <div className="kpi-tete"><span>{titre}</span><Icone size={18} /></div>
      <strong>{valeur}</strong>
      <small>{detail}</small>
    </div>
  )
}
