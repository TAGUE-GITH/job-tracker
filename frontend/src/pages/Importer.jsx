import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { lireOffreImportee, mettreOffreDeCote, oublierOffre } from '../importOffre.js'

// Arrivee depuis le favori "Ajouter a Taguora" : on ouvre le formulaire de candidature prerempli
export default function Importer() {
  const { user } = useAuth()
  const { hash } = useLocation()
  const [offre] = useState(() => lireOffreImportee(hash))

  useEffect(() => {
    // Pas connecte : on garde l'offre le temps de la connexion ; connecte : elle est utilisee
    if (!user && hash.length > 1) mettreOffreDeCote(hash)
    if (user) oublierOffre()
  }, [user, hash])

  if (!user) return <Navigate to="/login" replace />
  if (!offre) {
    return (
      <div className="container">
        <div className="card vide">
          <p>Aucune offre à importer.</p>
          <p className="muted small">Utilise le favori « Ajouter à Taguora » sur la page d'une offre d'emploi.</p>
          <Link to="/offres" className="btn primary">Retour aux offres</Link>
        </div>
      </div>
    )
  }
  return <Navigate to="/candidatures/nouvelle" replace state={{ offre }} />
}
