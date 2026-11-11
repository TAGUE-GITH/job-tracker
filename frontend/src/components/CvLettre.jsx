import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import OrvynaAvatar from './OrvynaAvatar.jsx'
import CvAdapte from './CvAdapte.jsx'
import DocumentActions from './DocumentActions.jsx'
import { FileText, Info, Mail, MessageSquare, Target } from 'lucide-react'
import MessageRecruteur from './MessageRecruteur.jsx'

// ORVYNA compare le CV a l'offre (score, competences manquantes) et redige la lettre de motivation
export default function CvLettre({ candidature, onChange, ongletInitial = 'analyse' }) {
  const [onglet, setOnglet] = useState(ongletInitial)
  const [cvs, setCvs] = useState(null)
  const [cvId, setCvId] = useState(candidature.cv?.id ?? '')

  useEffect(() => {
    api.documents()
      .then((docs) => {
        const liste = docs.filter((d) => d.type === 'CV')
        setCvs(liste)
        setCvId((actuel) => actuel || liste[0]?.id || '')
      })
      .catch(() => setCvs([]))
  }, [])

  const manque = !candidature.texteOffre
    ? <>Ajoute le texte de l'offre (bouton <Link to={`/candidatures/${candidature.id}/modifier`}>Modifier</Link>) pour que ORVYNA puisse la comparer à ton CV.</>
    : cvs && cvs.length === 0
      ? <>Importe ton CV (PDF ou Word) dans <Link to="/documents">CV & lettres</Link> pour l'analyser.</>
      : null

  return (
    <div className="card cv-lettre">
      <div className="ia-head">
        <OrvynaAvatar size={44} />
        <div>
          <h3>ORVYNA · CV, lettre et message</h3>
          <p className="muted small">Ton CV face à cette offre, un CV adapté, une lettre et le message pour le recruteur.</p>
        </div>
      </div>

      <div className="tabs">
        <button type="button" className={onglet === 'analyse' ? 'active' : ''} onClick={() => setOnglet('analyse')}>
          <Target size={16} /> Compatibilité
        </button>
        <button type="button" className={onglet === 'lettre' ? 'active' : ''} onClick={() => setOnglet('lettre')}>
          <Mail size={16} /> Lettre
        </button>
        <button type="button" className={onglet === 'cv' ? 'active' : ''} onClick={() => setOnglet('cv')}>
          <FileText size={16} /> CV adapté
        </button>
        <button type="button" className={onglet === 'message' ? 'active' : ''} onClick={() => setOnglet('message')}>
          <MessageSquare size={16} /> Message
        </button>
      </div>

      {manque ? (
        <p className="muted small">{manque}</p>
      ) : (
        <>
          {cvs && cvs.length > 1 && (
            <label>
              CV utilisé
              <select value={cvId} onChange={(e) => setCvId(Number(e.target.value))}>
                {cvs.map((d) => <option key={d.id} value={d.id}>{d.nomFichier}</option>)}
              </select>
            </label>
          )}
          {onglet === 'analyse' && <Analyse candidatureId={candidature.id} cvId={cvId} />}
          {onglet === 'lettre' && <Lettre candidature={candidature} cvId={cvId} onChange={onChange} />}
          {onglet === 'cv' && <CvAdapte candidature={candidature} cvId={cvId} onChange={onChange} Score={Score} />}
          {onglet === 'message' && <MessageRecruteur candidature={candidature} cvId={cvId} />}
        </>
      )}
    </div>
  )
}

