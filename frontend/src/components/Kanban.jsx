import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ETAPES, STATUTS, aujourdhui, formatDate } from '../labels'
import { aRelancer } from '../relances.js'

const COLONNES = ['EN_ATTENTE', 'EN_COURS', 'OFFRE_RECUE', 'REFUSEE', 'ABANDONNEE']
const ETAPES_EN_COURS = ['ECHANGE_RH', 'TEST_TECHNIQUE', 'ENTRETIEN_TECHNIQUE', 'ENTRETIEN_MANAGER']

// Le statut decoule des etapes : deplacer une carte ajoute l'etape correspondante
export default function Kanban({ liste, onMaj }) {
  const [survol, setSurvol] = useState(null)
  const [choix, setChoix] = useState(null)
  const [message, setMessage] = useState('')
  const relancer = new Set(aRelancer(liste).map((r) => r.c.id))

  const requete = (c, abandonnee) => ({
    nomEntreprise: c.nomEntreprise, poste: c.poste, lienOffre: c.lienOffre, texteOffre: c.texteOffre,
    dateCandidature: c.dateCandidature, cvId: c.cv?.id ?? null, lettreId: c.lettre?.id ?? null, abandonnee,
  })

  const executer = async (action, texte) => {
    setMessage('')
    try {
      onMaj(await action())
      setMessage(texte)
    } catch (e) {
      setMessage(e.message)
    }
  }

  const ajouterEtape = async (c, type) => {
    let cible = c
    if (c.statut === 'ABANDONNEE') cible = await api.modifierCandidature(c.id, requete(c, false))
    return api.ajouterEtape(cible.id, { type, date: aujourdhui(), notes: null })
  }

  const deplacer = (c, statut) => {
    setChoix(null)
    if (!c || c.statut === statut) return
    const nom = `${c.nomEntreprise}`
    switch (statut) {
      case 'EN_COURS':
        setChoix(c)
        break
      case 'OFFRE_RECUE':
        executer(() => ajouterEtape(c, 'OFFRE'), `Offre reçue chez ${nom} : étape ajoutée.`)
        break
      case 'REFUSEE':
        executer(() => ajouterEtape(c, 'REFUS'), `Refus enregistré pour ${nom}. Courage, la suivante sera la bonne !`)
        break
      case 'ABANDONNEE':
        executer(() => api.modifierCandidature(c.id, requete(c, true)), `${nom} marquée comme abandonnée.`)
        break
      case 'EN_ATTENTE':
        if (c.statut === 'ABANDONNEE') {
          executer(() => api.modifierCandidature(c.id, requete(c, false)), `${nom} n'est plus abandonnée.`)
        } else {
          setMessage("Pour revenir « En attente », supprime la dernière étape depuis la page de la candidature.")
        }
        break
      default:
    }
  }

  const deposer = (e, statut) => {
    e.preventDefault()
    setSurvol(null)
    deplacer(liste.find((c) => String(c.id) === e.dataTransfer.getData('text/plain')), statut)
  }

  return (
    <>
      {message && <p className="kanban-message">{message}</p>}
      {choix && (
        <div className="card kanban-choix">
          <strong>{choix.nomEntreprise} avance : quelle étape ?</strong>
          <div className="row">
            {ETAPES_EN_COURS.map((t) => (
              <button key={t} className="btn ghost small"
                onClick={() => { const c = choix; setChoix(null); executer(() => ajouterEtape(c, t), `${ETAPES[t]} ajouté pour ${c.nomEntreprise}.`) }}>
                {ETAPES[t]}
              </button>
            ))}
            <button className="btn small link" onClick={() => setChoix(null)}>Annuler</button>
          </div>
        </div>
      )}

      <div className="kanban">
        {COLONNES.map((statut) => {
          const cartes = liste.filter((c) => c.statut === statut)
          return (
            <section key={statut} className={`kanban-col col-${STATUTS[statut].couleur} ${survol === statut ? 'survol' : ''}`}
              onDragOver={(e) => { e.preventDefault(); setSurvol(statut) }}
              onDragLeave={() => setSurvol((s) => (s === statut ? null : s))}
              onDrop={(e) => deposer(e, statut)}>
              <header>
                <span>{STATUTS[statut].label}</span>
                <span className="kanban-compte">{cartes.length}</span>
              </header>
              {cartes.map((c) => {
                const derniere = [...c.etapes].sort((a, b) => a.date.localeCompare(b.date)).at(-1)
                return (
                  <article key={c.id} className="kanban-carte" draggable
                    onDragStart={(e) => e.dataTransfer.setData('text/plain', String(c.id))}>
                    <Link to={`/candidatures/${c.id}`} className="kanban-titre">{c.nomEntreprise}</Link>
                    <span className="kanban-poste">{c.poste}</span>
                    {derniere && <span className="muted small">{ETAPES[derniere.type]} · {formatDate(derniere.date)}</span>}
                    {relancer.has(c.id) && <span className="badge orange">À relancer</span>}
                    <select className="kanban-deplacer" value="" aria-label={`Déplacer ${c.nomEntreprise}`}
                      onChange={(e) => deplacer(c, e.target.value)}>
                      <option value="" disabled>Déplacer vers…</option>
                      {COLONNES.filter((s) => s !== statut).map((s) => <option key={s} value={s}>{STATUTS[s].label}</option>)}
                    </select>
                  </article>
                )
              })}
              {cartes.length === 0 && <p className="kanban-vide">Glisse une carte ici</p>}
            </section>
          )
        })}
      </div>
    </>
  )
}
