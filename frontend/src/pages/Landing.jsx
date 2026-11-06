import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import Logo from '../components/Logo.jsx'
import TagiaAvatar from '../components/TagiaAvatar.jsx'
import '../landing.css'

const FONCTIONNALITES = [
  {
    icone: '📌',
    titre: 'Chaque offre au même endroit',
    texte: "Lien, texte de l'annonce, entreprise, CV et lettre envoyés : tu retrouves tout en un clic, même si l'annonce disparaît.",
  },
  {
    icone: '🧭',
    titre: 'Une timeline par candidature',
    texte: 'Échange RH, test technique, entretiens, offre ou refus : chaque étape est notée et le statut se met à jour tout seul.',
  },
  {
    icone: '📊',
    titre: 'Tes statistiques en direct',
    texte: "Taux de réponse, d'entretien et d'offre, candidatures par mois, entonnoir de recrutement : tu sais ce qui marche.",
  },
  {
    icone: '🤖',
    titre: 'TAGIA, ton coach IA',
    texte: "À partir de l'offre et de tes notes, l'IA prépare les questions probables, des réponses STAR et te fait passer un entretien blanc.",
  },
  {
    icone: '📎',
    titre: 'Tes CV et lettres réutilisables',
    texte: 'Importe tes documents une fois, rattache-les à chaque candidature et sache exactement quelle version tu as envoyée.',
  },
  {
    icone: '🔒',
    titre: 'Tes données restent à toi',
    texte: 'Compte personnel, mot de passe chiffré, connexion sécurisée : personne d’autre ne voit tes candidatures.',
  },
]

const ETAPES = [
  { num: '1', titre: 'Ajoute une offre', texte: "Colle le lien et le texte de l'annonce, choisis ton CV. 30 secondes." },
  { num: '2', titre: 'Suis ton avancement', texte: 'Ajoute chaque échange au fil de l’eau : le tableau de bord se met à jour.' },
  { num: '3', titre: "Prépare l'entretien", texte: "L'IA lit l'offre et tes notes, puis t'entraîne question par question." },
]