function Analyse({ candidatureId, cvId }) {
  const [analyse, setAnalyse] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')

  const analyser = async () => {
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.compatibilite(candidatureId, { cvId: cvId || null })
      setAnalyse(JSON.parse(reponse))
    } catch (e) {
      setErreur(e instanceof SyntaxError ? "ORVYNA a renvoyé une analyse illisible, réessaie." : e.message)
    } finally {
      setChargement(false)
    }
  }

  return (
    <div className="analyse">
      {!analyse && !chargement && (
        <p className="muted small">
          ORVYNA lit ton CV et l'offre, puis donne un score d'adéquation, ce qui te manque et les mots-clés à ajouter
          pour passer les filtres des recruteurs.
        </p>
      )}
      <button className="btn primary" onClick={analyser} disabled={chargement}>
        {chargement ? 'ORVYNA analyse ton CV…' : analyse ? 'Relancer l’analyse' : 'Analyser mon CV'}
      </button>
      {erreur && <p className="erreur">{erreur}</p>}

      {analyse && (
        <div className="analyse-resultat">
          <div className="score-ligne">
            <Score valeur={Number(analyse.score) || 0} />
            <p>{analyse.resume}</p>
          </div>
          <Liste titre="Points forts" items={analyse.pointsForts} classe="ok" />
          <Liste titre="Points à renforcer" items={analyse.competencesManquantes} classe="manque" />
          {analyse.motsCles?.length > 0 && (
            <div>
              <h4>Mots-clés à ajouter à ton CV <span className="muted small">(s'ils sont vrais pour toi)</span></h4>
              <div className="chips">{analyse.motsCles.map((m) => <span key={m} className="chip">{m}</span>)}</div>
            </div>
          )}
          <Liste titre="Recommandations pour ton CV" items={analyse.conseils} />
        </div>
      )}
    </div>
  )
}

function Score({ valeur }) {
  const v = Math.max(0, Math.min(100, Math.round(valeur)))
  const couleur = v >= 75 ? '#16a34a' : v >= 50 ? '#f59e0b' : '#dc2626'
  const rayon = 34
  const perimetre = 2 * Math.PI * rayon
  return (
    <div className="score" aria-label={`Score de compatibilité : ${v} sur 100`}>
      <svg width="88" height="88" viewBox="0 0 88 88">
        <circle cx="44" cy="44" r={rayon} fill="none" stroke="var(--border)" strokeWidth="8" />
        <circle cx="44" cy="44" r={rayon} fill="none" stroke={couleur} strokeWidth="8" strokeLinecap="round"
          strokeDasharray={perimetre} strokeDashoffset={perimetre * (1 - v / 100)} transform="rotate(-90 44 44)"
          className="score-arc" />
      </svg>
      <span style={{ color: couleur }}>{v}<small>/100</small></span>
    </div>
  )
}

function Liste({ titre, items, classe = '' }) {
  if (!items?.length) return null
  return (
    <div>
      <h4>{titre}</h4>
      <ul className={`liste-analyse ${classe}`}>{items.map((t) => <li key={t}>{t}</li>)}</ul>
    </div>
  )
}

const dateLongue = () => new Date().toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })

/** En-tete propose par defaut : coordonnees du profil, entreprise, lieu et date du jour. */
function enTeteParDefaut(profil, candidature) {
  const expediteur = profil
    ? [profil.nom, profil.adresse, profil.telephone, profil.email].filter((l) => l && l.trim()).join('\n')
    : ''
  // "Lieu : Nantes" est ajoute au texte de l'offre par le favori et la page Offres
  const lieu = candidature.texteOffre?.match(/^Lieu : (.+)$/m)?.[1]
  const destinataire = [candidature.nomEntreprise, 'Service Recrutement', lieu].filter(Boolean).join('\n')
  const lieuDate = profil?.ville ? `À ${profil.ville}, le ${dateLongue()}` : `Le ${dateLongue()}`
  return { expediteur, destinataire, lieuDate }
}

