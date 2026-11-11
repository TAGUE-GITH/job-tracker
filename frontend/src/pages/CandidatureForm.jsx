import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { aujourdhui, formatDate } from '../labels'

const VIDE = {
  nomEntreprise: '', poste: '', lienOffre: '', texteOffre: '',
  dateCandidature: aujourdhui(), cvId: '', lettreId: '', abandonnee: false,
}

export default function CandidatureForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  // Offre envoyee par le favori "Ajouter a Postilyn" ou par la page Offres
  const offre = useLocation().state?.offre
  const [form, setForm] = useState(() => (offre && !id ? { ...VIDE, ...offre } : VIDE))
  const [documents, setDocuments] = useState([])
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    api.documents().then(setDocuments).catch(() => {})
    if (id) {
      api.candidature(id).then((c) => setForm({
        nomEntreprise: c.nomEntreprise,
        poste: c.poste,
        lienOffre: c.lienOffre ?? '',
        texteOffre: c.texteOffre ?? '',
        dateCandidature: c.dateCandidature,
        cvId: c.cv?.id ?? '',
        lettreId: c.lettre?.id ?? '',
        abandonnee: c.statut === 'ABANDONNEE',
      })).catch((e) => setErreur(e.message))
    }
  }, [id])

  const onChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value })
  }

  const uploader = async (e, type) => {
    const fichier = e.target.files[0]
    if (!fichier) return
    try {
      const doc = await api.uploadDocument(fichier, type)
      setDocuments([doc, ...documents])
      setForm((f) => ({ ...f, [type === 'CV' ? 'cvId' : 'lettreId']: doc.id }))
    } catch (err) {
      setErreur(err.message)
    }
    e.target.value = ''
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    setEnvoi(true)
    const data = { ...form, cvId: form.cvId || null, lettreId: form.lettreId || null }
    try {
      const c = id ? await api.modifierCandidature(id, data) : await api.creerCandidature(data)
      navigate(`/candidatures/${c.id}`)
    } catch (err) {
      setErreur(err.message)
      setEnvoi(false)
    }
  }

  const options = (type) => documents.filter((d) => d.type === type)

  return (
    <form className="card form" onSubmit={onSubmit}>
      <h1>{id ? 'Modifier la candidature' : 'Nouvelle candidature'}</h1>
      {offre && !id && (
        <p className="import-info">
          Offre importée{offre.lienOffre && <> depuis <strong>{hote(offre.lienOffre)}</strong></>} : vérifie
          l'entreprise et le poste, choisis ton CV, puis enregistre.
        </p>
      )}

      <div className="grid-2">
        <label>
          Entreprise *
          <input name="nomEntreprise" value={form.nomEntreprise} onChange={onChange} required />
        </label>
        <label>
          Poste *
          <input name="poste" value={form.poste} onChange={onChange} required />
        </label>
        <label>
          Lien de l'offre
          <input name="lienOffre" type="url" placeholder="https://…" value={form.lienOffre} onChange={onChange} />
        </label>
        <label>
          Date de candidature *
          <input name="dateCandidature" type="date" value={form.dateCandidature} onChange={onChange} required />
        </label>
      </div>

      <label>
        Texte de l'offre
        <textarea name="texteOffre" rows={10} placeholder="Colle ici le texte de l'annonce (utilisé par l'assistant IA)"
          value={form.texteOffre} onChange={onChange} />
      </label>

      <div className="grid-2">
        <DocumentChoix label="CV utilisé" name="cvId" valeur={form.cvId} onChange={onChange}
          options={options('CV')} onUpload={(e) => uploader(e, 'CV')} />
        <DocumentChoix label="Lettre de motivation (facultatif)" name="lettreId" valeur={form.lettreId}
          onChange={onChange} options={options('LETTRE')} onUpload={(e) => uploader(e, 'LETTRE')} />
      </div>

      {id && (
        <label className="checkbox">
          <input type="checkbox" name="abandonnee" checked={form.abandonnee} onChange={onChange} />
          J'ai abandonné cette candidature
        </label>
      )}

      {erreur && <p className="erreur">{erreur}</p>}
      <div className="actions">
        <button type="button" className="btn ghost" onClick={() => navigate(-1)}>Annuler</button>
        <button className="btn primary" disabled={envoi}>{envoi ? 'Enregistrement…' : 'Enregistrer'}</button>
      </div>
    </form>
  )
}

function DocumentChoix({ label, name, valeur, onChange, options, onUpload }) {
  return (
    <label>
      {label}
      <div className="row">
        <select name={name} value={valeur} onChange={onChange}>
          <option value="">— Aucun —</option>
          {options.map((d) => (
            <option key={d.id} value={d.id}>{d.nomFichier} ({formatDate(d.dateAjout)})</option>
          ))}
        </select>
        <span className="btn ghost upload">
          Importer
          <input type="file" accept=".pdf,.doc,.docx,.odt,.txt" onChange={onUpload} />
        </span>
      </div>
    </label>
  )
}

function hote(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return 'le web'
  }
}
