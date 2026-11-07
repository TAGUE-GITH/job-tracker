import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

// Rappelle qu'on est sur un compte de demonstration et invite a creer un vrai compte
export default function BandeauDemo() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user?.demo) return null

  const creerCompte = () => {
    logout()
    navigate('/login?mode=inscription')
  }

  return (
    <div className="bandeau-demo">
      <span>👀 <strong>Mode démo</strong> : données fictives, supprimées automatiquement dans quelques heures.</span>
      <button type="button" className="btn small btn-blanc" onClick={creerCompte}>Créer mon vrai compte</button>
    </div>
  )
}