function Lettre({ candidature, cvId, onChange }) {
  const [profil, setProfil] = useState(null)
  const [entete, setEntete] = useState(() => enTeteParDefaut(null, candidature))
  const [consignes, setConsignes] = useState('')
  const [texte, setTexte] = useState('')
  const [chargement, setChargement] = useState(false)
  const [enregistrement, setEnregistrement] = useState(false)
  const [message, setMessage] = useState('')
  const [lettrePdf, setLettrePdf] = useState(null)
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api.profil()
      .then((p) => {
        setProfil(p)
        setEntete(enTeteParDefaut(p, candidature))
      })
      .catch(() => {})
    // Uniquement a l'ouverture d'une candidature : l'enregistrement ne doit pas effacer l'en-tete modifie
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [candidature.id])

  const profilIncomplet = profil && (!profil.telephone || !profil.adresse || !profil.ville)
  const changerEntete = (e) => setEntete({ ...entete, [e.target.name]: e.target.value })

  const rediger = async () => {
    setErreur('')
    setMessage('')
    setChargement(true)
    try {
      const { reponse } = await api.lettreIa(candidature.id, { cvId: cvId || null, consignes })
      setTexte(reponse)
    } catch (e) {
      setErreur(e.message)
    } finally {
      setChargement(false)
    }
  }

  const copier = async () => {
    const complet = [entete.expediteur, entete.destinataire, entete.lieuDate, texte]
      .map((b) => b.trim()).filter(Boolean).join('\n\n')
    try {
      await navigator.clipboard.writeText(complet)
      setMessage('Lettre copiée avec l’en-tête.')
    } catch {
      setMessage('Copie impossible : sélectionne le texte puis Ctrl + C')
    }
  }

  const enregistrer = async () => {
    setErreur('')
    setEnregistrement(true)
    try {
      const maj = await api.enregistrerLettre(candidature.id, { texte, ...entete })
      onChange(maj)
      setLettrePdf(maj.lettre)
      setMessage('Lettre enregistrée en PDF et rattachée à la candidature.')
    } catch (e) {
      setErreur(e.message)
    } finally {
      setEnregistrement(false)
    }
  }

  return (
    <div className="lettre">
      <label>
        Consignes pour ORVYNA <span className="muted small">(facultatif)</span>
        <input value={consignes} onChange={(e) => setConsignes(e.target.value)} maxLength={1000}
          placeholder="ex. insiste sur mon projet Postulyn, ton plus dynamique, disponible en septembre…" />
      </label>
      <button className="btn primary" onClick={rediger} disabled={chargement}>
        {chargement ? 'ORVYNA rédige ta lettre…' : texte ? 'Réécrire la lettre' : 'Rédiger ma lettre'}
      </button>
      {erreur && <p className="erreur">{erreur}</p>}

      {texte && (
        <>
          <p className="muted small">Relis et corrige la lettre avant de l'envoyer : c'est toi qui signes.</p>
          {profilIncomplet && (
            <p className="import-info">
              <Info size={16} /> Complète ton téléphone, ton adresse et ta ville dans <Link to="/compte">Mon compte</Link> pour
              qu'ils s'ajoutent automatiquement en haut de tes lettres.
            </p>
          )}
          <div className="feuille">
            <div className="feuille-entete">
              <label className="bloc-expediteur">
                <span className="muted small">Tes coordonnées</span>
                <textarea name="expediteur" value={entete.expediteur} onChange={changerEntete} rows={5} maxLength={600}
                  placeholder={'Prénom Nom\nAdresse\nCode postal Ville\nTéléphone\nE-mail'} />
              </label>
              <div className="bloc-droite">
                <label>
                  <span className="muted small">Destinataire</span>
                  <textarea name="destinataire" value={entete.destinataire} onChange={changerEntete} rows={4} maxLength={600}
                    placeholder={'Entreprise\nService Recrutement\nAdresse'} />
                </label>
                <label>
                  <span className="muted small">Lieu et date</span>
                  <input name="lieuDate" value={entete.lieuDate} onChange={changerEntete} maxLength={200} />
                </label>
              </div>
            </div>
            <textarea className="lettre-texte" value={texte} onChange={(e) => setTexte(e.target.value)} rows={16}
              aria-label="Corps de la lettre" />
          </div>
          <div className="actions">
            <button className="btn ghost" onClick={copier}>Copier</button>
            <button className="btn primary" onClick={enregistrer} disabled={enregistrement || !texte.trim()}>
              {enregistrement ? 'Enregistrement…' : 'Enregistrer en PDF'}
            </button>
          </div>
        </>
      )}
      {message && <p className="succes">{message}</p>}
      {lettrePdf && <DocumentActions doc={lettrePdf} libelle="Ouvrir ma lettre" />}
    </div>
  )
}
