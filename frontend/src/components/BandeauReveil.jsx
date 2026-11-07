import { useEffect, useState } from 'react'

// Affiche un message quand le serveur (hebergement gratuit) met du temps a se reveiller
export default function BandeauReveil() {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const ecouter = (e) => setVisible(e.detail)
    window.addEventListener('taguora:lent', ecouter)
    return () => window.removeEventListener('taguora:lent', ecouter)
  }, [])

  if (!visible) return null
  return (
    <div className="bandeau-reveil" role="status">
      <span className="sablier">⏳</span>
      <span>
        <strong>Le serveur se réveille…</strong> Taguora est hébergé gratuitement : après une pause, le premier
        chargement peut prendre jusqu'à une minute.
      </span>
    </div>
  )
}
