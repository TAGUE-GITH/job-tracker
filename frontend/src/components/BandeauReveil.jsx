import { useEffect, useState } from 'react'
import { LoaderCircle } from 'lucide-react'

// Affiche un message quand le serveur (hebergement gratuit) met du temps a se reveiller
export default function BandeauReveil() {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const ecouter = (e) => setVisible(e.detail)
    window.addEventListener('postilyn:lent', ecouter)
    return () => window.removeEventListener('postilyn:lent', ecouter)
  }, [])

  if (!visible) return null
  return (
    <div className="bandeau-reveil" role="status">
      <span className="sablier"><LoaderCircle size={18} /></span>
      <span>
        <strong>Le serveur se réveille…</strong> Postilyn est hébergé gratuitement : après une pause, le premier
        chargement peut prendre jusqu'à une minute.
      </span>
    </div>
  )
}
