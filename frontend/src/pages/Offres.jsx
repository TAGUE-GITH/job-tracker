import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Globe, MapPin, Search, SearchX, Sparkles } from 'lucide-react'
import { api } from '../api'
import BoutonFavori from '../components/BoutonFavori.jsx'
import OffresPourMoi from '../components/OffresPourMoi.jsx'
import ListeDetailOffres from '../components/ListeDetailOffres.jsx'
import { creerCandidatureDepuisOffre } from '../offreSuivi.js'

const CONTRATS = [
  { valeur: '', label: 'Tous' },
  { valeur: 'CDI', label: 'CDI' },
  { valeur: 'CDD', label: 'CDD' },
  { valeur: 'ALTERNANCE', label: 'Alternance' },
  { valeur: 'STAGE', label: 'Stage' },
]
const PERIODES = [
  { valeur: 0, label: 'Toutes les dates' },
  { valeur: 1, label: 'Dernières 24 h' },
  { valeur: 3, label: '3 derniers jours' },
  { valeur: 7, label: '7 derniers jours' },
  { valeur: 30, label: '30 derniers jours' },
]
const RECHERCHE_DEFAUT = { q: '', lieu: '', pays: 'FR', contrat: '', domaine: '', tri: 'pertinence', periode: 0 }
const CLE_FILTRES = 'postulyn_recherche_offres'
const CLE_MODE = 'postulyn_mode_offres'

function lire(cle, defaut) {
  try {
    const v = localStorage.getItem(cle)
    return v ? JSON.parse(v) : defaut
  } catch {
    return defaut
  }
}
function ecrire(cle, valeur) {
  try { localStorage.setItem(cle, JSON.stringify(valeur)) } catch { /* stockage indisponible */ }
}

