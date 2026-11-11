// Logo Postulyn : monogramme "P" sobre sur un carre aux angles doux, suivi du nom
export function LogoMark({ size = 32 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true" className="logo-mark">
      <rect width="64" height="64" rx="14" fill="var(--brand, #1e3a8a)" />
      <path d="M22 48V16h13.5a10 10 0 0 1 0 20H22" fill="none" stroke="#fff" strokeWidth="6"
        strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="44" cy="46" r="4" fill="#60a5fa" />
    </svg>
  )
}

export default function Logo({ size = 32 }) {
  return (
    <span className="brand">
      <LogoMark size={size} />
      <span className="brand-name">Postulyn</span>
    </span>
  )
}
