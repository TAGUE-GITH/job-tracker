import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowDownUp, Briefcase, FileUp, Globe, MapPin, RefreshCw, Sparkles } from 'lucide-react'
import { api } from '../api'
import OrvynaAvatar from './OrvynaAvatar.jsx'
import ListeDetailOffres from './ListeDetailOffres.jsx'
import { creerCandidatureDepuisOffre } from '../offreSuivi.js'

const CONTRATS = [
  { valeur: '', label: 'Tous les contrats' },
  { valeur: 'ALTERNANCE', label: 'Alternance' },
  { valeur: 'STAGE', label: 'Stage' },
  { valeur: 'CDI', label: 'CDI' },
  { valeur: 'CDD', label: 'CDD' },
]

/**
 * "Offres pour moi" : ORVYNA lit le CV une fois, en tire un profil, puis l'application cherche les offres
 * correspondantes et les classe par correspondance avec le CV.
 */
export default function OffresPourMoi({ pays, suivies, setSuivies }) {
  const navigate = useNavigate()
  const [profil, setProfil] = useState(undefined)
  const [cvs, setCvs] = useState([])
  const [cvId, setCvId] = useState('')
  const [analyse, setAnalyse] = useState(false)
  const [filtres, setFiltres] = useState({ pays: 'FR', contrat: '', lieu: '', tri: 'pertinence' })
  const [resultat, setResultat] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api.profilCv().then((p) => setProfil(p ?? null)).catch(() => setProfil(null))
    api.documents().then((docs) => {
      const liste = docs.filter((d) => d.type === 'CV')
      setCvs(liste)
      setCvId(liste[0]?.id ?? '')
    }).catch(() => {})
  }, [])

  const chercher = async (f) => {
    setErreur('')
    setChargement(true)
    try {
      setResultat(await api.offresPourMoi(f))
    } catch (e) {
      setErreur(e.message)
      setResultat(null)
    } finally {
      setChargement(false)
    }
  }

  useEffect(() => {
    if (!profil) return
    // Premier chargement : le contrat le plus coherent avec le profil
    const f = { ...filtres, contrat: profil.contrats?.length === 1 ? profil.contrats[0] : filtres.contrat }
    setFiltres(f)
    chercher(f)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profil])

  const analyser = async (id) => {
    if (!id) return
    setErreur('')
    setAnalyse(true)
    try {
      setProfil(await api.analyserProfilCv(Number(id)))
    } catch (e) {
      setErreur(e.message)
    } finally {
      setAnalyse(false)
    }
  }

  const importer = async (e) => {
    const fichier = e.target.files?.[0]
    e.target.value = ''
    if (!fichier) return
    setErreur('')
    setAnalyse(true)
    try {
      const doc = await api.uploadDocument(fichier, 'CV')
      setCvs((l) => [doc, ...l])
      setCvId(doc.id)
      setProfil(await api.analyserProfilCv(doc.id))
    } catch (err) {
      setErreur(err.message)
    } finally {
      setAnalyse(false)
    }
  }

  const changer = (e) => {
    const f = { ...filtres, [e.target.name]: e.target.value }
    setFiltres(f)
    if (e.target.name !== 'lieu') chercher(f)
  }

  const ajouter = async (offre) => {
    const c = await creerCandidatureDepuisOffre(offre)
    setSuivies((x) => ({ ...x, [offre.url]: c.id }))
    return c
  }

  if (profil === undefined) return <p className="muted">Chargement…</p>

  if (!profil) {
    return (
      <div className="card pour-moi-intro">
        <OrvynaAvatar size={48} />
        <div>
          <h2>Les offres faites pour toi</h2>
          <p className="muted">
            ORVYNA lit ton CV, repère tes métiers, tes compétences et ton niveau, puis Postulyn cherche les offres
            qui te correspondent et les classe de la plus proche à la plus éloignée. Ensuite, un clic sur
            « Adapter mon CV » réécrit ton CV pour l'offre choisie.
          </p>
          {erreur && <p className="erreur">{erreur}</p>}
          <div className="pour-moi-actions">
            {cvs.length > 0 && (
              <>
                <select value={cvId} onChange={(e) => setCvId(e.target.value)} aria-label="CV à analyser">
                  {cvs.map((d) => <option key={d.id} value={d.id}>{d.nomFichier}</option>)}
                </select>
                <button className="btn primary" onClick={() => analyser(cvId)} disabled={analyse}>
                  <Sparkles size={16} /> {analyse ? 'ORVYNA lit ton CV…' : 'Analyser ce CV'}
                </button>
              </>
            )}
            <label className={`btn ${cvs.length ? 'ghost' : 'primary'} upload`}>
              <FileUp size={16} /> {analyse && !cvs.length ? 'ORVYNA lit ton CV…' : 'Importer mon CV (PDF ou Word)'}
              <input type="file" accept=".pdf,.doc,.docx" onChange={importer} disabled={analyse} />
            </label>
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className="card profil-cv">
        <div className="profil-cv-tete">
          <div>
            <span className="eyebrow-app">Ton profil d'après {profil.nomCv ?? 'ton CV'}</span>
            <h2>{profil.titre} {profil.niveau && <span className="badge bleu">{profil.niveau}</span>}</h2>
            {profil.resume && <p className="muted">{profil.resume}</p>}
          </div>
          <div className="row">
            <select value={cvId} onChange={(e) => setCvId(e.target.value)} aria-label="Autre CV">
              {cvs.map((d) => <option key={d.id} value={d.id}>{d.nomFichier}</option>)}
            </select>
            <button className="btn ghost" onClick={() => analyser(cvId)} disabled={analyse}>
              <RefreshCw size={15} /> {analyse ? 'Analyse…' : 'Réanalyser'}
            </button>
          </div>
        </div>
        <div className="chips">
          {(profil.competences ?? []).slice(0, 14).map((c) => <span key={c} className="chip">{c}</span>)}
        </div>
        <div className="filtres-offres filtres-pour-moi">
          <label>
            <span><Globe size={14} /> Pays</span>
            <select name="pays" value={filtres.pays} onChange={changer}>
              {pays.map((p) => <option key={p.code} value={p.code}>{p.nom}</option>)}
            </select>
          </label>
          <label>
            <span><Briefcase size={14} /> Contrat</span>
            <select name="contrat" value={filtres.contrat} onChange={changer}>
              {CONTRATS.map((c) => <option key={c.valeur} value={c.valeur}>{c.label}</option>)}
            </select>
          </label>
          <label>
            <span><MapPin size={14} /> Lieu</span>
            <input name="lieu" value={filtres.lieu} onChange={changer} placeholder="Ville (facultatif)"
              onKeyDown={(e) => { if (e.key === 'Enter') chercher(filtres) }} onBlur={() => chercher(filtres)} />
          </label>
          <label>
            <span><ArrowDownUp size={14} /> Trier par</span>
            <select name="tri" value={filtres.tri} onChange={changer}>
              <option value="pertinence">Meilleure correspondance</option>
              <option value="date">Les plus récentes</option>
            </select>
          </label>
        </div>
      </div>

      {erreur && <div className="card alerte"><strong>Recherche impossible</strong><p>{erreur}</p></div>}
      {chargement && <p className="muted">ORVYNA cherche les offres qui te correspondent…</p>}
      {resultat && !chargement && (
        <div className="interpretation">
          <span><strong>{resultat.offres.length} offre{resultat.offres.length > 1 ? 's' : ''}</strong> correspondant à ton profil, de la plus proche à la plus éloignée</span>
        </div>
      )}
      {!chargement && resultat && (
        <ListeDetailOffres items={resultat.offres} suivies={suivies} onAjouter={ajouter}
          onOuvrir={(id, outil) => navigate(`/candidatures/${id}${outil ? `?outil=${outil}` : ''}`)} />
      )}
      {resultat && resultat.offres.length === 0 && !chargement && (
        <div className="card vide">
          <p className="muted">Aucune offre assez proche de ton CV pour ces critères. Essaie un autre pays, « Tous les contrats » ou la recherche libre.</p>
        </div>
      )}
    </>
  )
}
