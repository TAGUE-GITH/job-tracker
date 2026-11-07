import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ETAPES, aujourdhui } from '../labels'
import { DELAI_RELANCE, aRelancer } from '../relances.js'

// Carte du tableau de bord : les candidatures sans nouvelles, avec un email de relance redige par TAGIA
export default function ARelancer() {
  const [liste, setListe] = useState(null)

  const charger = () => api.candidatures().then((cs) => setListe(aRelancer(cs))).catch(() => setListe([]))
  useEffect(() => {
    charger()
  }, [])

  if (!liste) return null
  return (
    <div className="card relances">
      <h3>🔔 À relancer {liste.length > 0 && <span className="badge orange">{liste.length}</span>}</h3>
      {liste.length === 0 ? (
        <p className="muted small">
          Rien à relancer : aucune candidature sans nouvelles depuis plus de {DELAI_RELANCE} jours. 👍
        </p>
      ) : (
        <>
          <p className="muted small">
            Sans nouvelles depuis plus de {DELAI_RELANCE} jours : une relance courte et polie montre ta motivation.
          </p>
          <ul className="relances-liste">
            {liste.map((r) => <Relance key={r.c.id} {...r} onFait={charger} />)}
          </ul>
        </>
      )}
    </div>
  )
}

function Relance({ c, derniere, jours, onFait }) {
  const [brouillon, setBrouillon] = useState('')
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const [copie, setCopie] = useState(false)

  const rediger = async () => {
    setErreur('')
    setChargement(true)
    try {
      setBrouillon((await api.relance(c.id)).reponse)
    } catch (e) {
      setErreur(e.message)
    } finally {
      setChargement(false)
    }
  }

  const copier = async () => {
    try {
      await navigator.clipboard.writeText(brouillon)
      setCopie(true)
    } catch {
      setErreur('Copie impossible : sélectionne le texte puis Ctrl + C')
    }
  }

  const marquerRelancee = async () => {
    try {
      await api.ajouterEtape(c.id, { type: 'RELANCE', date: aujourdhui(), notes: brouillon || null })
      onFait()
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <li>
      <div className="relance-ligne">
        <div>
          <Link to={`/candidatures/${c.id}`}><strong>{c.nomEntreprise}</strong></Link>
          <span className="muted"> · {c.poste}</span>
          <div className="muted small">
            {derniere ? ETAPES[derniere.type] : 'Candidature'} il y a <strong className="jours">{jours} jours</strong>
          </div>
        </div>
        <div className="relance-actions">
          <button className="btn ghost small" onClick={rediger} disabled={chargement}>
            {chargement ? 'TAGIA écrit…' : brouillon ? 'Réécrire' : '✉️ Rédiger la relance'}
          </button>
          <button className="btn primary small" onClick={marquerRelancee} title="Ajoute une étape Relance aujourd'hui">
            ✓ J'ai relancé
          </button>
        </div>
      </div>
      {erreur && <p className="erreur">{erreur}</p>}
      {brouillon && (
        <div className="relance-brouillon">
          <textarea value={brouillon} onChange={(e) => { setBrouillon(e.target.value); setCopie(false) }} rows={9} />
          <button className="btn ghost small" onClick={copier}>{copie ? 'Copié ✓' : 'Copier'}</button>
        </div>
      )}
    </li>
  )
}
