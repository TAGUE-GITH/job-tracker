import { Link } from 'react-router-dom'
import {
  ArrowRight, BarChart3, Briefcase, Check, FileText, GitBranch, Minus, Search, Sparkles, X,
} from 'lucide-react'
import { useAuth } from '../auth.jsx'
import Logo from '../components/Logo.jsx'
import { PiedDePage } from '../components/PagePublique.jsx'
import OrvynaAvatar from '../components/OrvynaAvatar.jsx'
import '../landing.css'

const FONCTIONNALITES = [
  {
    icone: Briefcase,
    titre: 'Toutes vos candidatures centralisées',
    texte: "Lien et texte de l'annonce, entreprise, CV et lettre envoyés : tout est conservé, même si l'annonce disparaît.",
  },
  {
    icone: GitBranch,
    titre: 'Un historique par candidature',
    texte: 'Échange RH, test technique, entretiens, offre ou refus : chaque étape est tracée et le statut se met à jour.',
  },
  {
    icone: BarChart3,
    titre: 'Des indicateurs fiables',
    texte: "Taux de réponse, d'entretien et d'offre, performance par CV et par source : vous savez ce qui fonctionne.",
  },
  {
    icone: Sparkles,
    titre: 'ORVYNA, assistante IA',
    texte: "Analyse de compatibilité, CV adapté à l'offre et compatible ATS, lettre de motivation et simulation d'entretien.",
  },
  {
    icone: FileText,
    titre: 'Documents maîtrisés',
    texte: 'Vos CV et lettres sont rattachés à chaque candidature : vous savez toujours quelle version a été envoyée.',
  },
  {
    icone: Search,
    titre: 'Offres intégrées',
    texte: "Recherchez des offres d'alternance, de stage ou d'emploi et ajoutez-les à votre suivi en un clic.",
  },
]

const ETAPES = [
  { num: '01', titre: 'Ajoutez une offre', texte: "Depuis la recherche intégrée, le favori du navigateur ou à la main, en quelques secondes." },
  { num: '02', titre: 'Suivez chaque étape', texte: 'Consignez les échanges au fil de l’eau : le tableau de bord se met à jour automatiquement.' },
  { num: '03', titre: 'Préparez-vous avec ORVYNA', texte: "CV adapté, lettre, questions probables et entretien blanc, à partir de l'annonce." },
]

// Photos libres de droits (licence Unsplash), servies et redimensionnees par Unsplash
const photo = (id, largeur = 900) => `https://images.unsplash.com/photo-${id}?w=${largeur}&q=75&auto=format&fit=crop`
const PHOTOS = {
  decourage: '1758874572670-63042c5448c6',
  postIt: '1758876019043-cedb0b6f56ea',
  sereine: '1765648763848-665267cf9929',
  souriante: '1688646583123-16844c80e78a',
  hero: '1765648684555-de2d0f6af467',
}

const DIFFICULTES = [
  'Un tableur à mettre à jour à la main, vite oublié',
  'Des post-it et des dizaines d’onglets ouverts',
  'Des relances oubliées, des réponses qui se perdent',
  'Le même CV envoyé partout, filtré par les logiciels de tri',
  'Des entretiens préparés à la dernière minute',
]

// Comparatif : oui, partiel, non
const COMPARATIF = [
  ['Toutes les candidatures et leur historique au même endroit', 'partiel', 'oui'],
  ['Rappel des candidatures à relancer', 'non', 'oui'],
  ['Offres de plusieurs pays et domaines, ajoutées en un clic', 'non', 'oui'],
  ['CV adapté à chaque offre et compatible ATS', 'non', 'oui'],
  ['Lettre de motivation rédigée à partir de l’offre', 'non', 'oui'],
  ['Simulation d’entretien, à l’écrit ou à l’oral', 'non', 'oui'],
  ['Statistiques : taux de réponse, CV et sources qui marchent', 'partiel', 'oui'],
  ['Accessible sur ordinateur et téléphone', 'partiel', 'oui'],
]

