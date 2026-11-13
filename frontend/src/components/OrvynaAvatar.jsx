import { useId } from 'react'

/**
 * Avatar anime de ORVYNA, l'assistante IA de Postulyn : un visage lumineux qui cligne des yeux et respire.
 * etat : 'repos' (par defaut), 'reflechit' (les yeux balaient, pendant qu'elle prepare sa reponse)
 * ou 'parle' (la bouche s'anime). Les animations s'arretent si l'utilisateur a demande moins de mouvement.
 */
export default function OrvynaAvatar({ size = 40, etat = 'repos', halo = false }) {
  const id = useId().replace(/:/g, '')
  return (
    <span className={`orvyna-avatar orvyna-${etat} ${halo ? 'orvyna-halo' : ''}`} role="img" aria-label="ORVYNA"
      style={{ width: size, height: size }}>
      <svg viewBox="0 0 100 100" width={size} height={size} aria-hidden="true">
        <defs>
          <radialGradient id={`tete${id}`} cx="35%" cy="28%" r="80%">
            <stop offset="0%" stopColor="#8fb3ff" />
            <stop offset="45%" stopColor="#3b5bdb" />
            <stop offset="100%" stopColor="#1e1b6b" />
          </radialGradient>
          <linearGradient id={`visiere${id}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#0b1230" />
            <stop offset="100%" stopColor="#141c48" />
          </linearGradient>
          <radialGradient id={`oeil${id}`} cx="50%" cy="40%" r="60%">
            <stop offset="0%" stopColor="#ecfeff" />
            <stop offset="55%" stopColor="#67e8f9" />
            <stop offset="100%" stopColor="#0891b2" />
          </radialGradient>
        </defs>
        <g className="o-corps">
          {/* Antenne lumineuse */}
          <line x1="50" y1="14" x2="50" y2="6" stroke="#8fb3ff" strokeWidth="3" strokeLinecap="round" />
          <circle className="o-antenne" cx="50" cy="5" r="4" fill="#67e8f9" />
          {/* Tete */}
          <circle cx="50" cy="54" r="40" fill={`url(#tete${id})`} />
          <ellipse cx="36" cy="30" rx="14" ry="7" fill="#fff" opacity="0.18" transform="rotate(-20 36 30)" />
          {/* Visiere et regard */}
          <rect x="20" y="40" width="60" height="28" rx="14" fill={`url(#visiere${id})`} />
          <g className="o-yeux">
            <ellipse className="o-oeil" cx="38" cy="54" rx="6" ry="7.5" fill={`url(#oeil${id})`} />
            <ellipse className="o-oeil" cx="62" cy="54" rx="6" ry="7.5" fill={`url(#oeil${id})`} />
          </g>
          {/* Bouche */}
          <rect className="o-bouche" x="42" y="76" width="16" height="3.5" rx="1.75" fill="#c7d7ff" />
        </g>
      </svg>
    </span>
  )
}
