import { Sparkles } from 'lucide-react'

// Pastille de ORVYNA, l'assistante IA de Postulyn : icone sobre sur fond de la couleur de marque
export default function OrvynaAvatar({ size = 40 }) {
  return (
    <span className="orvyna-avatar" role="img" aria-label="ORVYNA"
      style={{ width: size, height: size, borderRadius: size * 0.28 }}>
      <Sparkles size={Math.round(size * 0.5)} strokeWidth={2} aria-hidden="true" />
    </span>
  )
}
