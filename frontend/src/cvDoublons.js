const MOTS_GENERIQUES = new Set(['group', 'groupe', 'sarl', 'sas', 'sasu', 'inc', 'ltd', 'the', 'les', 'des', 'and'])
// Minuscules sans accents, pour comparer "Société" et "societe"
const normaliser = (s) => (s ?? '').toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')

/** Vrai si le projet decrit un travail deja presente dans une experience (meme entreprise citee). */
export function projetEnDoublon(projet, experiences) {
  const texteProjet = normaliser(`${projet.nom} ${projet.description}`)
  return (experiences ?? []).some((x) => normaliser(x.entreprise)
    .split(/[^a-z0-9]+/)
    .filter((mot) => mot.length >= 3 && !MOTS_GENERIQUES.has(mot))
    .some((mot) => new RegExp(`\\b${mot}\\b`).test(texteProjet)))
}
