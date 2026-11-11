import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ExternalLink, Plus, Sparkles } from 'lucide-react'
import { formatDate } from '../labels'

/**
 * Carte d'une offre : ajout au suivi, CV adapte par ORVYNA, lien pour postuler.
 * score / motsTrouves : correspondance avec le CV (offres recommandees).
 */
export default function CarteOffre({ offre, candidatureId, onAjouter, onOuvrir, score, motsTrouves }) {
  const [ouverte, setOuverte] = useState(false)
  const [aPostule, setAPostule] = useState(false)
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState('')

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

  return (
    <article className="card offre">
      <div className="offre-head">
        <div>
          <h3>{offre.titre}</h3>
          <p className="muted">
            {offre.entreprise || 'Entreprise non communiquée'}{offre.lieu && ` · ${offre.lieu}`}
          </p>
        </div>
        <div className="offre-head-droite">
          {score != null && (
            <span className={`correspondance ${score >= 70 ? 'forte' : score >= 45 ? 'moyenne' : ''}`}
              title="Correspondance avec ton CV">
              {score} %
            </span>
          )}
          {offre.datePublication && <span className="muted small nowrap">{formatDate(offre.datePublication)}</span>}
        </div>
      </div>

      <div className="offre-tags">
        {offre.contrat && <span className="badge bleu">{offre.contrat}</span>}
        {offre.salaire && <span className="badge vert">{offre.salaire}</span>}
        <span className="badge gris">{offre.source}</span>
        {candidatureId && <span className="badge orange">Dans ton suivi</span>}
      </div>

      {motsTrouves?.length > 0 && (
        <p className="mots-trouves small">
          <strong>En commun avec ton CV :</strong> {motsTrouves.join(', ')}
        </p>
      )}

      {offre.description && (
        <>
          <p className={`offre-desc ${ouverte ? 'ouverte' : ''}`}>{offre.description}</p>
          <button type="button" className="link small" onClick={() => setOuverte(!ouverte)}>
            {ouverte ? 'Réduire' : 'Lire la suite'}
          </button>
        </>
      )}

      {aPostule && !candidatureId && (
        <div className="offre-confirm">
          <span>Tu as postulé ? Ajoute l'offre à ton suivi pour ne pas la perdre.</span>
          <button className="btn primary small" onClick={() => ouvrir()} disabled={envoi}>
            {envoi ? 'Ajout…' : 'Oui, ajouter au suivi'}
          </button>
        </div>
      )}
      {erreur && <p className="erreur">{erreur}</p>}

      <div className="offre-actions">
        {candidatureId ? (
          <Link to={`/candidatures/${candidatureId}`} className="btn ghost">Voir dans mon suivi</Link>
        ) : (
          <button className="btn ghost" onClick={() => ouvrir()} disabled={envoi}><Plus size={16} /> Ajouter au suivi</button>
        )}
        <button className="btn ghost" onClick={() => ouvrir('cv')} disabled={envoi}
          title="ORVYNA réécrit ton CV pour cette offre">
          <Sparkles size={16} /> Adapter mon CV
        </button>
        {offre.url && (
          <a href={offre.url} target="_blank" rel="noopener noreferrer" className="btn primary"
            onClick={() => setAPostule(true)}>
            Postuler <ExternalLink size={15} />
          </a>
        )}
      </div>
    </article>
  )
}
