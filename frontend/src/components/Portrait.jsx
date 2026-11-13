import { FONDATEUR } from '../fondateur.js'

/** Portrait du fondateur : sa photo, ou ses initiales. */
export default function Portrait({ taille = 120 }) {
  return FONDATEUR.photo
    ? <img src={FONDATEUR.photo} alt={`${FONDATEUR.prenom} ${FONDATEUR.nom}`} className="portrait"
      style={{ width: taille, height: taille }} />
    : <span className="portrait portrait-initiales" style={{ width: taille, height: taille, fontSize: taille * 0.36 }}
      role="img" aria-label={`${FONDATEUR.prenom} ${FONDATEUR.nom}`}>{FONDATEUR.initiales}</span>
}