function Cellule({ valeur }) {
  if (valeur === 'oui') return <span className="cmp-oui"><Check size={18} /> Oui</span>
  if (valeur === 'partiel') return <span className="cmp-partiel"><Minus size={18} /> À la main</span>
  return <span className="cmp-non"><X size={18} /> Non</span>
}

export default function Landing() {
  const { user } = useAuth()
  const cta = user
    ? { to: '/', label: 'Accéder à mon espace' }
    : { to: '/login?mode=inscription', label: 'Créer un compte gratuit' }

  return (
    <div className="landing">
      <header className="landing-nav">
        <Link to="/accueil" className="landing-logo"><Logo size={30} /></Link>
        <nav>
          <a href="#fonctionnalites">Fonctionnalités</a>
          <a href="#ia">Assistante IA</a>
          <a href="#comparatif">Pourquoi Postulyn</a>
          <a href="#etapes">Fonctionnement</a>
        </nav>
        <div className="landing-nav-actions">
          {!user && <Link to="/demo" className="btn ghost lien-demo">Démo</Link>}
          {!user && <Link to="/login" className="btn ghost">Connexion</Link>}
          <Link to={cta.to} className="btn primary">{user ? 'Mon espace' : 'Commencer'}</Link>
        </div>
      </header>

      <section className="hero">
        <div className="hero-text">
          <span className="eyebrow">Plateforme de suivi de candidatures</span>
          <h1>Pilotez votre recherche d'emploi avec méthode.</h1>
          <p className="hero-sub">
            Postulyn centralise vos offres, suit chaque étape du recrutement, mesure vos résultats et vous
            prépare aux entretiens grâce à ORVYNA, une assistante IA qui connaît l'annonce.
          </p>
          <div className="hero-actions">
            <Link to={cta.to} className="btn primary btn-lg">{cta.label} <ArrowRight size={18} /></Link>
            {!user && <Link to="/demo" className="btn ghost btn-lg">Voir la démonstration</Link>}
          </div>
          <ul className="hero-points">
            <li><Check size={16} /> Gratuit</li>
            <li><Check size={16} /> Sans carte bancaire</li>
            <li><Check size={16} /> Données exportables</li>
          </ul>
        </div>

        <div className="hero-visual" aria-hidden="true">
          <img src={`${photo(PHOTOS.hero, 1000)}&crop=faces&h=750`} alt="" className="hero-photo" />
          <div className="mock">
            <div className="mock-side">
              <span className="mock-logo" />
              {[1, 2, 3, 4, 5].map((i) => <span key={i} className={i === 1 ? 'actif' : ''} />)}
            </div>
            <div className="mock-body">
              <div className="mock-kpis">
                <div><small>Candidatures</small><strong>24</strong></div>
                <div><small>Taux de réponse</small><strong>42 %</strong></div>
                <div><small>Entretiens</small><strong>7</strong></div>
              </div>
              <div className="mock-chart">
                {[35, 55, 40, 70, 60, 90, 75].map((h, i) => <span key={i} style={{ height: `${h}%` }} />)}
              </div>
              <div className="mock-rows">
                <div><b>Atlas Studio</b><em className="tag tag-blue">Entretien technique</em></div>
                <div><b>Nordlys</b><em className="tag tag-amber">Échange RH</em></div>
                <div><b>Kaliop Labs</b><em className="tag tag-green">Offre reçue</em></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section probleme">
        <div className="probleme-photos" aria-hidden="true">
          <img src={photo(PHOTOS.decourage, 700)} alt="" loading="lazy" className="photo-a" />
          <img src={photo(PHOTOS.postIt, 700)} alt="" loading="lazy" className="photo-b" />
        </div>
        <div className="probleme-texte">
          <span className="eyebrow">Le constat</span>
          <h2>Chercher un emploi ne devrait pas devenir un second emploi</h2>
          <p>
            Des dizaines de candidatures, autant d'annonces, de versions de CV et d'échanges : sans outil adapté,
            on s'épuise à tout garder en tête et les opportunités passent.
          </p>
          <ul className="liste-difficultes">
            {DIFFICULTES.map((d) => <li key={d}><X size={16} /> {d}</li>)}
          </ul>
        </div>
      </section>

      <section id="fonctionnalites" className="section">
        <div className="section-head">
          <span className="eyebrow">Fonctionnalités</span>
          <h2>Un outil complet, de l'annonce à l'offre</h2>
          <p>Remplacez le tableur et les e-mails éparpillés par un suivi structuré.</p>
        </div>
        <div className="features">
          {FONCTIONNALITES.map(({ icone: Icone, titre, texte }) => (
            <article key={titre} className="feature">
              <span className="feature-icon"><Icone size={20} strokeWidth={1.8} /></span>
              <h3>{titre}</h3>
              <p>{texte}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="comparatif" className="section comparatif">
        <div className="section-head">
          <span className="eyebrow">Pourquoi Postulyn</span>
          <h2>Ce qui change par rapport à un tableur, des post-it et vos e-mails</h2>
        </div>
        <div className="comparatif-grille">
          <div className="comparatif-photo">
            <img src={photo(PHOTOS.sereine, 800)} alt="Jeune femme souriante à son bureau devant son ordinateur" loading="lazy" />
            <div className="comparatif-legende">
              <strong>Une recherche organisée, sereine</strong>
              <span>Chaque offre, chaque échange et chaque document au même endroit, avec une assistante pour vous préparer.</span>
            </div>
          </div>
          <div className="card table-comparatif">
            <table>
              <thead>
                <tr><th>Fonctionnalité</th><th>Tableur et outils dispersés</th><th className="col-postulyn">Postulyn</th></tr>
              </thead>
              <tbody>
                {COMPARATIF.map(([libelle, autres, nous]) => (
                  <tr key={libelle}>
                    <td>{libelle}</td>
                    <td><Cellule valeur={autres} /></td>
                    <td className="col-postulyn"><Cellule valeur={nous} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section id="ia" className="section ia-section">
        <div className="ia-text">
          <span className="eyebrow">ORVYNA · assistante IA</span>
          <h2>Des candidatures mieux ciblées, des entretiens mieux préparés</h2>
          <p>
            ORVYNA lit le texte de l'offre et votre CV. Elle évalue votre compatibilité, rédige un CV adapté et
            compatible ATS, propose une lettre de motivation, puis joue le rôle du recruteur pour un entretien blanc,
            à l'écrit ou à l'oral.
          </p>
          <ul className="checks">
            <li><Check size={18} /> Score de compatibilité et compétences manquantes</li>
            <li><Check size={18} /> CV d'une page optimisé pour les logiciels de tri</li>
            <li><Check size={18} /> Simulation d'entretien avec bilan détaillé</li>
            <li><Check size={18} /> Réponses à vos questions sur l'application</li>
          </ul>
        </div>
        <div className="l-chat" aria-hidden="true">
          <div className="l-chat-head"><OrvynaAvatar size={34} /><div><b>ORVYNA</b><small>Assistante IA</small></div></div>
          <div className="bubble bot">Entretien technique chez <b>Atlas Studio</b>. Première question : comment sécurisez-vous une API REST ?</div>
          <div className="bubble me">Avec Spring Security et des jetons JWT signés, et une validation de chaque entrée.</div>
          <div className="bubble bot">Bonne base. Mentionnez aussi la gestion des rôles et la configuration CORS. Question suivante ?</div>
        </div>
      </section>

      <section id="etapes" className="section">
        <div className="section-head">
          <span className="eyebrow">Fonctionnement</span>
          <h2>Opérationnel en trois étapes</h2>
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
        <img src={photo(PHOTOS.souriante, 300)} alt="" loading="lazy" className="final-photo" aria-hidden="true" />
        <div>
          <h2>Prêt à structurer votre recherche ?</h2>
          <p>Créez votre espace en moins d'une minute.</p>
        </div>
        <Link to={cta.to} className="btn btn-lg btn-white">{cta.label} <ArrowRight size={18} /></Link>
      </section>

      <PiedDePage />
    </div>
  )
}
