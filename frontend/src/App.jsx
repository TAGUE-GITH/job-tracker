import { useEffect, useState } from 'react'
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import {
  Briefcase, FileText, Home, LayoutDashboard, LifeBuoy, LogOut, Menu, MessageSquareHeart, Search, Settings,
  ShieldCheck, X,
} from 'lucide-react'
import { useAuth } from './auth.jsx'
import { api } from './api'
import { initiales } from './labels'
import Login from './pages/Login.jsx'
import Landing from './pages/Landing.jsx'
import Logo from './components/Logo.jsx'
import OrvynaWidget from './components/OrvynaWidget.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Candidatures from './pages/Candidatures.jsx'
import CandidatureForm from './pages/CandidatureForm.jsx'
import CandidatureDetail from './pages/CandidatureDetail.jsx'
import Documents from './pages/Documents.jsx'
import Offres from './pages/Offres.jsx'
import Importer from './pages/Importer.jsx'
import Compte from './pages/Compte.jsx'
import Administration from './pages/Administration.jsx'
import BandeauReveil from './components/BandeauReveil.jsx'
import BandeauDemo from './components/BandeauDemo.jsx'
import Demo from './pages/Demo.jsx'
import MotDePasseOublie from './pages/MotDePasseOublie.jsx'
import Reinitialiser from './pages/Reinitialiser.jsx'
import Contact from './pages/Contact.jsx'
import Conditions from './pages/Conditions.jsx'
import Avis from './pages/Avis.jsx'

const NAVIGATION = [
  { to: '/', label: 'Tableau de bord', icone: LayoutDashboard, end: true },
  { to: '/offres', label: 'Offres d’emploi', icone: Search },
  { to: '/candidatures', label: 'Candidatures', icone: Briefcase },
  { to: '/documents', label: 'CV et lettres', icone: FileText },
]

function Sidebar({ user, onFermer, onDeconnexion }) {
  return (
    <aside className="sidebar" aria-label="Navigation principale">
      <div className="sidebar-head">
        <Link to="/accueil" onClick={onFermer} title="Page d'accueil de Postulyn"><Logo size={28} /></Link>
        <button className="btn-icone sidebar-fermer" onClick={onFermer} aria-label="Fermer le menu"><X size={20} /></button>
      </div>
      <nav className="sidebar-nav" onClick={onFermer}>
        <span className="sidebar-section">Espace de travail</span>
        {NAVIGATION.map(({ to, label, icone: Icone, end }) => (
          <NavLink key={to} to={to} end={end}><Icone size={18} strokeWidth={1.9} />{label}</NavLink>
        ))}
        <span className="sidebar-section">Compte</span>
        <NavLink to="/compte"><Settings size={18} strokeWidth={1.9} />Paramètres du compte</NavLink>
        {user.admin && <NavLink to="/administration"><ShieldCheck size={18} strokeWidth={1.9} />Administration</NavLink>}
        <span className="sidebar-section">Postulyn</span>
        <NavLink to="/avis"><MessageSquareHeart size={18} strokeWidth={1.9} />Donner mon avis</NavLink>
        <NavLink to="/contact"><LifeBuoy size={18} strokeWidth={1.9} />Aide et contact</NavLink>
        <NavLink to="/accueil"><Home size={18} strokeWidth={1.9} />Page d'accueil</NavLink>
      </nav>
      <div className="sidebar-user">
        <span className="avatar-initiales" aria-hidden="true">{initiales(user.nom)}</span>
        <div className="sidebar-user-infos">
          <strong>{user.nom}</strong>
          <small>{user.email}</small>
        </div>
        <button className="btn-icone" onClick={onDeconnexion} title="Se déconnecter" aria-label="Se déconnecter">
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  )
}

function Layout() {
  const { user, logout, majUtilisateur } = useAuth()
  const [menuOuvert, setMenuOuvert] = useState(false)
  const { pathname } = useLocation()

  // Droits a jour sans devoir se reconnecter (ex. compte nomme administrateur)
  useEffect(() => {
    if (!user || user.demo) return
    api.profil().then((p) => { if (!!p.admin !== !!user.admin) majUtilisateur({ admin: !!p.admin }) }).catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  // Le tiroir mobile se ferme a chaque changement de page
  useEffect(() => { setMenuOuvert(false) }, [pathname])

  if (!user) return <Navigate to="/accueil" replace />
  const fermer = () => setMenuOuvert(false)

  return (
    <div className={`app ${menuOuvert ? 'menu-ouvert' : ''}`}>
      <Sidebar user={user} onFermer={fermer} onDeconnexion={logout} />
      <div className="voile" onClick={fermer} aria-hidden="true" />
      <div className="contenu">
        <BandeauDemo />
        <header className="topbar-mobile">
          <Link to="/accueil" title="Page d'accueil de Postulyn"><Logo size={26} /></Link>
          <button className="btn-icone" onClick={() => setMenuOuvert(true)} aria-label="Ouvrir le menu"
            aria-expanded={menuOuvert}>
            <Menu size={20} />
          </button>
        </header>
        <main className="container">
          <Outlet />
        </main>
      </div>
      <OrvynaWidget />
    </div>
  )
}

export default function App() {
  return (
    <>
      <BandeauReveil />
      <Routes>
        <Route path="/accueil" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/importer" element={<Importer />} />
        <Route path="/demo" element={<Demo />} />
        <Route path="/mot-de-passe-oublie" element={<MotDePasseOublie />} />
        <Route path="/reinitialiser" element={<Reinitialiser />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/conditions" element={<Conditions />} />
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="candidatures" element={<Candidatures />} />
          <Route path="candidatures/nouvelle" element={<CandidatureForm />} />
          <Route path="candidatures/:id" element={<CandidatureDetail />} />
          <Route path="candidatures/:id/modifier" element={<CandidatureForm />} />
          <Route path="documents" element={<Documents />} />
          <Route path="offres" element={<Offres />} />
          <Route path="compte" element={<Compte />} />
          <Route path="administration" element={<Administration />} />
          <Route path="avis" element={<Avis />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}
