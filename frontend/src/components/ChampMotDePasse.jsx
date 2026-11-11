import { useState } from 'react'
import { forceMotDePasse, regleRespectee } from '../motDePasse.js'
import { Eye, EyeOff } from 'lucide-react'

// Champ mot de passe avec bouton afficher / masquer et, en option, la jauge de force
export default function ChampMotDePasse({ avecForce = false, value, ...props }) {
  const [visible, setVisible] = useState(false)
  const force = forceMotDePasse(value)

  return (
    <>
      <span className="champ-mdp">
        <input type={visible ? 'text' : 'password'} value={value} {...props} />
        <button type="button" className="oeil" onClick={() => setVisible(!visible)}
          aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'} title={visible ? 'Masquer' : 'Afficher'}>
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </span>
      {avecForce && value && (
        <span className="force-mdp" aria-live="polite">
          <span className="jauge-force"><span className={`niveau-${force.niveau}`} style={{ width: `${force.niveau * 25}%` }} /></span>
          <span className="small">
            <strong>{force.libelle}</strong>
            {!regleRespectee(value) && <span className="muted"> · 8 caractères minimum, avec une lettre et un chiffre</span>}
          </span>
        </span>
      )}
    </>
  )
}
