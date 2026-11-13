import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import DocumentActions from './DocumentActions.jsx'
import { Camera, Check, Plus, X } from 'lucide-react'

// Les listes se modifient dans une zone de texte, un element par ligne
const enLignes = (liste) => (liste ?? []).filter(Boolean).join('\n')
const enListe = (texte) => texte.split('\n').map((l) => l.trim()).filter(Boolean)

const LISTES = ['competencesTechniques', 'competencesTransverses', 'langues', 'atouts', 'centresInteret', 'liens']

/** Reponse de ORVYNA -> formulaire modifiable */
function versFormulaire(cv) {
  const f = {
    titre: cv.titre ?? '', profil: cv.profil ?? '', motsClesAjoutes: cv.motsClesAjoutes ?? [],
    competencesAjoutees: (cv.competencesAjoutees ?? []).filter(Boolean),
  }
  LISTES.forEach((cle) => { f[cle] = enLignes(cv[cle]) })
  f.experiences = (cv.experiences ?? []).map((x) => ({ ...x, missions: enLignes(x.missions) }))
  // Tous les blocs du CV importe sont gardes : rien n'est retire
  f.projets = (cv.projets ?? []).map((p) => ({ ...p, points: enLignes(p.points) }))
  f.formations = cv.formations ?? []
  f.autresRubriques = (cv.autresRubriques ?? []).map((r) => ({ titre: r.titre ?? '', elements: enLignes(r.elements) }))
  return f
}

/** Formulaire -> CV envoye au serveur pour le PDF */
function versCv(f) {
  const cv = { titre: f.titre, profil: f.profil }
  LISTES.forEach((cle) => { cv[cle] = enListe(f[cle]) })
  cv.experiences = f.experiences.map((x) => ({ ...x, missions: enListe(x.missions) }))
  cv.projets = f.projets.map((p) => ({ ...p, points: enListe(p.points) }))
  cv.formations = f.formations
  cv.autresRubriques = f.autresRubriques
    .map((r) => ({ titre: r.titre.trim(), elements: enListe(r.elements) }))
    .filter((r) => r.titre && r.elements.length)
  return cv
}

// Modeles de CV, en vrai texte lisible par les logiciels de tri (ATS)
const MODELES = [
  { code: 'CLASSIQUE', nom: 'Classique', detail: 'Nom centré, formation en tête, dates à droite. Sans photo.', photo: false },
  { code: 'MODERNE', nom: 'Chronologique', detail: 'Dates dans une colonne à gauche, photo carrée à droite.', photo: true },
  { code: 'ELEGANT', nom: 'Colonne beige', detail: 'Contact, profil et compétences dans une colonne beige.', photo: true },
  { code: 'COMPACT', nom: 'Épuré', detail: 'Bande grise, photo ronde, grands titres en gras.', photo: true },
]

/** Miniature du modele de CV (forme generale de la page). */
function ApercuModele({ code, photo }) {
  const lignes = (n, cle) => Array.from({ length: n }, (_, i) => <i key={`${cle}${i}`} />)
  if (code === 'ELEGANT') {
    return (
      <span className="apercu-modele apercu-elegant" aria-hidden="true">
        <span className="a-cote">
          {photo && <b className="a-photo rond" />}
          <b className="a-rubrique" />{lignes(3, 'c')}<b className="a-rubrique" />{lignes(4, 'p')}<b className="a-rubrique" />{lignes(3, 's')}
        </span>
        <span className="a-corps">
          <b className="a-nom" /><i className="a-titre" />
          <b className="a-rubrique" /><i className="a-ocre" />{lignes(4, 'e')}<b className="a-rubrique" /><i className="a-ocre" />{lignes(3, 'f')}
        </span>
      </span>
    )
  }
  if (code === 'MODERNE') {
    return (
      <span className="apercu-modele apercu-moderne" aria-hidden="true">
        {photo && <b className="a-photo" />}
        <b className="a-nom" /><i className="a-titre" />{lignes(2, 'c')}
        <b className="a-rubrique" />
        {[0, 1, 2].map((k) => <span key={k} className="a-deux"><i /><span>{lignes(2, `d${k}`)}</span></span>)}
        <b className="a-rubrique" />
        <span className="a-grille">{lignes(6, 'g')}</span>
      </span>
    )
  }
  if (code === 'COMPACT') {
    return (
      <span className="apercu-modele apercu-compact" aria-hidden="true">
        {photo && <b className="a-photo rond" />}
        <b className="a-nom" /><i className="a-titre" />{lignes(3, 'c')}
        <b className="a-rubrique" />{lignes(4, 'e')}<b className="a-rubrique" />{lignes(3, 'f')}
      </span>
    )
  }
  return (
    <span className="apercu-modele apercu-classique" aria-hidden="true">
      <b className="a-nom" /><i className="a-titre" />
      <b className="a-rubrique" />{[0, 1].map((k) => <span key={k} className="a-ligne"><i /><i /></span>)}
      <b className="a-rubrique" />{[0, 1, 2].map((k) => <span key={k} className="a-ligne"><i /><i /></span>)}{lignes(2, 'p')}
      <b className="a-rubrique" />{lignes(2, 'x')}
    </span>
  )
}