export default function Landing() {
  const { user } = useAuth()
  const cta = user
    ? { to: '/', label: 'Ouvrir mon espace' }
    : { to: '/login?mode=inscription', label: 'Commencer gratuitement' }

  return (
    <div className="landing">
      <div className="landing-glow" aria-hidden="true" />

      <header className="landing-nav">
        <Link to="/accueil" className="landing-logo"><Logo size={34} /></Link>
        <nav>
          <a href="#fonctionnalites">Fonctionnalités</a>
          <a href="#ia">TAGIA, coach IA</a>
          <a href="#etapes">Comment ça marche</a>
        </nav>
        <div className="landing-nav-actions">
          {!user && <Link to="/login" className="btn ghost">Connexion</Link>}
          <Link to={cta.to} className="btn primary">{user ? 'Mon espace' : 'Créer un compte'}</Link>
        </div>
      </header>

      <section className="hero">
        <div className="hero-text">
          <span className="pill">✨ Nouveau · coach d'entretien IA inclus</span>
          <h1>
            Tes candidatures,<br />
            <span className="gradient-text">enfin sous contrôle.</span>
          </h1>
          <p className="hero-sub">
            Taguora centralise tes offres, suit chaque étape du recrutement, mesure tes progrès
            et te prépare aux entretiens avec une IA qui connaît l'annonce.
          </p>
          <div className="hero-actions">
            <Link to={cta.to} className="btn primary btn-lg">{cta.label} →</Link>
            <a href="#fonctionnalites" className="btn ghost btn-lg">Découvrir</a>
          </div>
          <ul className="hero-points">
            <li>✓ Gratuit</li>
            <li>✓ Sans carte bancaire</li>
            <li>✓ Prêt en 1 minute</li>
          </ul>
        </div>

        <div className="hero-visual" aria-hidden="true">
          <div className="mock">
            <div className="mock-bar"><span /><span /><span /></div>
            <div className="mock-body">
              <div className="mock-kpis">
                <div><small>Candidatures</small><strong>24</strong></div>
                <div><small>Taux de réponse</small><strong>42 %</strong></div>
                <div><small>Entretiens</small><strong>7</strong></div>
              </div>
              <div className="mock-chart">
                {[35, 55, 40, 70, 60, 90].map((h, i) => (
                  <span key={i} style={{ height: `${h}%`, animationDelay: `${i * 90}ms` }} />
                ))}
              </div>
              <div className="mock-rows">
                <div><b>Atlas Studio</b><em className="tag tag-blue">Entretien technique</em></div>
                <div><b>Nordlys</b><em className="tag tag-amber">Échange RH</em></div>
                <div><b>Kaliop Labs</b><em className="tag tag-green">Offre reçue 🎉</em></div>
              </div>
            </div>
          </div>
          <div className="float-card float-ia">
            <span className="float-icon"><TagiaAvatar size={36} /></span>
            <div><b>Question probable</b><small>« Parle-moi d'un bug difficile que tu as résolu. »</small></div>
          </div>
          <div className="float-card float-offre">
            <span className="float-icon">🎉</span>
            <div><b>Offre reçue</b><small>Kaliop Labs · Dev fullstack</small></div>
          </div>
        </div>
      </section>

      <section className="stats-band">
        <div><strong>6</strong><span>étapes suivies par candidature</span></div>
        <div><strong>3</strong><span>taux clés calculés pour toi</span></div>
        <div><strong>8</strong><span>questions d'entretien préparées par l'IA</span></div>
        <div><strong>0 €</strong><span>pour toujours</span></div>
      </section>

      <section id="fonctionnalites" className="section">
        <div className="section-head">
          <span className="eyebrow">Fonctionnalités</span>
          <h2>Tout ce qu'il faut pour décrocher le poste</h2>
          <p>Fini le tableur oublié et les mails perdus : Taguora garde le fil pour toi.</p>
        </div>
        <div className="features">
          {FONCTIONNALITES.map((f) => (
            <article key={f.titre} className="feature">
              <span className="feature-icon">{f.icone}</span>
              <h3>{f.titre}</h3>
              <p>{f.texte}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="ia" className="section ia-section">
        <div className="ia-text">
          <span className="eyebrow">TAGIA · ton coach IA</span>
          <h2>Arrive à l'entretien en ayant déjà répondu aux questions</h2>
          <p>
            TAGIA, l'assistant de Taguora, lit le texte de l'offre et les notes de tes échanges. Il te donne ce que
            l'entreprise recherche, les questions les plus probables avec une trame de réponse,
            les points techniques à réviser… puis il joue le recruteur pour un entretien blanc.
          </p>
          <ul className="checks">
            <li>Préparation complète en un clic</li>
            <li>Simulation question par question, avec retour sur tes réponses</li>
            <li>Adapté à l'étape : RH, technique ou manager</li>
            <li>Une question sur l'appli ? TAGIA répond aussi, sur chaque page</li>
          </ul>
        </div>
        <div className="l-chat" aria-hidden="true">
          <div className="l-chat-head"><TagiaAvatar size={38} /><div><b>TAGIA</b><small>En ligne</small></div></div>
          <div className="bubble bot">Prêt pour ton entretien technique chez <b>Atlas Studio</b> ? Première question : comment sécurises-tu une API REST ?</div>
          <div className="bubble me">J'utilise Spring Security avec des jetons JWT signés, et je valide chaque entrée…</div>
          <div className="bubble bot">Très bien 👍 Pense aussi à parler des rôles et du CORS. On passe à la suite ?</div>
          <div className="typing"><span /><span /><span /></div>
        </div>
      </section>

      <section id="etapes" className="section">
        <div className="section-head">
          <span className="eyebrow">Comment ça marche</span>
          <h2>Trois étapes, zéro prise de tête</h2>
        </div>
        <div className="steps">
          {ETAPES.map((e) => (
            <div key={e.num} className="step">
              <span className="step-num">{e.num}</span>
              <h3>{e.titre}</h3>
              <p>{e.texte}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="final-cta">
        <h2>Ta prochaine offre commence ici.</h2>
        <p>Crée ton espace en moins d'une minute et ajoute ta première candidature.</p>
        <Link to={cta.to} className="btn btn-lg btn-white">{cta.label} →</Link>
      </section>

      <footer className="landing-footer">
        <Logo size={24} />
        <span>© {new Date().getFullYear()} Taguora · Fait avec ❤️ pour les chercheurs d'emploi</span>
      </footer>
    </div>
  )
}