export default function Offres() {
  const navigate = useNavigate()
  const [mode, setMode] = useState(() => lire(CLE_MODE, 'recherche'))
  const [form, setForm] = useState(() => ({ ...RECHERCHE_DEFAUT, ...lire(CLE_FILTRES, {}) }))
  const [criteres, setCriteres] = useState({ pays: [{ code: 'FR', nom: 'France' }], domaines: [] })
  const [resultat, setResultat] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  // lien de l'offre -> id de la candidature deja creee
  const [suivies, setSuivies] = useState({})

  useEffect(() => {
    api.criteresOffres().then(setCriteres).catch(() => {})
    api.candidatures().then((liste) => {
      const map = {}
      liste.forEach((c) => { if (c.lienOffre) map[c.lienOffre] = c.id })
      setSuivies(map)
    }).catch(() => {})
  }, [])

  async function chercher(criteresRecherche, page = 0) {
    setErreur('')
    setChargement(true)
    ecrire(CLE_FILTRES, criteresRecherche)
    try {
      const res = await api.rechercheOffres({ ...criteresRecherche, page })
      setResultat((prev) => (page > 0 && prev ? { ...res, offres: [...prev.offres, ...res.offres] } : res))
    } catch (e) {
      setErreur(e.message)
      if (page === 0) setResultat(null)
    } finally {
      setChargement(false)
    }
  }

  // Premiere visite : on affiche tout de suite les offres les plus recentes
  useEffect(() => {
    if (mode === 'recherche') chercher(form)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode])

  const changerMode = (m) => {
    setMode(m)
    ecrire(CLE_MODE, m)
  }
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  /** Filtre change : on relance aussitot */
  const filtrer = (cle, valeur) => {
    const f = { ...form, [cle]: valeur }
    setForm(f)
    chercher(f)
  }
  const onSubmit = (e) => {
    e.preventDefault()
    chercher(form)
  }
  const essayer = (q) => {
    const f = { ...form, q }
    setForm(f)
    chercher(f)
  }

  const ajouter = async (offre) => {
    const c = await creerCandidatureDepuisOffre(offre)
    setSuivies((s) => ({ ...s, [offre.url]: c.id }))
    return c
  }
  const ouvrir = (id, outil) => navigate(`/candidatures/${id}${outil ? `?outil=${outil}` : ''}`)

  const interpretation = resultat?.interpretation
  const items = (resultat?.offres ?? []).map((o) => ({ offre: o.offre }))
  const equivalents = (interpretation?.synonymes ?? []).slice(0, 4)

  return (
    <div className="page-offres">
      <div className="page-head">
        <div>
          <h1>Offres d'emploi</h1>
          <p className="muted">Des milliers d'offres en France et à l'étranger, triées pour ce que tu cherches vraiment.</p>
        </div>
        <div className="tabs modes-offres" role="tablist">
          <button type="button" role="tab" aria-selected={mode === 'recherche'} className={mode === 'recherche' ? 'active' : ''}
            onClick={() => changerMode('recherche')}><Search size={16} /> Rechercher</button>
          <button type="button" role="tab" aria-selected={mode === 'pour-moi'} className={mode === 'pour-moi' ? 'active' : ''}
            onClick={() => changerMode('pour-moi')}><Sparkles size={16} /> Selon mon CV</button>
        </div>
      </div>

      {mode === 'pour-moi' ? (
        <OffresPourMoi pays={criteres.pays} suivies={suivies} setSuivies={setSuivies} />
      ) : (
        <>
          <form className="barre-recherche card" onSubmit={onSubmit} role="search">
            <div className="barre-champs">
              <label className="champ-recherche">
                <Search size={19} aria-hidden="true" />
                <input name="q" value={form.q} onChange={onChange} maxLength={200}
                  placeholder="Métier, compétence ou mot-clé (ex. dev full stack, comptable, data)" aria-label="Métier ou mot-clé" />
              </label>
              <label className="champ-lieu">
                <MapPin size={18} aria-hidden="true" />
                <input name="lieu" value={form.lieu} onChange={onChange} maxLength={100}
                  placeholder="Ville, département ou région" aria-label="Lieu" />
              </label>
              <label className="champ-pays">
                <Globe size={18} aria-hidden="true" />
                <select name="pays" value={form.pays} onChange={(e) => filtrer('pays', e.target.value)} aria-label="Pays">
                  {criteres.pays.map((p) => <option key={p.code} value={p.code}>{p.nom}</option>)}
                </select>
              </label>
              <button className="btn primary btn-rechercher" disabled={chargement}>Rechercher</button>
            </div>
            <div className="barre-filtres">
              <div className="pastilles" role="radiogroup" aria-label="Type de contrat">
                {CONTRATS.map((c) => (
                  <button key={c.valeur} type="button" role="radio" aria-checked={form.contrat === c.valeur}
                    className={`pastille ${form.contrat === c.valeur ? 'active' : ''}`} onClick={() => filtrer('contrat', c.valeur)}>
                    {c.label}
                  </button>
                ))}
              </div>
              <div className="filtres-selects">
                <select value={form.periode} onChange={(e) => filtrer('periode', Number(e.target.value))} aria-label="Date de publication">
                  {PERIODES.map((p) => <option key={p.valeur} value={p.valeur}>{p.label}</option>)}
                </select>
                <select value={form.domaine} onChange={(e) => filtrer('domaine', e.target.value)} aria-label="Domaine">
                  <option value="">Tous les domaines</option>
                  {criteres.domaines.map((d) => <option key={d.code} value={d.code}>{d.libelle}</option>)}
                </select>
                <select value={form.tri} onChange={(e) => filtrer('tri', e.target.value)} aria-label="Trier">
                  <option value="pertinence">Les plus pertinentes</option>
                  <option value="date">Les plus récentes</option>
                </select>
              </div>
            </div>
          </form>

          {interpretation && !chargement && (
            <div className="interpretation">
              <span>
                <strong>{items.length}{resultat.pageSuivante ? '+' : ''} offre{items.length > 1 ? 's' : ''}</strong> pour
                « {interpretation.intitule} »
                {interpretation.contrat && !form.contrat && <> en {interpretation.contrat.toLowerCase()}</>}
                {interpretation.lieu && !form.lieu && <> à {interpretation.lieu}</>}
              </span>
              {equivalents.length > 0 && (
                <span className="interpretation-equivalents">
                  Inclut aussi :
                  {equivalents.map((s) => (
                    <button key={s} type="button" className="lien-equivalent" onClick={() => essayer(s)}
                      title={`Chercher seulement « ${s} »`}>{s}</button>
                  ))}
                </span>
              )}
            </div>
          )}
          {!interpretation && resultat && !chargement && (
            <div className="interpretation"><span><strong>{items.length}{resultat.pageSuivante ? '+' : ''} offres récentes</strong></span></div>
          )}

          {erreur && <div className="card alerte"><strong>La recherche n'a pas abouti</strong><p>{erreur}</p></div>}
          {chargement && !resultat?.offres?.length && <Squelette />}

          {resultat && <ListeDetailOffres items={items} suivies={suivies} onAjouter={ajouter} onOuvrir={ouvrir} />}

          {resultat && items.length === 0 && !chargement && (
            <div className="card aucune-offre">
              <SearchX size={36} aria-hidden="true" />
              <h2>Aucune offre ne correspond pour l'instant</h2>
              <p className="muted">Élargis la zone, retire un filtre ou essaie un intitulé proche.</p>
              {resultat.suggestions?.length > 0 && (
                <div className="pastilles centre-pastilles">
                  {resultat.suggestions.map((s) => (
                    <button key={s} type="button" className="pastille" onClick={() => essayer(s)}>{s}</button>
                  ))}
                </div>
              )}
              {(form.contrat || form.periode > 0 || form.domaine || form.lieu) && (
                <button className="btn ghost" onClick={() => {
                  const f = { ...form, contrat: '', periode: 0, domaine: '', lieu: '' }
                  setForm(f)
                  chercher(f)
                }}>Retirer tous les filtres</button>
              )}
            </div>
          )}

          {resultat?.pageSuivante && !chargement && items.length > 0 && (
            <div className="centre">
              <button className="btn ghost" onClick={() => chercher(form, resultat.page + 1)}>Afficher plus d'offres</button>
            </div>
          )}
          {chargement && resultat?.offres?.length > 0 && <p className="muted centre-texte">Chargement…</p>}
        </>
      )}

      <BoutonFavori />
    </div>
  )
}

function Squelette() {
  return (
    <div className="resultats-offres squelette" aria-busy="true" aria-label="Recherche en cours">
      <ul className="liste-offres">
        {[1, 2, 3, 4, 5].map((i) => (
          <li key={i}><div className="ligne-offre"><i /><i /><i /></div></li>
        ))}
      </ul>
      <div className="detail-offre card"><i /><i /><i /><i /></div>
    </div>
  )
}
