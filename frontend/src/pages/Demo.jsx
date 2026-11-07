import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import Logo from '../components/Logo.jsx'

// /demo : cree un compte de demonstration rempli et y connecte le visiteur (lien a partager aux recruteurs).
// /demo?vers=/candidatures ouvre directement une page.
export default function Demo() {
  const { user, demo } = useAuth()
  const [params] = useSearchParams()
  const [erreur, setErreur] = useState('')
  const lance = useRef(false)
  const vers = params.get('vers')?.startsWith('/') ? params.get('vers') : '/'

  useEffect(() => {
    if (user || lance.current) return
    lance.current = true
    demo().catch((e) => setErreur(e.message))
  }, [user, demo])

  if (user) return <Navigate to={vers} replace />
  return (
    <div className="auth-page">
      <div className="card auth-card demo-chargement">
        <Logo size={40} />
        {erreur ? (
          <>
            <p className="erreur">{erreur}</p>
            <Link to="/accueil" className="btn ghost">Retour à l'accueil</Link>
          </>
        ) : (
          <>
            <div className="spinner" aria-hidden="true" />
            <p><strong>Préparation de ton espace de démonstration…</strong></p>
            <p className="muted small">Candidatures, entretiens, CV et lettre d'exemple : tout est fictif.</p>
          </>
        )}
      </div>
    </div>
  )
}
