// Meme regle que le serveur (MotDePasse.REGLE) : 8 caracteres minimum, une lettre et un chiffre
export const regleRespectee = (mdp = '') => mdp.length >= 8 && /\p{L}/u.test(mdp) && /\d/.test(mdp)

const LIBELLES = ['Trop faible', 'Faible', 'Correct', 'Bon', 'Excellent']

/** Estimation simple de la force : longueur et variete des caracteres (niveau 0 a 4). */
export function forceMotDePasse(mdp = '') {
  if (!mdp) return { niveau: 0, libelle: LIBELLES[0] }
  let points = 0
  if (mdp.length >= 8) points++
  if (mdp.length >= 12) points++
  if (/\p{Ll}/u.test(mdp) && /\p{Lu}/u.test(mdp)) points++
  if (/\d/.test(mdp) && /\p{L}/u.test(mdp)) points++
  if (/[^\p{L}\d]/u.test(mdp)) points++
  const niveau = regleRespectee(mdp) ? Math.max(1, Math.min(4, points - 1)) : 0
  return { niveau, libelle: LIBELLES[niveau] }
}
