import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import PagePublique from '../components/PagePublique.jsx'

const MISE_A_JOUR_CONDITIONS = '11 novembre 2026'

// Conditions generales d'utilisation et politique de confidentialite de Postulyn
export default function Conditions() {
  const { hash } = useLocation()
  useEffect(() => {
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView()
  }, [hash])

  return (
    <PagePublique titre="Conditions d'utilisation"
      sousTitre={`Conditions générales d'utilisation et politique de confidentialité · dernière mise à jour : ${MISE_A_JOUR_CONDITIONS}`}>
      <article className="card texte-legal">
        <nav className="sommaire">
          <a href="#objet">1. Objet</a>
          <a href="#compte">2. Compte</a>
          <a href="#service">3. Service</a>
          <a href="#ia">4. Assistante IA</a>
          <a href="#offres">5. Offres d'emploi</a>
          <a href="#engagements">6. Engagements</a>
          <a href="#responsabilite">7. Responsabilité</a>
          <a href="#confidentialite">8. Données personnelles</a>
          <a href="#modifications">9. Modifications</a>
        </nav>

        <h2 id="objet">1. Objet</h2>
        <p>Postulyn est une application web gratuite d'aide à la recherche d'emploi : suivi des candidatures,
          recherche d'offres, gestion des CV et lettres, statistiques et assistante d'intelligence artificielle
          ORVYNA. Les présentes conditions encadrent l'utilisation du service. Créer un compte implique de les
          accepter.</p>

        <h2 id="compte">2. Compte utilisateur</h2>
        <ul>
          <li>L'inscription demande un nom, une adresse email valide et un mot de passe personnel.</li>
          <li>Tu es responsable de la confidentialité de ton mot de passe et des actions faites depuis ton compte.
            En cas de doute, change-le et utilise « Déconnecter mes autres appareils » dans les paramètres.</li>
          <li>Tu peux supprimer ton compte à tout moment depuis « Paramètres du compte » : toutes tes données sont
            alors effacées définitivement.</li>
          <li>Un compte utilisé de manière abusive (spam, tentative d'intrusion, contenu illicite) peut être suspendu
            ou supprimé.</li>
        </ul>

        <h2 id="service">3. Le service</h2>
        <p>Postulyn est fourni gratuitement, « en l'état ». Nous faisons de notre mieux pour qu'il soit disponible et
          fiable, sans garantie de disponibilité permanente : le serveur peut notamment mettre jusqu'à une minute à
          démarrer après une période d'inactivité. Des fonctionnalités peuvent évoluer ou être retirées. Pense à
          exporter régulièrement tes données (« Exporter mes données »).</p>

        <h2 id="ia">4. Assistante IA ORVYNA</h2>
        <p>ORVYNA génère des contenus (analyse de compatibilité, CV adapté, lettre de motivation, préparation et
          simulation d'entretien, réponses aux questions) à l'aide d'un modèle d'intelligence artificielle. Ces contenus
          peuvent contenir des erreurs : relis-les et corrige-les avant toute utilisation, tu restes seul responsable
          des documents que tu envoies. ORVYNA n'invente pas d'expérience ni de diplôme ; n'ajoute à ton CV que des
          compétences que tu maîtrises réellement.</p>

        <h2 id="offres">5. Offres d'emploi de sites tiers</h2>
        <p>Les offres affichées proviennent de services tiers (La bonne alternance, Jooble, Himalayas, Arbeitnow…).
          Postulyn n'est pas l'auteur de ces annonces et ne garantit ni leur exactitude ni leur disponibilité. La
          candidature se fait toujours sur le site d'origine, selon ses propres conditions. Méfie-toi de toute offre
          qui te demanderait de l'argent ou des informations bancaires.</p>

        <h2 id="engagements">6. Tes engagements</h2>
        <ul>
          <li>Utiliser Postulyn pour ta propre recherche d'emploi, dans le respect des lois.</li>
          <li>N'importer que des documents dont tu as le droit de disposer, sans contenu illicite ni malveillant.</li>
          <li>Ne pas tenter de perturber le service, d'accéder aux données d'autres utilisateurs ni d'automatiser
            massivement des requêtes.</li>
        </ul>

        <h2 id="responsabilite">7. Responsabilité</h2>
        <p>Postulyn est un outil d'organisation : il ne garantit pas l'obtention d'un entretien ou d'un emploi. Sa
          responsabilité ne saurait être engagée pour une perte de données, une indisponibilité du service, le contenu
          d'offres tierces ou l'usage des contenus générés par l'IA, dans les limites permises par la loi.</p>

        <h2 id="confidentialite">8. Données personnelles et confidentialité</h2>
        <p><strong>Données collectées :</strong> nom, email, mot de passe (stocké chiffré, jamais en clair),
          coordonnées facultatives (téléphone, adresse, ville), candidatures, notes d'étapes, documents importés
          (CV, lettres), messages envoyés au support et avis.</p>
        <p><strong>Finalités :</strong> fournir le service, sécuriser ton compte (blocage après plusieurs échecs de
          connexion, emails de réinitialisation), répondre à tes demandes et améliorer l'application. Tes données ne
          sont ni vendues ni utilisées à des fins publicitaires.</p>
        <p><strong>Sous-traitants :</strong> hébergement de l'application (Render, serveurs à Francfort, et Vercel),
          base de données (Neon), envoi d'emails (Brevo) et traitement par l'IA (Groq) : quand tu utilises ORVYNA, le
          texte de l'offre, de ton CV et de tes notes utiles à la demande est transmis au fournisseur d'IA pour générer
          la réponse. Les offres sont recherchées auprès des sites tiers cités plus haut, sans leur transmettre tes
          données personnelles.</p>
        <p><strong>Conservation :</strong> tes données sont conservées tant que ton compte existe et supprimées
          lorsque tu le supprimes. Les comptes de démonstration et leurs données fictives sont effacés automatiquement.</p>
        <p><strong>Tes droits (RGPD) :</strong> accès et portabilité (« Exporter mes données »), rectification
          (« Paramètres du compte »), effacement (« Supprimer mon compte »), opposition et limitation. Pour toute
          demande, écris-nous via la page <Link to="/contact">Aide et contact</Link>. Tu peux aussi saisir la CNIL
          (cnil.fr).</p>
        <p><strong>Stockage local :</strong> Postulyn n'utilise pas de cookies publicitaires ni de traceurs. Le
          navigateur conserve seulement ton jeton de connexion et quelques préférences (par exemple tes derniers filtres
          d'offres) pour le bon fonctionnement de l'application.</p>

        <h2 id="modifications">9. Modifications et contact</h2>
        <p>Ces conditions peuvent évoluer ; la date de mise à jour figure en haut de la page. En cas de changement
          important, tu en seras informé dans l'application. Pour toute question : <Link to="/contact">Aide et contact</Link>.
          Les présentes conditions sont soumises au droit français.</p>
      </article>
    </PagePublique>
  )
}
