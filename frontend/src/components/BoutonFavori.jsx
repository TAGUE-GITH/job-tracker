import { useEffect, useRef, useState } from 'react'
import { codeFavori } from '../importOffre.js'

// Lien a glisser dans la barre de favoris. React bloque les liens "javascript:" dans le JSX :
// on pose l'adresse directement sur l'element.
export default function BoutonFavori() {
  const lien = useRef(null)
  const [aide, setAide] = useState(false)

  useEffect(() => {
    lien.current?.setAttribute('href', codeFavori())
  }, [])

  return (
    <div className="card favori">
      <div className="favori-tete">
        <div>
          <h3>Ajoute une offre depuis n'importe quel site</h3>
          <p className="muted small">
            Welcome to the Jungle, LinkedIn, Indeed, HelloWork, Apec, site carrière d'une entreprise… :
            un clic sur ce favori envoie l'offre dans ton suivi.
          </p>
        </div>
        <a ref={lien} className="btn primary bouton-favori" onClick={(e) => { e.preventDefault(); setAide(true) }}
          title="Glisse-moi dans ta barre de favoris" draggable>
          Ajouter à Postulyn
        </a>
      </div>
      {aide ? (
        <ol className="favori-etapes">
          <li>Affiche la barre de favoris : <kbd>Ctrl</kbd> + <kbd>Maj</kbd> + <kbd>B</kbd> (<kbd>⌘</kbd> + <kbd>Maj</kbd> + <kbd>B</kbd> sur Mac).</li>
          <li><strong>Glisse</strong> le bouton « Ajouter à Postulyn » dans cette barre (ne clique pas dessus ici).</li>
          <li>Sur la page d'une offre, clique sur le favori. Astuce : sélectionne d'abord le texte de l'annonce si le site est mal lu.</li>
          <li>Postulyn s'ouvre avec l'offre préremplie : vérifie, choisis ton CV, enregistre.</li>
        </ol>
      ) : (
        <button type="button" className="link small" onClick={() => setAide(true)}>Comment l'installer ?</button>
      )}
    </div>
  )
}
