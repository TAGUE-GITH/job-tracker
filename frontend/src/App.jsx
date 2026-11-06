import { Link, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Login from './pages/Login.jsx'
import Landing from './pages/Landing.jsx'
import Logo from './components/Logo.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Candidatures from './pages/Candidatures.jsx'
import CandidatureForm from './pages/CandidatureForm.jsx'
import CandidatureDetail from './pages/CandidatureDetail.jsx'
import Documents from './pages/Documents.jsx'

function Layout() {
  const { user, logout } = useAuth()
  if (!user) return <Navigate to="/accueil" replace />

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/accueil" className="logo"><Logo size={28} /></Link>
        <nav>
          <NavLink to="/" end>Tableau de bord</NavLink>
          <NavLink to="/candidatures">Candidatures</NavLink>
          <NavLink to="/documents">CV & lettres</NavLink>
        </nav>
        <div className="user">
          <span>{user.nom}</span>
          <button className="btn ghost" onClick={logout}>Déconnexion</button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/accueil" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="candidatures" element={<Candidatures />} />
        <Route path="candidatures/nouvelle" element={<CandidatureForm />} />
        <Route path="candidatures/:id" element={<CandidatureDetail />} />
        <Route path="candidatures/:id/modifier" element={<CandidatureForm />} />
        <Route path="documents" element={<Documents />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
