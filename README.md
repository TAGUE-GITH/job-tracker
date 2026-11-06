# Candivo

**Candivo** est une application pour suivre ses candidatures : l'offre (lien, texte, entreprise), le CV et la lettre utilisés, l'évolution du processus (premier échange RH, entretien technique, offre ou refus), des statistiques visuelles et un **assistant IA gratuit** pour préparer les entretiens.

![CI](https://github.com/TAGUE-GITH/job-tracker/actions/workflows/ci.yml/badge.svg)

## Fonctionnalités

- **Comptes utilisateurs** : inscription et connexion (JWT, mots de passe hachés avec BCrypt)
- **Candidatures** : entreprise, poste, lien, texte de l'offre, date, CV et lettre de motivation (facultative), recherche et filtre par statut
- **Évolution** : timeline des étapes (candidature envoyée, échange RH, test technique, entretiens, offre, refus) avec des notes. Le statut se met à jour automatiquement.
- **CV et lettres** : import de fichiers (PDF, Word…) réutilisables d'une candidature à l'autre
- **Tableau de bord** : candidatures par mois, répartition par statut, entonnoir de recrutement, taux de réponse, d'entretien et d'offre
- **Ivo, l'assistant IA** : une bulle d'aide sur chaque page répond aux questions sur l'application, sur tes candidatures et sur la recherche d'emploi
- **Coach d'entretien (Ivo)** : préparation d'un entretien (questions probables et réponses conseillées, questions à poser, points à réviser) et simulation d'entretien interactive, à partir du texte de l'offre et de tes notes

## Stack technique

| Partie | Technologies |
|---|---|
| Frontend | React 19, Vite, React Router, Recharts, react-markdown |
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), Bean Validation |
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

### Obtenir une IA gratuite

- **Groq** (par défaut) : crée une clé sur https://console.groq.com/keys
- **Ollama** (100 % local, sans clé) : installe Ollama, lance `ollama pull llama3.2`, puis définis `AI_BASE_URL=http://localhost:11434/v1`, `AI_MODEL=llama3.2` et `AI_API_KEY=ollama`
- **OpenRouter** : `AI_BASE_URL=https://openrouter.ai/api/v1`, avec un modèle `:free`

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```

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
| POST | `/api/ia/aide` | Ivo, aide sur l'application (`{ messages }`) |
| GET / POST | `/api/documents` | Liste et import (multipart : `fichier`, `type`) |
| GET | `/api/documents/{id}/fichier` | Téléchargement |
| DELETE | `/api/documents/{id}` | Suppression |
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
