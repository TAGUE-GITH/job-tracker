import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowLeft, Banknote, Briefcase, Building2, CalendarDays, Check, ExternalLink, MapPin, Plus, Sparkles,
} from 'lucide-react'
import { depuis } from '../labels'

const recente = (iso) => iso && (Date.now() - new Date(`${iso}T12:00:00`).getTime()) / 86400000 <= 2
const teletravail = (o) => /t[ée]l[ée]travail|remote|homeoffice/i.test(`${o.lieu ?? ''} ${o.titre ?? ''}`)

/**
 * Resultats d'offres : liste compacte a gauche, detail de l'offre choisie a droite (en plein ecran sur telephone).
 * items : [{ offre, score?, motsTrouves? }]
 */
export default function ListeDetailOffres({ items, suivies, onAjouter, onOuvrir }) {
  const [choisieId, setChoisieId] = useState(null)
  const [detailMobile, setDetailMobile] = useState(false)
  const detailRef = useRef(null)
  const choisie = items.find((i) => i.offre.id === choisieId) ?? items[0]

  // Nouvelle liste : on revient en haut du detail
  useEffect(() => { detailRef.current?.scrollTo?.({ top: 0 }) }, [choisie?.offre.id])

  const choisir = (id) => {
    setChoisieId(id)
    setDetailMobile(true)
  }

  if (!items.length) return null

  return (
    <div className={`resultats-offres ${detailMobile ? 'detail-ouvert' : ''}`}>
      <ul className="liste-offres" aria-label="Offres">
        {items.map(({ offre, score }) => (
          <li key={offre.id}>
            <button type="button" className={`ligne-offre ${choisie?.offre.id === offre.id ? 'active' : ''}`}
              onClick={() => choisir(offre.id)} aria-current={choisie?.offre.id === offre.id}>
              <span className="ligne-offre-tete">
                <strong>{offre.titre}</strong>
                {score != null && <span className={`correspondance ${score >= 70 ? 'forte' : score >= 45 ? 'moyenne' : ''}`}>{score} %</span>}
              </span>
              <span className="ligne-offre-infos">
                {offre.entreprise || 'Entreprise confidentielle'}
                {offre.lieu && <> · {offre.lieu}</>}
              </span>
              <span className="ligne-offre-tags">
                {recente(offre.datePublication) && <span className="tag-offre nouveau">Nouveau</span>}
                {offre.contrat && <span className="tag-offre">{offre.contrat}</span>}
                {teletravail(offre) && <span className="tag-offre">Télétravail</span>}
                {suivies[offre.url] && <span className="tag-offre suivie"><Check size={12} /> Suivie</span>}
                <span className="ligne-offre-date">{depuis(offre.datePublication)}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>

      {choisie && (
        <DetailOffre key={choisie.offre.id} item={choisie} candidatureId={suivies[choisie.offre.url]}
          onAjouter={onAjouter} onOuvrir={onOuvrir} onFermer={() => setDetailMobile(false)} refContenu={detailRef} />
      )}
    </div>
  )
}

function DetailOffre({ item, candidatureId, onAjouter, onOuvrir, onFermer, refContenu }) {
  const { offre, score, motsTrouves } = item
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState('')
  const [aPostule, setAPostule] = useState(false)

  /** Ajoute l'offre au suivi (si besoin) puis ouvre la candidature, eventuellement sur un outil d'ORVYNA. */
  const ouvrir = async (outil) => {
    setEnvoi(true)
    setErreur('')
    try {
      const id = candidatureId ?? (await onAjouter(offre)).id
      onOuvrir(id, outil)
    } catch (e) {
      setErreur(e.message)
    } finally {
      setEnvoi(false)
    }
  }

  const description = (offre.description ?? '').trim()
  const courte = description.length < 400

  return (
    <article className="detail-offre card" ref={refContenu} aria-label={offre.titre}>
      <button type="button" className="btn ghost small retour-liste" onClick={onFermer}>
        <ArrowLeft size={15} /> Retour aux offres
      </button>
      <header className="detail-offre-tete">
        <h2>{offre.titre}</h2>
        <ul className="detail-offre-infos">
          <li><Building2 size={16} /> {offre.entreprise || 'Entreprise confidentielle'}</li>
          {offre.lieu && <li><MapPin size={16} /> {offre.lieu}</li>}
          {offre.contrat && <li><Briefcase size={16} /> {offre.contrat}</li>}
          {offre.salaire && <li><Banknote size={16} /> {offre.salaire}</li>}
          {offre.datePublication && <li><CalendarDays size={16} /> Publiée {depuis(offre.datePublication).toLowerCase()}</li>}
        </ul>
        <div className="detail-offre-actions">
          {offre.url && (
            <a href={offre.url} target="_blank" rel="noopener noreferrer" className="btn primary"
              onClick={() => setAPostule(true)}>
              Postuler <ExternalLink size={15} />
            </a>
          )}
          <button className="btn ghost" onClick={() => ouvrir('cv')} disabled={envoi}
            title="ORVYNA réécrit ton CV pour cette offre">
            <Sparkles size={16} /> Adapter mon CV
          </button>
          {candidatureId ? (
            <Link to={`/candidatures/${candidatureId}`} className="btn ghost"><Check size={16} /> Dans mon suivi</Link>
          ) : (
            <button className="btn ghost" onClick={() => ouvrir()} disabled={envoi}><Plus size={16} /> Suivre</button>
          )}
        </div>
        {aPostule && !candidatureId && (
          <div className="offre-confirm">
            <span>Tu as postulé ? Ajoute l'offre à ton suivi pour ne pas la perdre.</span>
            <button className="btn primary small" onClick={() => ouvrir()} disabled={envoi}>Ajouter au suivi</button>
          </div>
        )}
        {erreur && <p className="erreur">{erreur}</p>}
        {offre.source === 'Adzuna' && (
          // Seule mention de source affichee : exigee par les conditions d'utilisation d'Adzuna
          <a className="mention-adzuna" href="https://www.adzuna.fr" target="_blank" rel="noopener noreferrer">Jobs by Adzuna</a>
        )}
      </header>

      {score != null && (
        <section className="detail-correspondance">
          <span className={`correspondance ${score >= 70 ? 'forte' : score >= 45 ? 'moyenne' : ''}`}>{score} %</span>
          <p>
            <strong>Correspondance avec ton CV.</strong>
            {motsTrouves?.length > 0 && <> En commun : {motsTrouves.join(', ')}.</>}
          </p>
        </section>
      )}

      <section className="detail-offre-description">
        <h3>Description du poste</h3>
        {description ? <p>{description}</p> : <p className="muted">Pas de description fournie.</p>}
        {courte && offre.url && (
          <p className="muted small">
            Ceci est un résumé : <a href={offre.url} target="_blank" rel="noopener noreferrer">consulte l'annonce complète</a> pour
            toutes les missions et le profil recherché.
          </p>
        )}
      </section>
    </article>
  )
}
