import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import {
  Briefcase, Check, FileText, Inbox, LayoutDashboard, LogOut, RefreshCw, Reply, RotateCcw, Search, ShieldCheck,
  ShieldOff, Star, Trash2, UserPlus, Users,
} from 'lucide-react'
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
  const [onglet, setOnglet] = useState('apercu')

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

  const changerRole = async (u, admin) => {
    const question = admin
      ? `Nommer ${u.nom} (${u.email}) administrateur ? Il aura accès à cette page et pourra gérer tous les comptes.`
      : `Retirer les droits d'administrateur de ${u.nom} (${u.email}) ?`
    if (!confirm(question)) return
    setErreur('')
    try {
      await api.adminRole(u.id, admin)
      setMessage(admin ? `${u.nom} est maintenant administrateur.` : `${u.nom} n'est plus administrateur.`)
      setUtilisateurs((liste) => liste.map((x) => (x.id === u.id ? { ...x, admin } : x)))
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

      <div className="tabs onglets-admin" role="tablist">
        <button type="button" role="tab" aria-selected={onglet === 'apercu'} className={onglet === 'apercu' ? 'active' : ''}
          onClick={() => setOnglet('apercu')}><LayoutDashboard size={16} /> Vue d'ensemble</button>
        <button type="button" role="tab" aria-selected={onglet === 'messages'} className={onglet === 'messages' ? 'active' : ''}
          onClick={() => setOnglet('messages')}>
          <Inbox size={16} /> Messages
          {stats?.messagesNonTraites > 0 && <span className="pastille">{stats.messagesNonTraites}</span>}
        </button>
        <button type="button" role="tab" aria-selected={onglet === 'avis'} className={onglet === 'avis' ? 'active' : ''}
          onClick={() => setOnglet('avis')}><Star size={16} /> Avis</button>
      </div>

      {onglet === 'messages' && <MessagesAdmin onChange={charger} />}
      {onglet === 'avis' && <AvisAdmin />}

      {onglet === 'apercu' && stats && (
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

      {onglet === 'apercu' && (
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
                  <td data-label="Inscription" className="nowrap">{dateCourte(u.dateCreation)}</td>
                  <td data-label="Candidatures">{u.candidatures}</td>
                  <td data-label="Documents">{u.documents}</td>
                  <td data-label="Rôle">
                    {u.principal ? <span className="badge bleu" title="Défini par ADMIN_EMAILS sur Render">Administrateur principal</span>
                      : u.admin ? <span className="badge bleu">Administrateur</span>
                        : u.demo ? <span className="badge orange">Démo</span>
                          : <span className="badge gris">Utilisateur</span>}
                  </td>
                  <td className="td-actions">
                    <div className="row">
                      {u.id !== user.id && !u.principal && !u.demo && (u.admin ? (
                        <button className="btn ghost small carre" onClick={() => changerRole(u, false)}
                          title="Retirer les droits d'administrateur" aria-label={`Retirer les droits d'administrateur de ${u.nom}`}>
                          <ShieldOff size={15} />
                        </button>
                      ) : (
                        <button className="btn ghost small carre" onClick={() => changerRole(u, true)}
                          title="Nommer administrateur" aria-label={`Nommer ${u.nom} administrateur`}>
                          <ShieldCheck size={15} />
                        </button>
                      ))}
                      <button className="btn ghost small carre" onClick={() => deconnecter(u)}
                        title="Déconnecter toutes ses sessions" aria-label={`Déconnecter ${u.nom}`}>
                        <LogOut size={15} />
                      </button>
                      {!u.admin && (
                        <button className="btn danger small carre" onClick={() => supprimer(u)}
                          title="Supprimer le compte et ses données" aria-label={`Supprimer le compte de ${u.nom}`}>
                          <Trash2 size={15} />
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
      )}
    </>
  )
}

const CATEGORIES = { PROBLEME: ['Problème', 'rouge'], QUESTION: ['Question', 'bleu'], SUGGESTION: ['Suggestion', 'vert'], AUTRE: ['Autre', 'gris'] }
const dateHeure = (iso) => new Date(iso).toLocaleString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })

/** Messages envoyes depuis la page Contact : non traites en premier. */
function MessagesAdmin({ onChange }) {
  const [messages, setMessages] = useState(null)
  const [ouvert, setOuvert] = useState(null)
  const [erreur, setErreur] = useState('')

  useEffect(() => { api.adminMessages().then(setMessages).catch((e) => setErreur(e.message)) }, [])

  const traiter = async (m, traite) => {
    try {
      await api.adminTraiterMessage(m.id, traite)
      setMessages((liste) => liste.map((x) => (x.id === m.id ? { ...x, traite } : x)))
      onChange()
    } catch (e) {
      setErreur(e.message)
    }
  }

  const supprimer = async (m) => {
    if (!confirm(`Supprimer le message « ${m.sujet} » de ${m.nom} ?`)) return
    try {
      await api.adminSupprimerMessage(m.id)
      setMessages((liste) => liste.filter((x) => x.id !== m.id))
      onChange()
    } catch (e) {
      setErreur(e.message)
    }
  }

  if (erreur) return <p className="erreur">{erreur}</p>
  if (!messages) return <p className="muted">Chargement…</p>
  if (messages.length === 0) {
    return <div className="card vide"><p className="muted">Aucun message pour l'instant.</p></div>
  }

  return (
    <div className="card liste-messages">
      {messages.map((m) => {
        const [libelle, couleur] = CATEGORIES[m.categorie] ?? [m.categorie, 'gris']
        const deplie = ouvert === m.id
        return (
          <article key={m.id} className={`message-admin ${m.traite ? 'traite' : ''}`}>
            <button type="button" className="message-entete" onClick={() => setOuvert(deplie ? null : m.id)} aria-expanded={deplie}>
              <span className={`point ${m.traite ? '' : 'non-lu'}`} aria-hidden="true" />
              <span className="message-titre">
                <strong>{m.sujet}</strong>
                <small>{m.nom} · {m.email}{m.userId ? '' : ' · visiteur'}</small>
              </span>
              <span className={`badge ${couleur}`}>{libelle}</span>
              <small className="muted nowrap">{dateHeure(m.dateCreation)}</small>
            </button>
            {deplie && (
              <div className="message-corps">
                <p>{m.message}</p>
                <div className="row">
                  <a className="btn primary small" href={`mailto:${m.email}?subject=${encodeURIComponent(`Re: ${m.sujet}`)}`}>
                    <Reply size={14} /> Répondre par email
                  </a>
                  <button className="btn ghost small" onClick={() => traiter(m, !m.traite)}>
                    {m.traite ? <><RotateCcw size={14} /> Marquer non traité</> : <><Check size={14} /> Marquer traité</>}
                  </button>
                  <button className="btn danger small" onClick={() => supprimer(m)}><Trash2 size={14} /> Supprimer</button>
                </div>
              </div>
            )}
          </article>
        )
      })}
    </div>
  )
}

/** Avis des utilisateurs : note moyenne, repartition et commentaires. */
function AvisAdmin() {
  const [synthese, setSynthese] = useState(null)
  const [erreur, setErreur] = useState('')
  useEffect(() => { api.adminAvis().then(setSynthese).catch((e) => setErreur(e.message)) }, [])

  if (erreur) return <p className="erreur">{erreur}</p>
  if (!synthese) return <p className="muted">Chargement…</p>
  const max = Math.max(1, ...Object.values(synthese.parNote))

  return (
    <>
      <div className="admin-grille">
        <div className="card avis-synthese">
          <span className="muted">Note moyenne</span>
          <strong>{synthese.nombre ? synthese.moyenne.toLocaleString('fr-FR') : '—'}<small> / 5</small></strong>
          <Etoiles note={Math.round(synthese.moyenne)} />
          <span className="muted small">{synthese.nombre} avis</span>
        </div>
        <div className="card groupes">
          <h3>Répartition des notes</h3>
          <ul className="statuts-liste">
            {[5, 4, 3, 2, 1].map((n) => (
              <li key={n}>
                <div className="groupe-ligne"><span>{n} étoile{n > 1 ? 's' : ''}</span><strong>{synthese.parNote[n] ?? 0}</strong></div>
                <div className="jauge"><span style={{ width: `${((synthese.parNote[n] ?? 0) / max) * 100}%` }} /></div>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="card liste-messages">
        {synthese.avis.length === 0 && <p className="muted">Aucun avis pour l'instant.</p>}
        {synthese.avis.map((a) => (
          <article key={a.id} className="avis-admin">
            <div className="avis-tete">
              <Etoiles note={a.note} />
              <strong>{a.nom}</strong>
              {a.email && <small className="muted">{a.email}</small>}
              <small className="muted nowrap">{dateHeure(a.dateCreation)}</small>
            </div>
            {a.commentaire ? <p>{a.commentaire}</p> : <p className="muted small">Sans commentaire.</p>}
          </article>
        ))}
      </div>
    </>
  )
}

function Etoiles({ note }) {
  return (
    <span className="etoiles-lecture" aria-label={`${note} sur 5`}>
      {[1, 2, 3, 4, 5].map((n) => <Star key={n} size={15} className={n <= note ? 'pleine' : ''} />)}
    </span>
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
