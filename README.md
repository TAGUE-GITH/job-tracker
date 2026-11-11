<div align="center">

<img src="frontend/public/icon-192.png" alt="Logo Postilyn" width="88" />

# Postilyn

**Le copilote de ta recherche d'alternance, de stage ou d'emploi.**
Suis tes candidatures, trouve des offres, et laisse **ORVYNA**, l'assistant IA, adapter ton CV, écrire tes lettres et t'entraîner aux entretiens.

[![CI](https://github.com/TAGUE-GITH/job-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/TAGUE-GITH/job-tracker/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4-6db33f)
![React 19](https://img.shields.io/badge/React-19-61dafb)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-336791)

### [🌐 Ouvrir l'application](https://taguora.vercel.app) · [👀 Essayer la démo sans inscription](https://taguora.vercel.app/demo)

<sub>Le serveur est hébergé gratuitement : après une pause, le premier chargement peut prendre jusqu'à une minute.</sub>

<img src="docs/captures/tableau-de-bord.png" alt="Tableau de bord de Postilyn" width="900" />

</div>

## Pourquoi Postilyn ?

Chercher une alternance, c'est des dizaines de candidatures, des tableurs oubliés, des relances qu'on ne fait pas et des entretiens qu'on prépare seul. Postilyn réunit tout au même endroit et ajoute une IA qui **connaît l'offre et ton CV** :

- 🎯 **Score de compatibilité CV ↔ offre** avec les compétences manquantes et les mots-clés à ajouter
- 📄 **CV réécrit pour chaque offre**, en PDF d'une page lisible par les logiciels de tri (ATS)
- ✉️ **Lettre de motivation sur mesure** avec un en-tête à la française
- 🎙️ **Entretien blanc à l'oral** : ORVYNA pose les questions à voix haute et note tes réponses
- 🔔 **Relances intelligentes** et **📊 statistiques** qui montrent ce qui marche vraiment

## Captures d'écran

| Fiche candidature et ORVYNA | Vue Kanban |
|---|---|
| <img src="docs/captures/candidature.png" alt="Fiche d'une candidature avec ORVYNA" /> | <img src="docs/captures/kanban.png" alt="Vue Kanban des candidatures" /> |
| **CV généré, compatible ATS (une page)** | **Page d'accueil** |
| <img src="docs/captures/cv-ats.png" alt="CV au format ATS généré par Postilyn" /> | <img src="docs/captures/accueil.png" alt="Page d'accueil de Postilyn" /> |

## Fonctionnalités

### Suivi des candidatures
- **Candidatures** : entreprise, poste, lien et texte de l'offre, CV et lettre utilisés, recherche et filtre par statut
- **Timeline des étapes** (échange RH, tests, entretiens, offre, refus, relance) avec notes ; le statut se met à jour tout seul
- **Vue Kanban** par glisser-déposer (menu « Déplacer vers… » sur téléphone)
- **Relances intelligentes** : candidatures sans nouvelles depuis 7 jours, email de relance rédigé par ORVYNA
- **Tableau de bord** : taux de réponse, d'entretien et d'offre, délai moyen de réponse, candidatures par mois, entonnoir, **taux de réponse par CV et par site d'offres**

### ORVYNA, l'assistant IA
- **Compatibilité CV ↔ offre** : score sur 100, points forts, compétences manquantes, mots-clés ATS
- **CV adapté à l'offre** : titre, profil, compétences de l'offre proches des tiennes, projet le plus pertinent détaillé ; sans inventer d'expérience ni de diplôme ; PDF « ATS » d'une page, rattaché à la candidature et noté
- **Lettre de motivation** modifiable, PDF avec tes coordonnées, le destinataire, le lieu et la date
- **Coach d'entretien** : questions probables et réponses STAR, simulation à l'écrit ou **à l'oral** (synthèse et reconnaissance vocale du navigateur), bilan noté sur 10
- **Bulle d'aide** sur chaque page : ORVYNA répond aux questions sur l'application et sur la recherche d'emploi

### Trouver des offres
- **Recherche en direct** : La bonne alternance (toutes les offres d'alternance en France) et Jooble (stages, CDI, CDD)
- **Favori « Ajouter à Postilyn »** : envoie n'importe quelle offre (Welcome to the Jungle, LinkedIn, Indeed, HelloWork, Apec…) dans le suivi en un clic, en lisant les données schema.org `JobPosting` de la page
- Liens « Voir aussi » vers Welcome to the Jungle, HelloWork et l'Apec avec la même recherche

### Sécurité
- Mots de passe hachés avec **BCrypt**, règle commune serveur et interface (8 caractères minimum, une lettre et un chiffre) avec jauge de force
- **Mot de passe oublié** : lien à usage unique valable 30 minutes envoyé par email (API Brevo) ; seul le hash SHA-256 du jeton est stocké ; la réponse ne révèle jamais si un email est inscrit
- **Blocage après 5 échecs** de connexion pendant 15 minutes
- **Révocation des sessions** : changer ou réinitialiser son mot de passe déconnecte tous les autres appareils (version du jeton JWT vérifiée à chaque requête), bouton « Déconnecter mes autres appareils »

### Confort
- **Démo sans inscription** : chaque visiteur reçoit un compte neuf rempli d'exemples, supprimé automatiquement après 6 heures
- **Application installable (PWA)**, responsive (menu mobile, listes en cartes), mode sombre automatique
- **Mon compte** : coordonnées, mot de passe, export de toutes ses données (JSON), suppression du compte

## Architecture

```mermaid
flowchart LR
    U[Navigateur<br/>React 19 + Vite<br/>PWA] -->|HTTPS + JWT| API[API Spring Boot 4<br/>Java 21]
    V[Vercel<br/>site statique] -.sert.-> U
    API -->|JPA + Flyway| DB[(PostgreSQL<br/>Neon)]
    API -->|API compatible OpenAI| IA[Groq<br/>gpt-oss-120b]
    API --> LBA[La bonne alternance]
    API --> J[Jooble]
    API -->|PDFBox| PDF[CV et lettres PDF]
    GH[GitHub Actions<br/>tests + build] -->|CI verte| R[Render<br/>image Docker]
    R -.héberge.-> API
```

## Stack technique

| Partie | Technologies |
|---|---|
| Frontend | React 19, Vite, React Router, Recharts, react-markdown, Vitest + Testing Library |
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Flyway, Spring Security (JWT), Bean Validation, PDFBox |
| Base de données | PostgreSQL (Docker, Neon en production), H2 en développement et pour les tests |
| IA | Toute API compatible OpenAI : Groq (gratuit, par défaut), OpenRouter, Ollama (local)… |
| DevOps | Docker multi-stage, docker-compose, GitHub Actions (tests, build, images sur GHCR), Render (Blueprint) pour l'API, Vercel pour le site |

## Démarrage rapide avec Docker

```bash
cp .env.example .env        # puis renseigne AI_API_KEY (et change JWT_SECRET)
docker compose up --build
```

L'application est disponible sur http://localhost:3000.

## Développement sans Docker

Prérequis : JDK 21 et Node.js 24.

```bash
# Terminal 1 : backend sur http://localhost:8080 (base H2 enregistrée dans backend/data)
cd backend
./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run

# Terminal 2 : frontend sur http://localhost:5173 (les appels /api sont redirigés vers le backend)
cd frontend
npm install
npm run dev
```

Pour activer l'IA en local, définis `AI_API_KEY` avant de lancer le backend :

```bash
# PowerShell
$env:AI_API_KEY="gsk_..."
# Bash
export AI_API_KEY="gsk_..."
```

### Activer les offres d'emploi

La page **Offres** a besoin d'au moins une de ces API (gratuites) :

- **La bonne alternance** (offres d'alternance de toute la France, dont celles de France Travail) : crée un compte sur https://api.apprentissage.beta.gouv.fr, génère une clé d'API et renseigne `LBA_API_KEY`. Cette API cherche par métier (codes ROME) : Postilyn traduit les mots-clés (développeur, data, réseau, RH, marketing…) en codes ROME, ou tu peux taper directement un code comme `M1805`. Le lieu est un département (`75`) ou une ville (rayon de 30 km).
- **Jooble** (tous contrats : stage, CDI, CDD) : demande une clé gratuite sur https://fr.jooble.org/api/about et renseigne `JOOBLE_API_KEY`. Une clé obtenue sur le site français donne surtout des offres françaises : mets alors `JOOBLE_HOST=fr.jooble.org` (par défaut `jooble.org`, le site international).
- **Adzuna** (`ADZUNA_APP_ID`, `ADZUNA_APP_KEY`) : devenu payant (25 appels d'essai gratuits), le code reste disponible.
- **France Travail** (`FT_CLIENT_ID`, `FT_CLIENT_SECRET`) : l'API « Offres d'emploi v2 » n'est plus en accès libre sur francetravail.io. Le code reste prêt si ton application y a accès.

Avec plusieurs sources, l'application propose un onglet par source. La candidature se fait toujours sur le site d'origine de l'annonce (bouton « Postuler »).

### Obtenir une IA gratuite

- **Groq** (par défaut) : crée une clé sur https://console.groq.com/keys
- **Ollama** (100 % local, sans clé) : installe Ollama, lance `ollama pull llama3.2`, puis définis `AI_BASE_URL=http://localhost:11434/v1`, `AI_MODEL=llama3.2` et `AI_API_KEY=ollama`
- **OpenRouter** : `AI_BASE_URL=https://openrouter.ai/api/v1`, avec un modèle `:free`

## Tests

```bash
cd backend && ./mvnw test        # tests JUnit (API, PDF, migrations)
cd frontend && npm run lint && npm test && npm run build   # oxlint, Vitest + Testing Library, build
```

## Base de données et migrations

Le schéma est versionné avec **Flyway** (`backend/src/main/resources/db/migration`) : `V1` crée les tables, chaque évolution ajoute un nouveau script `V2__...`, `V3__...`. Hibernate ne modifie jamais la base (`ddl-auto=validate`), il vérifie seulement qu'elle correspond aux entités. Une base créée avant Flyway est reprise automatiquement comme version 1 (`baseline-on-migrate`).

## CI/CD (GitHub Actions)

Le workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) s'exécute à chaque push et à chaque pull request sur `main` :

1. **backend** : compilation et tests Maven
2. **frontend** : lint (oxlint) et build Vite
3. **docker** (push sur `main` uniquement) : build des images et publication sur GitHub Container Registry (`ghcr.io/tague-gith/job-tracker-backend` et `ghcr.io/tague-gith/job-tracker-frontend`)

## Déploiement gratuit : Render + Neon

L'infrastructure est décrite dans [render.yaml](render.yaml) :
- **job-tracker-api** : le backend, un service web Docker gratuit ;
- **job-tracker** : le frontend, un site statique gratuit.

Render ne déploie que lorsque la CI GitHub Actions est verte (`autoDeployTrigger: checksPass`).

1. **Base de données Neon** : crée un projet sur https://neon.tech (région Frankfurt), puis récupère la *connection string*, de la forme `postgresql://UTILISATEUR:MOTDEPASSE@HOTE/neondb?sslmode=require`.
2. **Render** : sur https://render.com, connecte-toi avec GitHub, puis *New > Blueprint* et choisis ce dépôt. Render demande alors :
   - `DB_URL` = `jdbc:postgresql://HOTE/neondb?sslmode=require`
   - `DB_USER` = `UTILISATEUR`
   - `DB_PASSWORD` = `MOTDEPASSE`
   - `AI_API_KEY` = ta clé Groq
   - `LBA_API_KEY` (alternance) et `JOOBLE_API_KEY` (stages, CDI, CDD) pour les offres d'emploi
3. Le site est ensuite disponible sur `https://job-tracker.onrender.com` (l'URL exacte s'affiche dans Render).

### Variante : frontend sur Vercel

Le backend reste sur Render, et le frontend est hébergé sur Vercel. Le navigateur appelle directement le backend Render, dont l'adresse est dans [frontend/.env.vercel](frontend/.env.vercel). [frontend/vercel.json](frontend/vercel.json) lance le build avec `--mode vercel` pour lire ce fichier. On n'utilise pas de proxy Vercel : il coupe les requêtes trop longues (erreur 502) pendant le réveil du backend gratuit.

1. Sur https://vercel.com, connecte-toi avec GitHub, puis *Add New > Project* et importe ce dépôt.
2. **Root Directory** : `frontend` (le framework Vite est détecté automatiquement).
3. *Deploy*. Chaque push sur `main` redéploie le site.
4. Sur Render, dans le service backend, définis `CORS_ORIGINS` avec l'URL de production Vercel (par exemple `https://job-tracker-xxx.vercel.app`).

Si l'URL du backend change, mets à jour `VITE_API_HOST` dans `frontend/.env.vercel`.

> Sur l'offre gratuite, le backend s'endort après 15 min d'inactivité : la première requête prend alors environ 1 min. Les CV et lettres sont stockés dans PostgreSQL, car le disque de Render est effacé à chaque déploiement.

## API REST

Toutes les routes, sauf `/api/auth/**`, demandent l'en-tête `Authorization: Bearer <token>`.

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/auth/register` | Inscription |
| POST | `/api/auth/login` | Connexion (renvoie le JWT) |
| POST | `/api/auth/mot-de-passe-oublie` | Envoie un lien de réinitialisation par email (`{ email }`, réponse toujours 204) |
| POST | `/api/auth/reinitialiser` | Nouveau mot de passe avec le jeton reçu (`{ jeton, nouveau }`) |
| POST | `/api/compte/deconnexion-partout` | Déconnecte les autres appareils, renvoie un jeton neuf |
| POST | `/api/auth/demo` | Crée un compte de démonstration rempli d'exemples (supprimé après 6 h) |
| GET | `/api/candidatures?statut=&q=` | Liste filtrée |
| POST / GET / PUT / DELETE | `/api/candidatures[/{id}]` | CRUD des candidatures |
| POST / PUT / DELETE | `/api/candidatures/{id}/etapes[/{etapeId}]` | Étapes de la candidature |
| POST | `/api/candidatures/{id}/ia` | Assistant IA (`{ typeEtape, messages }`) |
| POST | `/api/candidatures/{id}/ia/compatibilite` | Score CV ↔ offre (`{ cvId }`, réponse JSON) |
| POST | `/api/candidatures/{id}/ia/cv` | CV réécrit pour l'offre (`{ cvId }`, réponse JSON) |
| POST | `/api/candidatures/{id}/cv` | Enregistre le CV adapté en PDF ATS et le rattache |
| POST | `/api/candidatures/{id}/ia/relance` | Brouillon d'email de relance |
| POST | `/api/candidatures/{id}/ia/lettre` | Lettre de motivation (`{ cvId, consignes }`) |
| POST | `/api/candidatures/{id}/lettre` | Enregistre la lettre en PDF et la rattache (`{ texte }`) |
| POST | `/api/ia/aide` | ORVYNA, aide sur l'application (`{ messages }`) |
| GET / POST | `/api/documents` | Liste et import (multipart : `fichier`, `type`) |
| GET | `/api/documents/{id}/fichier` | Téléchargement |
| DELETE | `/api/documents/{id}` | Suppression |
| GET | `/api/offres?q=&lieu=&contrat=&source=&page=` | Recherche d'offres d'emploi (La bonne alternance, Jooble, Adzuna, France Travail) |
| GET / PUT | `/api/compte/profil` | Coordonnées (`{ nom, telephone, adresse, ville }`) |
| PUT | `/api/compte/mot-de-passe` | Changer de mot de passe (`{ actuel, nouveau }`) |
| GET | `/api/compte/export` | Export de toutes ses données |
| POST | `/api/compte/suppression` | Suppression du compte (`{ motDePasse }`) |
| GET | `/api/stats` | Statistiques du tableau de bord |

## Modèle de données

```
users
- id : Long, obligatoire
- nom : String, obligatoire
- email : String, obligatoire, unique
- password : String (hash BCrypt), obligatoire

candidature
- id : Long, obligatoire
- nomEntreprise : String, obligatoire
- poste : String, obligatoire
- lienOffre : String, facultatif
- texteOffre : TEXT, facultatif
- dateCandidature : LocalDate, obligatoire
- statut : StatutCandidature, obligatoire
- user_id : FK -> users, obligatoire
- cv_id : FK -> document, facultatif
- lettre_id : FK -> document, facultatif

etape
- id : Long, obligatoire
- type : TypeEtape, obligatoire
- date : LocalDate, obligatoire
- notes : TEXT, facultatif   (questions posées, ressenti… utilisés par l'IA)
- candidature_id : FK -> candidature, obligatoire

document
- id : Long, obligatoire
- nomFichier : String, obligatoire
- type : TypeDocument (CV, LETTRE), obligatoire
- contentType : String, facultatif
- dateAjout : LocalDate, obligatoire
- user_id : FK -> users, obligatoire

document_contenu   (table à part pour ne pas charger les fichiers dans les listes)
- document_id : PK et FK -> document
- donnees : bytea, obligatoire

enum StatutCandidature : EN_ATTENTE, EN_COURS, OFFRE_RECUE, REFUSEE, ABANDONNEE
enum TypeEtape : CANDIDATURE_ENVOYEE, ECHANGE_RH, TEST_TECHNIQUE,
                 ENTRETIEN_TECHNIQUE, ENTRETIEN_MANAGER, OFFRE, REFUS
enum TypeDocument : CV, LETTRE
```

Le statut d'une candidature découle de sa dernière étape (par exemple `ECHANGE_RH` donne `EN_COURS` et `REFUS` donne `REFUSEE`), sauf si elle a été marquée comme abandonnée.
