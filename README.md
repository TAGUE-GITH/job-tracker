# Taguora

**Taguora** est une application pour suivre ses candidatures : l'offre (lien, texte, entreprise), le CV et la lettre utilisés, l'évolution du processus (premier échange RH, entretien technique, offre ou refus), des statistiques visuelles et un **assistant IA gratuit** pour préparer les entretiens.

![CI](https://github.com/TAGUE-GITH/job-tracker/actions/workflows/ci.yml/badge.svg)

## Fonctionnalités

- **Comptes utilisateurs** : inscription et connexion (JWT, mots de passe hachés avec BCrypt)
- **Offres d'emploi** : recherche d'offres en direct (La bonne alternance, Jooble) par mots-clés, lieu et contrat (alternance, stage, CDI, CDD), bouton « Postuler » vers l'annonce d'origine et ajout au suivi en un clic
- **Favori « Ajouter à Taguora »** : glissé dans la barre de favoris, il envoie n'importe quelle offre (Welcome to the Jungle, LinkedIn, Indeed, HelloWork, Apec…) dans ton suivi en un clic. Il lit les données structurées schema.org `JobPosting` de la page (titre, entreprise, lieu, contrat, description), sinon le texte sélectionné ou le contenu principal
- **Candidatures** : entreprise, poste, lien, texte de l'offre, date, CV et lettre de motivation (facultative), recherche et filtre par statut
- **Vue Kanban** : les candidatures en colonnes par statut, à déplacer par glisser-déposer (ou avec le menu « Déplacer vers… » sur téléphone) ; l'étape correspondante est ajoutée automatiquement
- **Relances intelligentes** : le tableau de bord liste les candidatures sans nouvelles depuis 7 jours, TAGIA rédige l'email de relance, et « J'ai relancé » ajoute une étape *Relance* (qui ne change pas le statut)
- **Évolution** : timeline des étapes (candidature envoyée, échange RH, test technique, entretiens, offre, refus) avec des notes. Le statut se met à jour automatiquement.
- **CV et lettres** : import de fichiers (PDF, Word…) réutilisables d'une candidature à l'autre
- **Statistiques malignes** : taux de réponse par version de CV et par site d'offres, délai moyen avant une première réponse
- **Mon compte** : coordonnées (téléphone, adresse, ville) pour l'en-tête des lettres, changement de mot de passe, export de toutes ses données (JSON) et suppression définitive du compte
- **Application installable (PWA)** : « Installer Taguora » depuis Chrome ou Edge (ordinateur et Android) ou « Sur l'écran d'accueil » sur iPhone ; l'interface s'ouvre même hors connexion
- **Tableau de bord** : candidatures par mois, répartition par statut, entonnoir de recrutement, taux de réponse, d'entretien et d'offre
- **TAGIA, l'assistant IA** : une bulle d'aide sur chaque page répond aux questions sur l'application, sur tes candidatures et sur la recherche d'emploi
- **Compatibilité CV ↔ offre (TAGIA)** : TAGIA lit ton CV (PDF ou Word) et l'offre, donne un score sur 100, tes points forts, les compétences qui manquent et les mots-clés à ajouter pour passer les filtres des recruteurs
- **CV adapté à l'offre, compatible ATS (TAGIA)** : TAGIA réécrit ton CV pour l'offre (titre, profil, compétences dans l'ordre de l'offre, en ajoutant celles de l'offre proches des tiennes, missions reformulées avec ses mots-clés, projet le plus pertinent détaillé, atouts, centres d'intérêt) sans inventer d'expérience ni de diplôme ; le PDF tient toujours sur une page (mise en page resserrée automatiquement) ; tout est modifiable, puis un PDF « ATS » (une colonne, vrai texte, rubriques classiques) est créé, rattaché à la candidature et noté
- **Lettre de motivation sur mesure (TAGIA)** : rédigée à partir de ton CV et de l'offre, avec un en-tête à la française (tes coordonnées en haut à gauche, le destinataire à droite, le lieu et la date), modifiable, puis enregistrée en PDF et rattachée à la candidature en un clic
- **Entretien blanc à l'oral (TAGIA)** : TAGIA pose les questions à voix haute, tu réponds au micro (reconnaissance vocale du navigateur, Chrome et Edge), il commente chaque réponse puis donne un bilan noté sur 10
- **Coach d'entretien (TAGIA)** : préparation d'un entretien (questions probables et réponses conseillées, questions à poser, points à réviser) et simulation d'entretien interactive, à partir du texte de l'offre et de tes notes

## Stack technique

| Partie | Technologies |
|---|---|
| Frontend | React 19, Vite, React Router, Recharts, react-markdown, Vitest + Testing Library |
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Flyway, Spring Security (JWT), Bean Validation, PDFBox |
| Base de données | PostgreSQL (Docker, Neon en production), H2 en développement et pour les tests |
| IA | Toute API compatible OpenAI : Groq (gratuit, par défaut), OpenRouter, Ollama (local)… |
| DevOps | Docker multi-stage, docker-compose, GitHub Actions (tests, build, images sur GHCR), Render (Blueprint) |

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

- **La bonne alternance** (offres d'alternance de toute la France, dont celles de France Travail) : crée un compte sur https://api.apprentissage.beta.gouv.fr, génère une clé d'API et renseigne `LBA_API_KEY`. Cette API cherche par métier (codes ROME) : Taguora traduit les mots-clés (développeur, data, réseau, RH, marketing…) en codes ROME, ou tu peux taper directement un code comme `M1805`. Le lieu est un département (`75`) ou une ville (rayon de 30 km).
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
| POST | `/api/ia/aide` | TAGIA, aide sur l'application (`{ messages }`) |
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
