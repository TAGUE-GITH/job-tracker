import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useAuth } from '../auth.jsx'
import Logo from './Logo.jsx'
import '../landing.css'

const ANNEE = new Date().getFullYear()

/** Mise en page des pages publiques (contact, conditions) : en-tete, contenu centre, pied de page. */
export default function PagePublique({ titre, sousTitre, children }) {
  const { user } = useAuth()
  return (
    <div className="landing page-publique">
      <header className="landing-nav">
        <Link to="/accueil" className="landing-logo"><Logo size={30} /></Link>
        <div className="landing-nav-actions">
          {user
            ? <Link to="/" className="btn ghost"><ArrowLeft size={16} /> Retour à mon espace</Link>
            : <Link to="/login" className="btn ghost">Connexion</Link>}
        </div>
      </header>
      <main className="page-publique-contenu">
        <h1>{titre}</h1>
        {sousTitre && <p className="page-publique-sous-titre">{sousTitre}</p>}
        {children}
      </main>
      <PiedDePage />
    </div>
  )
}

export function PiedDePage() {
  return (
    <footer className="landing-footer">
      <Logo size={22} />
      <nav className="liens-pied">
        <Link to="/contact">Aide et contact</Link>
        <Link to="/conditions">Conditions d'utilisation</Link>
        <Link to="/conditions#confidentialite">Confidentialité</Link>
      </nav>
      <span>© {ANNEE} Postulyn</span>
    </footer>
  )
}
