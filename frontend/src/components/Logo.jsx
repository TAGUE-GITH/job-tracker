// Logo Taguora : une courbe ascendante qui finit sur une etoile (la candidature qui aboutit)
export function LogoMark({ size = 32 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <linearGradient id="taguora-g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="1" stopColor="#d946ef" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="18" fill="url(#taguora-g)" />
      <path d="M14 44 L26 32 L34 38 L48 22" fill="none" stroke="#fff" strokeWidth="5"
        strokeLinecap="round" strokeLinejoin="round" />
      <path d="M48 12 l2.6 5.4 5.4 2.6 -5.4 2.6 -2.6 5.4 -2.6 -5.4 -5.4 -2.6 5.4 -2.6z" fill="#fde68a" />
    </svg>
  )
}

export default function Logo({ size = 32 }) {
  return (
    <span className="brand">
      <LogoMark size={size} />
      <span className="brand-name">Taguora</span>
    </span>
  )
}
