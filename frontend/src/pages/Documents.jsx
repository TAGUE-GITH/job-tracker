import { useEffect, useState } from 'react'
import { api } from '../api'
import DocumentActions from '../components/DocumentActions.jsx'
import { formatDate } from '../labels'

export default function Documents() {
  const [docs, setDocs] = useState([])
  const [type, setType] = useState('CV')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api.documents().then(setDocs).catch((e) => setErreur(e.message))
  }, [])

  const uploader = async (e) => {
    const fichier = e.target.files[0]
    if (!fichier) return
    try {
      const doc = await api.uploadDocument(fichier, type)
      setDocs([doc, ...docs])
      setErreur('')
    } catch (err) {
      setErreur(err.message)
    }
    e.target.value = ''
  }

  const supprimer = async (doc) => {
    if (!confirm(`Supprimer ${doc.nomFichier} ? Il sera retiré des candidatures qui l'utilisent.`)) return
    await api.supprimerDocument(doc.id)
    setDocs(docs.filter((d) => d.id !== doc.id))
  }

  return (
    <>
      <div className="page-head">
        <h1>CV & lettres de motivation</h1>
        <div className="row">
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="CV">CV</option>
            <option value="LETTRE">Lettre de motivation</option>
          </select>
          <span className="btn primary upload">
            + Importer
            <input type="file" accept=".pdf,.doc,.docx,.odt,.txt" onChange={uploader} />
          </span>
        </div>
      </div>

      {erreur && <p className="erreur">{erreur}</p>}
      {docs.length === 0 ? (
        <div className="card vide"><p className="muted">Aucun document pour l'instant.</p></div>
      ) : (
        <div className="card table-wrap table-cards">
          <table>
            <thead>
              <tr><th>Fichier</th><th>Type</th><th>Ajouté le</th><th /></tr>
            </thead>
            <tbody>
              {docs.map((d) => (
                <tr key={d.id}>
                  <td data-label="Fichier"><DocumentActions doc={d} /></td>
                  <td data-label="Type">{d.type === 'CV' ? 'CV' : 'Lettre'}</td>
                  <td data-label="Ajouté le">{formatDate(d.dateAjout)}</td>
                  <td className="right td-actions"><button className="btn danger small" onClick={() => supprimer(d)}>Supprimer</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
