import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import BoutonFavori from '../components/BoutonFavori.jsx'
import OffresPourMoi from '../components/OffresPourMoi.jsx'
import CarteOffre from '../components/CarteOffre.jsx'
import { creerCandidatureDepuisOffre } from '../offreSuivi.js'
import { ArrowDownUp, Briefcase, ExternalLink, Globe, Info, Layers, Search, Sparkles } from 'lucide-react'

const CONTRATS = [
  { valeur: '', label: 'Tous les contrats' },
  { valeur: 'ALTERNANCE', label: 'Alternance' },
  { valeur: 'STAGE', label: 'Stage' },
  { valeur: 'CDI', label: 'CDI' },
  { valeur: 'CDD', label: 'CDD' },
]

const RECHERCHE_DEFAUT = { q: '', lieu: '', contrat: '', source: '', pays: 'FR', domaine: '', tri: 'date' }
const CLE_FILTRES = 'postulyn_filtres_offres'

// Derniers filtres choisis, retrouves a la prochaine visite
function filtresMemorises() {
  try {
    const f = JSON.parse(localStorage.getItem(CLE_FILTRES) || 'null')
    return f ? { ...RECHERCHE_DEFAUT, ...f, source: '' } : RECHERCHE_DEFAUT
  } catch {
    return RECHERCHE_DEFAUT
  }
}

// Sites Indeed par pays (www.indeed.com pour les Etats-Unis, uk pour le Royaume-Uni)
const INDEED = { FR: 'fr', BE: 'be', CH: 'ch', LU: 'lu', CA: 'ca', DE: 'de', GB: 'uk', IE: 'ie', ES: 'es', PT: 'pt', IT: 'it', NL: 'nl', US: 'www', MA: 'ma' }

const DEPARTEMENT = /^(\d{2,3}|2[ab])$/i
const MOT_CONTRAT = { ALTERNANCE: 'alternance', STAGE: 'stage', CDI: 'CDI', CDD: 'CDD' }

// Welcome to the Jungle, Apec et HelloWork n'ont pas d'API publique : on ouvre leur site avec la meme recherche
function liensExternes({ q, lieu, contrat, pays }, nomPays, motDomaine) {
  const mot = MOT_CONTRAT[contrat]
  const base = q.trim() || motDomaine || ''
  const motsCles = mot && !base.toLowerCase().includes(mot.toLowerCase()) ? `${base} ${mot}`.trim() : base
  const linkedin = { nom: 'LinkedIn', url: `https://www.linkedin.com/jobs/search/?${new URLSearchParams({ keywords: motsCles, location: lieu.trim() || nomPays })}` }
  const indeed = INDEED[pays]
    ? [{ nom: 'Indeed', url: `https://${INDEED[pays]}.indeed.com/jobs?${new URLSearchParams({ q: motsCles, l: lieu.trim() })}` }]
    : []
  if (pays !== 'FR') return [linkedin, ...indeed]
  const hellowork = new URLSearchParams({ k: motsCles })
  if (lieu.trim()) hellowork.set('l', lieu.trim())
  const apec = new URLSearchParams({ motsCles })
  if (DEPARTEMENT.test(lieu.trim())) apec.set('lieux', lieu.trim().toUpperCase())
  const wttj = new URLSearchParams({ query: motsCles })
  if (lieu.trim() && !DEPARTEMENT.test(lieu.trim())) wttj.set('aroundQuery', lieu.trim())
  return [
    { nom: 'Welcome to the Jungle', url: `https://www.welcometothejungle.com/fr/jobs?${wttj}`, note: 'tech et startups' },
    { nom: 'HelloWork', url: `https://www.hellowork.com/fr-fr/emploi/recherche.html?${hellowork}` },
    { nom: 'Apec', url: `https://www.apec.fr/candidat/recherche-emploi.html/emploi?${apec}`, note: 'offres cadres' },
    linkedin,
    ...indeed,
  ]
}

