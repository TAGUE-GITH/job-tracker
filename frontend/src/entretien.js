import { normaliserLangage } from './codeLangages.js'

// Ligne ajoutee par ORVYNA a la fin d'un exercice : "EXERCICE_CODE: java"
const MARQUEUR_EXERCICE = /^\s*EXERCICE_CODE:\s*([\w#+.-]+)\s*$/im
export const ETAPES_CODE = ['TEST_TECHNIQUE', 'ENTRETIEN_TECHNIQUE']

/** Texte affiche, sans le marqueur d'exercice. */
export const sansMarqueur = (texte) => texte.replace(new RegExp(MARQUEUR_EXERCICE.source, 'gim'), '').trim()

/** Texte lu a voix haute : sans marqueur, sans blocs de code ni mise en forme Markdown. */
export const pourLaVoix = (texte) => sansMarqueur(texte)
  .replace(/```[\s\S]*?```/g, ' (le code est affiché à l’écran) ')
  .replace(/[*#`_>]/g, '')

/** Exercice de code propose dans un message : langage et code de depart (premier bloc de code). */
export function exercice(texte) {
  const m = texte.match(MARQUEUR_EXERCICE)
  if (!m) return null
  const bloc = texte.match(/```[\w#+.-]*\n([\s\S]*?)```/)
  return { langage: normaliserLangage(m[1]), code: bloc ? bloc[1].replace(/\n$/, '') : '' }
}
