import { useState } from 'react'
import { api } from '../api'

/** Nom du document cliquable (ouvre le PDF) suivi d'un bouton pour l'enregistrer sur l'appareil. */
export default function DocumentActions({ doc, libelle }) {
  const [erreur, setErreur] = useState('')
  if (!doc) return '—'

  const agir = (action) => async () => {
    setErreur('')
    try {
      await action(doc.id, doc.nomFichier)
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <span className="doc-actions">
      <button type="button" className="link" onClick={agir(api.ouvrirDocument)}>{libelle ?? doc.nomFichier}</button>
      <button type="button" className="btn ghost small" onClick={agir(api.telechargerDocument)}
        title={`Enregistrer ${doc.nomFichier} sur cet appareil`}>
        ⬇️ Télécharger
      </button>
      {erreur && <span className="erreur small">{erreur}</span>}
    </span>
  )
}