export default function Offres() {
  const navigate = useNavigate()
  const [form, setForm] = useState(filtresMemorises)
  const [criteres, setCriteres] = useState({ pays: [{ code: 'FR', nom: 'France' }], domaines: [] })
  const [resultat, setResultat] = useState(null)
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  // lien de l'offre -> id de la candidature deja creee
  const [suivies, setSuivies] = useState({})
  const [mode, setMode] = useState(() => {
    try { return localStorage.getItem('postulyn_mode_offres') || 'pour-moi' } catch { return 'pour-moi' }
  })
  const changerMode = (m) => {
    setMode(m)
    try { localStorage.setItem('postulyn_mode_offres', m) } catch { /* stockage indisponible */ }
  }

  async function chercher(criteres, page) {
    setErreur('')
    setChargement(true)
    try {
      try { localStorage.setItem(CLE_FILTRES, JSON.stringify({ ...criteres, source: '' })) } catch { /* stockage indisponible */ }
      const res = await api.offres({ ...criteres, page })
      setResultat((prev) => (page > 0 && prev ? { ...res, offres: [...prev.offres, ...res.offres] } : res))
      if (!criteres.source) setForm((f) => ({ ...f, source: res.source }))
    } catch (e) {
      setErreur(e.message)
      if (page === 0) setResultat(null)
    } finally {
      setChargement(false)
    }
  }

  useEffect(() => {
    api.candidatures().then((liste) => {
      const map = {}
      liste.forEach((c) => { if (c.lienOffre) map[c.lienOffre] = c.id })
      setSuivies(map)
    }).catch(() => {})
    api.criteresOffres().then(setCriteres).catch(() => {})
    api.offres({ ...filtresMemorises(), page: 0 })
      .then((res) => {
        setResultat(res)
        setForm((f) => ({ ...f, source: res.source }))
      })
      .catch((e) => setErreur(e.message))
      .finally(() => setChargement(false))
  }, [])

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  // Changer de pays ou de domaine relance la recherche (la source n'est plus forcement disponible)
  const changerFiltre = (e) => {
    const f = { ...form, [e.target.name]: e.target.value, source: '' }
    setForm(f)
    chercher(f, 0)
  }
  // Le tri garde la source choisie
  const changerTri = (e) => {
    const f = { ...form, tri: e.target.value }
    setForm(f)
    chercher(f, 0)
  }
  const nomPays = criteres.pays.find((p) => p.code === form.pays)?.nom ?? form.pays
  const domaineChoisi = criteres.domaines.find((d) => d.code === form.domaine)
  const onSubmit = (e) => {
    e.preventDefault()
    chercher(form, 0)
  }
  const changerSource = (source) => {
    const f = { ...form, source }
    setForm(f)
    chercher(f, 0)
  }

  const ajouterAuSuivi = async (offre) => {
    const c = await creerCandidatureDepuisOffre(offre)
    setSuivies((x) => ({ ...x, [offre.url]: c.id }))
    return c
  }
  const ouvrirCandidature = (id, outil) => navigate(`/candidatures/${id}${outil ? `?outil=${outil}` : ''}`)

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Offres d'emploi</h1>
          <p className="muted">Tous les domaines, en France et à l'étranger : postule en un clic et suis l'offre dans Postulyn.</p>
        </div>
      </div>

      <div className="tabs modes-offres" role="tablist">
        <button type="button" role="tab" aria-selected={mode === 'pour-moi'} className={mode === 'pour-moi' ? 'active' : ''}
          onClick={() => changerMode('pour-moi')}><Sparkles size={16} /> Offres pour moi</button>
        <button type="button" role="tab" aria-selected={mode === 'libre'} className={mode === 'libre' ? 'active' : ''}
          onClick={() => changerMode('libre')}><Search size={16} /> Recherche libre</button>
      </div>

      {mode === 'pour-moi' && <OffresPourMoi pays={criteres.pays} suivies={suivies} setSuivies={setSuivies} />}

      {mode === 'libre' && (<>
      <form className="card recherche-offres" onSubmit={onSubmit}>
        <label className="champ-mots">
          Mots-clés
          <span className="champ-icone">
            <Search size={16} />
            <input name="q" value={form.q} onChange={onChange} placeholder="Métier, compétence, entreprise…" />
          </span>
        </label>
        <label>
          Lieu
          <input name="lieu" value={form.lieu} onChange={onChange}
            placeholder={form.pays === 'FR' ? 'Ville ou département, ex. Lyon ou 75' : 'Ville (facultatif)'} />
        </label>
        <button className="btn primary" disabled={chargement}>Rechercher</button>
        <div className="filtres-offres">
          <label>
            <span><Globe size={14} /> Pays</span>
            <select name="pays" value={form.pays} onChange={changerFiltre}>
              {criteres.pays.map((p) => <option key={p.code} value={p.code}>{p.nom}</option>)}
            </select>
          </label>
          <label>
            <span><Layers size={14} /> Domaine</span>
            <select name="domaine" value={form.domaine} onChange={changerFiltre}>
              <option value="">Tous les domaines</option>
              {criteres.domaines.map((d) => <option key={d.code} value={d.code}>{d.libelle}</option>)}
            </select>
          </label>
          <label>
            <span><ArrowDownUp size={14} /> Trier par</span>
            <select name="tri" value={form.tri} onChange={changerTri}>
              <option value="date">Les plus récentes</option>
              <option value="pertinence">Les plus pertinentes</option>
            </select>
          </label>
          <label>
            <span><Briefcase size={14} /> Contrat</span>
            <select name="contrat" value={form.contrat} onChange={changerFiltre}>
              {CONTRATS.map((c) => <option key={c.valeur} value={c.valeur}>{c.label}</option>)}
            </select>
          </label>
        </div>
      </form>

      {resultat?.sources?.length > 1 && (
        <div className="tabs sources">
          {resultat.sources.map((s) => (
            <button key={s} type="button" className={s === resultat.source ? 'active' : ''}
              onClick={() => changerSource(s)} disabled={chargement}>{s}</button>
          ))}
        </div>
      )}

      {erreur && <div className="card alerte"><strong>Recherche impossible</strong><p>{erreur}</p></div>}

      <div className="voir-aussi">
        <span className="muted small">Voir aussi :</span>
        {liensExternes(form, nomPays, domaineChoisi?.libelle.split(' ')[0]).map((l) => (
          <a key={l.nom} href={l.url} target="_blank" rel="noopener noreferrer" className="btn ghost small">
            {l.nom} <ExternalLink size={13} />{l.note && <span className="muted"> · {l.note}</span>}
          </a>
        ))}
        <span className="muted small">puis ajoute l'offre avec « + Nouvelle candidature ».</span>
      </div>

      {resultat && (
        <p className="muted small">
          {resultat.total.toLocaleString('fr-FR')} offre{resultat.total > 1 ? 's' : ''} · {nomPays} · source {resultat.source}
        </p>
      )}
      {resultat?.remarque && <p className="info-source"><Info size={15} /> {resultat.remarque}</p>}

      <div className="offres">
        {resultat?.offres.map((o) => (
          <CarteOffre key={o.id} offre={o} candidatureId={suivies[o.url]}
            onAjouter={ajouterAuSuivi} onOuvrir={ouvrirCandidature} />
        ))}
      </div>

      {resultat && resultat.offres.length === 0 && !chargement && (
        <div className="card vide"><p className="muted">Aucune offre pour cette recherche. Essaie d'autres mots-clés, un autre domaine ou une autre source.</p></div>
      )}
      {chargement && <p className="muted">Recherche en cours…</p>}
      {resultat?.pageSuivante && !chargement && (
        <div className="centre">
          <button className="btn ghost" onClick={() => chercher(form, resultat.page + 1)}>Voir plus d'offres</button>
        </div>
      )}

      </>)}

      <BoutonFavori />
    </>
  )
}
