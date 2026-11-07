import { useState } from 'react'
import { Link, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Login from './pages/Login.jsx'
import Landing from './pages/Landing.jsx'
import Logo from './components/Logo.jsx'
import TagiaWidget from './components/TagiaWidget.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Candidatures from './pages/Candidatures.jsx'
import CandidatureForm from './pages/CandidatureForm.jsx'
import CandidatureDetail from './pages/CandidatureDetail.jsx'
import Documents from './pages/Documents.jsx'
import Offres from './pages/Offres.jsx'
import Importer from './pages/Importer.jsx'
import Compte from './pages/Compte.jsx'
import BandeauReveil from './components/BandeauReveil.jsx'
import BandeauDemo from './components/BandeauDemo.jsx'
import Demo from './pages/Demo.jsx'

function Layout() {
  const { user, logout } = useAuth()
  const [menuOuvert, setMenuOuvert] = useState(false)
  if (!user) return <Navigate to="/accueil" replace />

  const fermer = () => setMenuOuvert(false)

  return (
    <div className="app">
      <BandeauDemo />
      <header className={`topbar ${menuOuvert ? 'menu-ouvert' : ''}`}>
        <Link to="/accueil" className="logo"><Logo size={28} /></Link>
        <button className="burger" onClick={() => setMenuOuvert(!menuOuvert)}
          aria-label={menuOuvert ? 'Fermer le menu' : 'Ouvrir le menu'} aria-expanded={menuOuvert}>
          <span /><span /><span />
        </button>
        <nav onClick={fermer}>
          <NavLink to="/" end>Tableau de bord</NavLink>
          <NavLink to="/offres">Offres</NavLink>
          <NavLink to="/candidatures">Candidatures</NavLink>
          <NavLink to="/documents">CV & lettres</NavLink>
        </nav>
        <div className="user">
          <Link to="/compte" onClick={fermer} title="Mon compte">👤 {user.nom}</Link>
          <button className="btn ghost" onClick={logout}>Déconnexion</button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
      <TagiaWidget />
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
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="candidatures" element={<Candidatures />} />
          <Route path="candidatures/nouvelle" element={<CandidatureForm />} />
          <Route path="candidatures/:id" element={<CandidatureDetail />} />
          <Route path="candidatures/:id/modifier" element={<CandidatureForm />} />
          <Route path="documents" element={<Documents />} />
          <Route path="offres" element={<Offres />} />
          <Route path="compte" element={<Compte />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}
