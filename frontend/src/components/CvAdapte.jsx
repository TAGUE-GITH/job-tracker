import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

// Les listes se modifient dans une zone de texte, un element par ligne
const enLignes = (liste) => (liste ?? []).filter(Boolean).join('\n')
const enListe = (texte) => texte.split('\n').map((l) => l.trim()).filter(Boolean)

const LISTES = ['competencesTechniques', 'competencesTransverses', 'langues', 'atouts', 'centresInteret', 'liens']

/** Reponse de TAGIA -> formulaire modifiable */
function versFormulaire(cv) {
  const f = { titre: cv.titre ?? '', profil: cv.profil ?? '', motsClesAjoutes: cv.motsClesAjoutes ?? [] }
  LISTES.forEach((cle) => { f[cle] = enLignes(cv[cle]) })
  f.experiences = (cv.experiences ?? []).map((x) => ({ ...x, missions: enLignes(x.missions) }))
  f.projets = cv.projets ?? []
  f.formations = cv.formations ?? []
  return f
}

/** Formulaire -> CV envoye au serveur pour le PDF */
function versCv(f) {
  const cv = { titre: f.titre, profil: f.profil }
  LISTES.forEach((cle) => { cv[cle] = enListe(f[cle]) })
  cv.experiences = f.experiences.map((x) => ({ ...x, missions: enListe(x.missions) }))
  cv.projets = f.projets
  cv.formations = f.formations
  return cv
}

const VIDES = {
  experiences: { poste: '', entreprise: '', lieu: '', periode: '', missions: '' },
  projets: { nom: '', description: '', technologies: '' },
  formations: { diplome: '', etablissement: '', periode: '', details: '' },
}

export default function CvAdapte({ candidature, cvId, onChange, Score }) {
  const [form, setForm] = useState(null)
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
      setErreur(e instanceof SyntaxError ? 'TAGIA a renvoyé un CV illisible, réessaie.' : e.message)
    } finally {
      setChargement(false)
    }
  }

  const changer = (cle, valeur) => setForm((f) => ({ ...f, [cle]: valeur }))
  const changerElement = (liste, i, cle, valeur) =>
    setForm((f) => ({ ...f, [liste]: f[liste].map((x, j) => (j === i ? { ...x, [cle]: valeur } : x)) }))
  const ajouter = (liste) => setForm((f) => ({ ...f, [liste]: [...f[liste], { ...VIDES[liste] }] }))
  const retirer = (liste, i) => setForm((f) => ({ ...f, [liste]: f[liste].filter((_, j) => j !== i) }))

  const enregistrer = async () => {
    setErreur('')
    setEnregistrement(true)
    try {
      const maj = await api.enregistrerCv(candidature.id, versCv(form))
      onChange(maj)
      setResultat({ nom: maj.cv?.nomFichier, score: null })
      // Note ATS du nouveau CV : on le compare a l'offre
      const { reponse } = await api.compatibilite(candidature.id, { cvId: maj.cv?.id })
      setResultat({ nom: maj.cv?.nomFichier, score: JSON.parse(reponse) })
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
          TAGIA réécrit ton CV pour cette offre : titre, profil, compétences dans l'ordre de l'offre, missions
          reformulées avec ses mots-clés, atouts et centres d'intérêt. Il n'invente rien : tout vient de ton CV.
          Le PDF est pensé pour les logiciels de tri (ATS) : une colonne, du vrai texte, des rubriques classiques.
        </p>
      )}
      <button className="btn primary" onClick={adapter} disabled={chargement}>
        {chargement ? 'TAGIA adapte ton CV…' : form ? 'Réadapter depuis mon CV' : '✨ Adapter mon CV à l’offre'}
      </button>
      {erreur && <p className="erreur">{erreur}</p>}

      {form && (
        <div className="cv-form">
          {form.motsClesAjoutes.length > 0 && (
            <div>
              <h4>🔑 Mots-clés de l'offre intégrés</h4>
              <div className="chips">{form.motsClesAjoutes.map((m) => <span key={m} className="chip">{m}</span>)}</div>
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

          <Groupe titre="Expériences" onAjouter={() => ajouter('experiences')}>
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
                <label>Description<textarea rows={2} value={p.description ?? ''} onChange={(e) => changerElement('projets', i, 'description', e.target.value)} /></label>
              </Bloc>
            ))}
          </Groupe>

          <Groupe titre="Formation" onAjouter={() => ajouter('formations')}>
            {form.formations.map((f, i) => (
              <Bloc key={i} onRetirer={() => retirer('formations', i)}>
                <div className="grid-2">
                  <Champ libelle="Diplôme" valeur={f.diplome} onChange={(v) => changerElement('formations', i, 'diplome', v)} />
                  <Champ libelle="Établissement" valeur={f.etablissement} onChange={(v) => changerElement('formations', i, 'etablissement', v)} />
                  <Champ libelle="Période" valeur={f.periode} onChange={(v) => changerElement('formations', i, 'periode', v)} />
                  <Champ libelle="Détails" valeur={f.details} onChange={(v) => changerElement('formations', i, 'details', v)} />
                </div>
              </Bloc>
            ))}
          </Groupe>

          <div className="grid-2">
            <Liste titre="Langues" valeur={form.langues} onChange={(v) => changer('langues', v)} />
            <Liste titre="Atouts" valeur={form.atouts} onChange={(v) => changer('atouts', v)} />
            <Liste titre="Centres d'intérêt" valeur={form.centresInteret} onChange={(v) => changer('centresInteret', v)} />
            <Liste titre="Liens (GitHub, LinkedIn, portfolio)" valeur={form.liens} onChange={(v) => changer('liens', v)} />
          </div>

          <button className="btn primary" onClick={enregistrer} disabled={enregistrement}>
            {enregistrement ? 'Création du PDF et calcul de la note ATS…' : '📄 Enregistrer le CV en PDF (compatible ATS)'}
          </button>
        </div>
      )}

      {resultat && (
        <div className="cv-resultat">
          <p className="succes">« {resultat.nom} » est enregistré et rattaché à cette candidature ✓</p>
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
      <button type="button" className="btn ghost small" onClick={onAjouter}>+ Ajouter</button>
    </fieldset>
  )
}

function Bloc({ onRetirer, children }) {
  return (
    <div className="cv-bloc">
      <button type="button" className="icon" title="Retirer" onClick={onRetirer}>×</button>
      {children}
    </div>
  )
}
