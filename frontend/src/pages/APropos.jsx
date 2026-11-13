import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useAuth } from '../auth.jsx'
import Logo from '../components/Logo.jsx'
import { PiedDePage } from '../components/PagePublique.jsx'
import Portrait from '../components/Portrait.jsx'
import { ENGAGEMENTS, FONDATEUR, HISTOIRE } from '../fondateur.js'
import '../landing.css'

/** Page "A propos" : le fondateur, l'histoire de l'idee et les engagements de Postulyn. */
export default function APropos() {
  const { user } = useAuth()
  return (
    <div className="landing a-propos">
      <header className="landing-nav">
        <Link to="/accueil" className="landing-logo"><Logo size={30} /></Link>
        <nav>
          <Link to="/accueil">Accueil</Link>
          <a href="#histoire">L’histoire</a>
          <a href="#engagements">Engagements</a>
        </nav>
        <div className="landing-nav-actions">
          {!user && <Link to="/login" className="btn ghost">Connexion</Link>}
          <Link to={user ? '/' : '/login?mode=inscription'} className="btn primary">{user ? 'Mon espace' : 'Commencer'}</Link>
        </div>
      </header>

      <section className="section a-propos-hero">
        <span className="eyebrow">À propos</span>
        <h1>Une idée née d’une recherche d’emploi, construite pour tous les candidats</h1>
        <p className="hero-sub">
          Postulyn est un projet indépendant, conçu et développé par {FONDATEUR.prenom} {FONDATEUR.nom}, pour rendre
          la recherche d’emploi plus simple, plus organisée et plus juste.
        </p>
      </section>

      <section id="histoire" className="section fondateur">
        <aside className="fondateur-carte">
          <Portrait taille={132} />
          <h2>{FONDATEUR.prenom} {FONDATEUR.nom}</h2>
          <p className="fondateur-role">{FONDATEUR.role}</p>
          <div className="fondateur-parcours">
            {FONDATEUR.parcours.map((p) => (
              <div key={p.titre}>
                <small>{p.periode}</small>
                <strong>{p.titre}</strong>
                <span>{p.lieu}</span>
              </div>
            ))}
          </div>
          <div className="fondateur-liens">
            {FONDATEUR.liens.map((l) => (
              <a key={l.libelle} href={l.url} target="_blank" rel="noopener noreferrer">{l.libelle}</a>
            ))}
          </div>
        </aside>
        <div className="fondateur-histoire">
          <span className="eyebrow">L’histoire</span>
          <h2>Pourquoi j’ai créé Postulyn</h2>
          {HISTOIRE.map((para) => <p key={para.slice(0, 30)}>{para}</p>)}
        </div>
      </section>

      <section id="engagements" className="section">
        <div className="section-head">
          <span className="eyebrow">Engagements</span>
          <h2>Ce qui guide Postulyn</h2>
        </div>
        <div className="engagements">
          {ENGAGEMENTS.map((e, i) => (
            <article key={e.titre} className="engagement">
              <span className="engagement-num">{String(i + 1).padStart(2, '0')}</span>
              <h3>{e.titre}</h3>
              <p>{e.texte}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="final-cta">
        <div>
          <h2>Une idée, une remarque ?</h2>
          <p>Vos retours font évoluer Postulyn. Écrivez-moi, je lis chaque message.</p>
        </div>
        <Link to="/contact" className="btn btn-lg btn-white">Me contacter <ArrowRight size={18} /></Link>
      </section>

      <PiedDePage />
    </div>
  )
}