const VIDES = {
  experiences: { poste: '', entreprise: '', lieu: '', periode: '', missions: '' },
  projets: { nom: '', description: '', points: '', technologies: '' },
  formations: { diplome: '', etablissement: '', lieu: '', periode: '', details: '' },
  autresRubriques: { titre: '', elements: '' },
}

export default function CvAdapte({ candidature, cvId, onChange, Score }) {
  const [form, setForm] = useState(null)
  const [modele, setModele] = useState('CLASSIQUE')
  const [avecPhoto, setAvecPhoto] = useState(false)
  const [aUnePhoto, setAUnePhoto] = useState(false)

  useEffect(() => {
    api.profil().then((p) => setAUnePhoto(!!p.photo)).catch(() => {})
  }, [])
  const [chargement, setChargement] = useState(false)
  const [enregistrement, setEnregistrement] = useState(false)
  const [erreur, setErreur] = useState('')
  const [resultat, setResultat] = useState(null)

  const adapter = async () => {
    setErreur('')
    setResultat(null)
    setChargement(true)
    try {
      const { reponse } = await api.cvIa(candidature.id, { cvId: cvId || null })
      setForm(versFormulaire(JSON.parse(reponse)))
    } catch (e) {
      setErreur(e instanceof SyntaxError ? 'ORVYNA a renvoyé un CV illisible, réessaie.' : e.message)
    } finally {
      setChargement(false)
    }
  }

  const changer = (cle, valeur) => setForm((f) => ({ ...f, [cle]: valeur }))
  const changerElement = (liste, i, cle, valeur) =>
    setForm((f) => ({ ...f, [liste]: f[liste].map((x, j) => (j === i ? { ...x, [cle]: valeur } : x)) }))
  const ajouter = (liste) => setForm((f) => ({ ...f, [liste]: [...f[liste], { ...VIDES[liste] }] }))
  const retirer = (liste, i) => setForm((f) => ({ ...f, [liste]: f[liste].filter((_, j) => j !== i) }))

  // Retire une competence ajoutee par ORVYNA, de la liste des ajouts et des listes de competences
  const retirerCompetence = (competence) => setForm((f) => {
    const sans = (texte) => enListe(texte).filter((c) => c.toLowerCase() !== competence.toLowerCase()).join('\n')
    return {
      ...f,
      competencesAjoutees: f.competencesAjoutees.filter((c) => c !== competence),
      competencesTechniques: sans(f.competencesTechniques),
      competencesTransverses: sans(f.competencesTransverses),
    }
  })

  const enregistrer = async () => {
    setErreur('')
    setEnregistrement(true)
    try {
      const maj = await api.enregistrerCv(candidature.id, { ...versCv(form), modele, avecPhoto: avecPhoto && aUnePhoto })
      onChange(maj)
      setResultat({ doc: maj.cv, score: null })
      // Note ATS du nouveau CV : on le compare a l'offre
      const { reponse } = await api.compatibilite(candidature.id, { cvId: maj.cv?.id })
      setResultat({ doc: maj.cv, score: JSON.parse(reponse) })
    } catch (e) {
      setErreur(e instanceof SyntaxError ? 'CV enregistré, mais la note ATS est illisible : relance l’analyse.' : e.message)
    } finally {
      setEnregistrement(false)
    }
  }

  return (
    <div className="cv-adapte">
      {!form && !chargement && (
        <p className="muted small">
          ORVYNA part de ton CV importé et l'améliore pour cette offre, sans rien supprimer : tes expériences
          (postes, entreprises, dates) restent les mêmes, leurs missions sont reformulées et détaillées avec les mots
          de l'offre. Toutes tes formations, tes projets et tes autres rubriques sont gardés. Tes compétences et tes
          atouts restent, ORVYNA ajoute seulement ceux que l'offre demande et qui te correspondent. Tu peux tout
          modifier et ajouter des rubriques, puis choisir parmi 4 mises en forme lisibles par les logiciels de tri (ATS).
        </p>
      )}
      <button className="btn primary" onClick={adapter} disabled={chargement}>
        {chargement ? 'ORVYNA adapte ton CV…' : form ? 'Réadapter depuis mon CV' : 'Adapter mon CV à l’offre'}
      </button>
      {erreur && <p className="erreur">{erreur}</p>}

      {form && (
        <div className="cv-form">
          {form.motsClesAjoutes.length > 0 && (
            <div>
              <h4>Mots-clés de l'offre intégrés</h4>
              <div className="chips">{form.motsClesAjoutes.map((m) => <span key={m} className="chip">{m}</span>)}</div>
            </div>
          )}
          {form.competencesAjoutees.length > 0 && (
            <div className="competences-ajoutees">
              <h4>Compétences ajoutées d'après l'offre</h4>
              <p className="muted small">
                Proches de tes compétences actuelles. Garde celles que tu sauras expliquer en entretien, retire les autres.
              </p>
              <div className="chips">
                {form.competencesAjoutees.map((c) => (
                  <span key={c} className="chip chip-ajout">
                    {c}
                    <button type="button" onClick={() => retirerCompetence(c)} aria-label={`Retirer ${c}`}><X size={14} /></button>
                  </span>
                ))}
              </div>
            </div>
          )}
          <p className="muted small">
            Relis tout : c'est ton CV. Tes coordonnées viennent de <Link to="/compte">Mon compte</Link>.
          </p>

          <label>Titre du CV<input value={form.titre} onChange={(e) => changer('titre', e.target.value)} /></label>
          <label>Profil<textarea rows={4} value={form.profil} onChange={(e) => changer('profil', e.target.value)} /></label>
          <div className="grid-2">
            <Liste titre="Compétences techniques" valeur={form.competencesTechniques} onChange={(v) => changer('competencesTechniques', v)} />
            <Liste titre="Compétences transverses" valeur={form.competencesTransverses} onChange={(v) => changer('competencesTransverses', v)} />
          </div>

          <Groupe titre="Expériences professionnelles" onAjouter={() => ajouter('experiences')}>
            {form.experiences.map((x, i) => (
              <Bloc key={i} onRetirer={() => retirer('experiences', i)}>
                <div className="grid-2">
                  <Champ libelle="Poste" valeur={x.poste} onChange={(v) => changerElement('experiences', i, 'poste', v)} />
                  <Champ libelle="Entreprise" valeur={x.entreprise} onChange={(v) => changerElement('experiences', i, 'entreprise', v)} />
                  <Champ libelle="Lieu" valeur={x.lieu} onChange={(v) => changerElement('experiences', i, 'lieu', v)} />
                  <Champ libelle="Période" valeur={x.periode} onChange={(v) => changerElement('experiences', i, 'periode', v)} />
                </div>
                <Liste titre="Missions" valeur={x.missions} onChange={(v) => changerElement('experiences', i, 'missions', v)} />
              </Bloc>
            ))}
          </Groupe>

          <Groupe titre="Projets" onAjouter={() => ajouter('projets')}>
            {form.projets.map((p, i) => (
              <Bloc key={i} onRetirer={() => retirer('projets', i)}>
                <div className="grid-2">
                  <Champ libelle="Nom" valeur={p.nom} onChange={(v) => changerElement('projets', i, 'nom', v)} />
                  <Champ libelle="Technologies" valeur={p.technologies} onChange={(v) => changerElement('projets', i, 'technologies', v)} />
                </div>
                <label>Contexte<textarea rows={2} value={p.description ?? ''} onChange={(e) => changerElement('projets', i, 'description', e.target.value)} /></label>
                <Liste titre="Réalisations détaillées" valeur={p.points ?? ''} onChange={(v) => changerElement('projets', i, 'points', v)} />
              </Bloc>
            ))}
          </Groupe>

          <Groupe titre="Formation" onAjouter={() => ajouter('formations')}>
            {form.formations.map((f, i) => (
              <Bloc key={i} onRetirer={() => retirer('formations', i)}>
                <div className="grid-2">
                  <Champ libelle="Diplôme" valeur={f.diplome} onChange={(v) => changerElement('formations', i, 'diplome', v)} />
                  <Champ libelle="Établissement" valeur={f.etablissement} onChange={(v) => changerElement('formations', i, 'etablissement', v)} />
                  <Champ libelle="Ville" valeur={f.lieu} onChange={(v) => changerElement('formations', i, 'lieu', v)} />
                  <Champ libelle="Période" valeur={f.periode} onChange={(v) => changerElement('formations', i, 'periode', v)} />
                  <Champ libelle="Détails" valeur={f.details} onChange={(v) => changerElement('formations', i, 'details', v)} />
                </div>
              </Bloc>
            ))}
          </Groupe>

          <Groupe titre="Autres rubriques (certifications, bénévolat, activités…)" onAjouter={() => ajouter('autresRubriques')}>
            {form.autresRubriques.length === 0 && (
              <p className="muted small">Ajoute une rubrique si tu veux mettre en avant autre chose : certifications, bénévolat, associations, prix…</p>
            )}
            {form.autresRubriques.map((r, i) => (
              <Bloc key={i} onRetirer={() => retirer('autresRubriques', i)}>
                <Champ libelle="Titre de la rubrique" valeur={r.titre} onChange={(v) => changerElement('autresRubriques', i, 'titre', v)} />
                <Liste titre="Éléments" valeur={r.elements ?? ''} onChange={(v) => changerElement('autresRubriques', i, 'elements', v)} />
              </Bloc>
            ))}
          </Groupe>

          <div className="grid-2">
            <Liste titre="Langues" valeur={form.langues} onChange={(v) => changer('langues', v)} />
            <Liste titre="Atouts" valeur={form.atouts} onChange={(v) => changer('atouts', v)} />
            <Liste titre="Centres d'intérêt" valeur={form.centresInteret} onChange={(v) => changer('centresInteret', v)} />
            <Liste titre="Liens (GitHub, LinkedIn, portfolio)" valeur={form.liens} onChange={(v) => changer('liens', v)} />
          </div>

          <fieldset className="cv-groupe choix-modele">
            <legend>Mise en forme</legend>
            <div className="modeles-cv" role="radiogroup" aria-label="Modèle de CV">
              {MODELES.map((m) => (
                <button key={m.code} type="button" role="radio" aria-checked={modele === m.code}
                  className={`modele-cv ${modele === m.code ? 'choisi' : ''}`} onClick={() => setModele(m.code)}>
                  <ApercuModele code={m.code} photo={m.photo && avecPhoto && aUnePhoto} />
                  <strong>
                    {modele === m.code && <Check size={14} />} {m.nom}
                  </strong>
                  <small>{m.detail}</small>
                </button>
              ))}
            </div>
            <label className="checkbox case-photo">
              <input type="checkbox" checked={avecPhoto && aUnePhoto} disabled={!aUnePhoto}
                onChange={(e) => setAvecPhoto(e.target.checked)} />
              <span><Camera size={15} /> Ajouter ma photo</span>
            </label>
            <p className="muted small">
              {aUnePhoto
                ? (modele === 'CLASSIQUE'
                  ? 'Le modèle Classique se présente sans photo, comme les CV internationaux : choisis un autre modèle pour l’afficher.'
                  : 'La photo est facultative : en France elle est courante, mais de nombreux recruteurs à l’international préfèrent un CV sans photo.')
                : <>Pour ajouter une photo, enregistre-la d'abord dans <Link to="/compte">Paramètres du compte</Link>.</>}
            </p>
          </fieldset>

          <button className="btn primary" onClick={enregistrer} disabled={enregistrement}>
            {enregistrement ? 'Création du PDF et calcul de la note ATS…' : 'Enregistrer le CV en PDF (compatible ATS)'}
          </button>
        </div>
      )}

      {resultat && (
        <div className="cv-resultat">
          <p className="succes">« {resultat.doc?.nomFichier} » est enregistré et rattaché à cette candidature.</p>
          <DocumentActions doc={resultat.doc} libelle="Ouvrir mon CV" />
          {resultat.score && (
            <div className="score-ligne">
              <Score valeur={Number(resultat.score.score) || 0} />
              <p><strong>Note ATS estimée du nouveau CV.</strong> {resultat.score.resume}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function Liste({ titre, valeur, onChange }) {
  return (
    <label>
      {titre} <span className="muted small">(une par ligne)</span>
      <textarea rows={Math.min(8, Math.max(3, valeur.split('\n').length + 1))} value={valeur} onChange={(e) => onChange(e.target.value)} />
    </label>
  )
}

function Champ({ libelle, valeur, onChange }) {
  return <label>{libelle}<input value={valeur ?? ''} onChange={(e) => onChange(e.target.value)} /></label>
}

function Groupe({ titre, onAjouter, children }) {
  return (
    <fieldset className="cv-groupe">
      <legend>{titre}</legend>
      {children}
      <button type="button" className="btn ghost small" onClick={onAjouter}><Plus size={14} /> Ajouter</button>
    </fieldset>
  )
}

function Bloc({ onRetirer, children }) {
  return (
    <div className="cv-bloc">
      <button type="button" className="icon" title="Retirer" aria-label="Retirer" onClick={onRetirer}><X size={16} /></button>
      {children}
    </div>
  )
}
