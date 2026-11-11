import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { aujourdhui, formatDate } from '../labels'
import BoutonFavori from '../components/BoutonFavori.jsx'
import { ExternalLink, Plus } from 'lucide-react'

const CONTRATS = [
  { valeur: '', label: 'Tous les contrats' },
  { valeur: 'ALTERNANCE', label: 'Alternance' },
  { valeur: 'STAGE', label: 'Stage' },
  { valeur: 'CDI', label: 'CDI' },
  { valeur: 'CDD', label: 'CDD' },
]

const RECHERCHE_DEFAUT = { q: 'développeur java', lieu: '', contrat: 'ALTERNANCE', source: '' }

const DEPARTEMENT = /^(\d{2,3}|2[ab])$/i
const MOT_CONTRAT = { ALTERNANCE: 'alternance', STAGE: 'stage', CDI: 'CDI', CDD: 'CDD' }

// Welcome to the Jungle, Apec et HelloWork n'ont pas d'API publique : on ouvre leur site avec la meme recherche
function liensExternes({ q, lieu, contrat }) {
  const mot = MOT_CONTRAT[contrat]
  const motsCles = mot && !q.toLowerCase().includes(mot.toLowerCase()) ? `${q} ${mot}`.trim() : q.trim()
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
  ]
}

export default function Offres() {
  const navigate = useNavigate()
  const [form, setForm] = useState(RECHERCHE_DEFAUT)
  const [resultat, setResultat] = useState(null)
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  // lien de l'offre -> id de la candidature deja creee
  const [suivies, setSuivies] = useState({})

  async function chercher(criteres, page) {
    setErreur('')
    setChargement(true)
    try {
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
    api.offres({ ...RECHERCHE_DEFAUT, page: 0 })
      .then((res) => {
        setResultat(res)
        setForm((f) => ({ ...f, source: res.source }))
      })
      .catch((e) => setErreur(e.message))
      .finally(() => setChargement(false))
  }, [])

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
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
    const texte = [
      offre.description,
      '',
      `Source : ${offre.source}${offre.lieu ? ` · ${offre.lieu}` : ''}${offre.contrat ? ` · ${offre.contrat}` : ''}`,
      offre.salaire ? `Salaire : ${offre.salaire}` : null,
    ].filter((l) => l !== null).join('\n')
    const c = await api.creerCandidature({
      nomEntreprise: offre.entreprise || 'Entreprise non communiquée',
      poste: offre.titre,
      lienOffre: offre.url?.slice(0, 1000),
      texteOffre: texte,
      dateCandidature: aujourdhui(),
    })
    setSuivies((s) => ({ ...s, [offre.url]: c.id }))
    return c
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Offres d'emploi</h1>
          <p className="muted">Trouve une offre, postule en un clic et suis-la dans Postilyn.</p>
        </div>
      </div>

      <form className="card recherche-offres" onSubmit={onSubmit}>
        <label>
          Mots-clés
          <input name="q" value={form.q} onChange={onChange} placeholder="développeur java, data, réseau… ou code ROME" />
        </label>
        <label>
          Lieu
          <input name="lieu" value={form.lieu} onChange={onChange}
            placeholder="Ville ou département, ex. Lyon ou 75" />
        </label>
        <label>
          Contrat
          <select name="contrat" value={form.contrat} onChange={onChange}>
            {CONTRATS.map((c) => <option key={c.valeur} value={c.valeur}>{c.label}</option>)}
          </select>
        </label>
        <button className="btn primary" disabled={chargement}>Rechercher</button>
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
        {liensExternes(form).map((l) => (
          <a key={l.nom} href={l.url} target="_blank" rel="noopener noreferrer" className="btn ghost small">
            {l.nom} <ExternalLink size={13} />{l.note && <span className="muted"> · {l.note}</span>}
          </a>
        ))}
        <span className="muted small">puis ajoute l'offre avec « + Nouvelle candidature ».</span>
      </div>

      {resultat && (
        <p className="muted small">
          {resultat.total.toLocaleString('fr-FR')} offre{resultat.total > 1 ? 's' : ''} · source {resultat.source}
        </p>
      )}

      <div className="offres">
        {resultat?.offres.map((o) => (
          <CarteOffre key={o.id} offre={o} candidatureId={suivies[o.url]}
            onAjouter={ajouterAuSuivi} onOuvrir={(id) => navigate(`/candidatures/${id}`)} />
        ))}
      </div>

      {resultat && resultat.offres.length === 0 && !chargement && (
        <div className="card vide"><p className="muted">Aucune offre pour cette recherche. Essaie d'autres mots-clés.</p></div>
      )}
      {chargement && <p className="muted">Recherche en cours…</p>}
      {resultat?.pageSuivante && !chargement && (
        <div className="centre">
          <button className="btn ghost" onClick={() => chercher(form, resultat.page + 1)}>Voir plus d'offres</button>
        </div>
      )}

      <BoutonFavori />
    </>
  )
}

function CarteOffre({ offre, candidatureId, onAjouter, onOuvrir }) {
  const [ouverte, setOuverte] = useState(false)
  const [aPostule, setAPostule] = useState(false)
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState('')

  const ajouter = async () => {
    setEnvoi(true)
    setErreur('')
    try {
      const c = await onAjouter(offre)
      onOuvrir(c.id)
    } catch (e) {
      setErreur(e.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <article className="card offre">
      <div className="offre-head">
        <div>
          <h3>{offre.titre}</h3>
          <p className="muted">
            {offre.entreprise || 'Entreprise non communiquée'}{offre.lieu && ` · ${offre.lieu}`}
          </p>
        </div>
        {offre.datePublication && <span className="muted small nowrap">{formatDate(offre.datePublication)}</span>}
      </div>

      <div className="offre-tags">
        {offre.contrat && <span className="badge bleu">{offre.contrat}</span>}
        {offre.salaire && <span className="badge vert">{offre.salaire}</span>}
        {candidatureId && <span className="badge orange">Dans ton suivi</span>}
      </div>

      {offre.description && (
        <>
          <p className={`offre-desc ${ouverte ? 'ouverte' : ''}`}>{offre.description}</p>
          <button type="button" className="link small" onClick={() => setOuverte(!ouverte)}>
            {ouverte ? 'Réduire' : 'Lire la suite'}
          </button>
        </>
      )}

      {aPostule && !candidatureId && (
        <div className="offre-confirm">
          <span>Tu as postulé ? Ajoute l'offre à ton suivi pour ne pas la perdre.</span>
          <button className="btn primary small" onClick={ajouter} disabled={envoi}>
            {envoi ? 'Ajout…' : 'Oui, ajouter au suivi'}
          </button>
        </div>
      )}
      {erreur && <p className="erreur">{erreur}</p>}

      <div className="offre-actions">
        {candidatureId ? (
          <Link to={`/candidatures/${candidatureId}`} className="btn ghost">Voir dans mon suivi</Link>
        ) : (
          <button className="btn ghost" onClick={ajouter} disabled={envoi}><Plus size={16} /> Ajouter au suivi</button>
        )}
        {offre.url && (
          <a href={offre.url} target="_blank" rel="noopener noreferrer" className="btn primary"
            onClick={() => setAPostule(true)}>
            Postuler <ExternalLink size={15} />
          </a>
        )}
      </div>
    </article>
  )
}
