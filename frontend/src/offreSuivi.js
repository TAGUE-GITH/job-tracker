import { api } from './api'
import { aujourdhui } from './labels'

/** Cree la candidature (avec le texte de l'offre, utile a ORVYNA) a partir d'une offre externe. */
export async function creerCandidatureDepuisOffre(offre) {
  const texte = [
    offre.description,
    '',
    `Source : ${offre.source}${offre.lieu ? ` · ${offre.lieu}` : ''}${offre.contrat ? ` · ${offre.contrat}` : ''}`,
    offre.salaire ? `Salaire : ${offre.salaire}` : null,
  ].filter((l) => l !== null).join('\n')
  return api.creerCandidature({
    nomEntreprise: offre.entreprise || 'Entreprise non communiquée',
    poste: offre.titre,
    lienOffre: offre.url?.slice(0, 1000),
    texteOffre: texte,
    dateCandidature: aujourdhui(),
  })
}
