import { useId } from 'react'

// Avatar de TAGIA, l'assistant IA de Taguora : un petit compagnon rond avec une antenne-etoile
export default function TagiaAvatar({ size = 40 }) {
  const id = useId()
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" role="img" aria-label="TAGIA">
      <defs>
        <linearGradient id={`${id}-corps`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="1" stopColor="#d946ef" />
        </linearGradient>
        <linearGradient id={`${id}-visage`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#1e1b4b" />
          <stop offset="1" stopColor="#312e81" />
        </linearGradient>
      </defs>
      {/* antenne */}
      <line x1="32" y1="13" x2="32" y2="6" stroke="#a5b4fc" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M32 0.5 l1.8 3.7 3.7 1.8 -3.7 1.8 -1.8 3.7 -1.8 -3.7 -3.7 -1.8 3.7 -1.8z" fill="#fde68a" />
      {/* tete */}
      <rect x="6" y="12" width="52" height="46" rx="20" fill={`url(#${id}-corps)`} />
      {/* oreilles */}
      <rect x="2" y="29" width="6" height="12" rx="3" fill="#a78bfa" />
      <rect x="56" y="29" width="6" height="12" rx="3" fill="#a78bfa" />
      {/* visiere */}
      <rect x="13" y="22" width="38" height="26" rx="12" fill={`url(#${id}-visage)`} />
      {/* yeux */}
      <ellipse cx="25" cy="33" rx="3.6" ry="4.4" fill="#67e8f9">
        <animate attributeName="ry" values="4.4;4.4;0.6;4.4;4.4" keyTimes="0;0.9;0.94;0.98;1" dur="4s" repeatCount="indefinite" />
      </ellipse>
      <ellipse cx="39" cy="33" rx="3.6" ry="4.4" fill="#67e8f9">
        <animate attributeName="ry" values="4.4;4.4;0.6;4.4;4.4" keyTimes="0;0.9;0.94;0.98;1" dur="4s" repeatCount="indefinite" />
      </ellipse>
      {/* sourire et joues */}
      <path d="M26 41 Q32 45.5 38 41" fill="none" stroke="#67e8f9" strokeWidth="2.4" strokeLinecap="round" />
      <circle cx="18.5" cy="40" r="2.2" fill="#f0abfc" opacity="0.7" />
      <circle cx="45.5" cy="40" r="2.2" fill="#f0abfc" opacity="0.7" />
    </svg>
  )
}
