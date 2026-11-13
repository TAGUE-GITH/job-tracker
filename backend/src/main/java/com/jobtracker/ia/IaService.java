package com.jobtracker.ia;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.document.CvPdf;
import com.jobtracker.document.DocumentService;
import com.jobtracker.etape.Etape;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.ia.IaDtos.IaRequest;
import com.jobtracker.ia.IaDtos.IaResponse;
import com.jobtracker.ia.IaDtos.Message;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * ORVYNA, l'assistant IA de Postulyn : coach d'entretien et aide sur l'application.
 * Utilise n'importe quelle API compatible OpenAI
 * (Groq et OpenRouter ont des offres gratuites, Ollama tourne en local sans cle).
 */
@Service
public class IaService {

    private static final int MAX_TEXTE_OFFRE = 8000;
    private static final int MAX_CANDIDATURES_CONTEXTE = 40;

    private static final String FORMAT = """
            Reponds en francais et tutoie l'utilisateur, de facon concrete et structuree. \
            Mise en forme Markdown simple : titres (##, ###), listes a puces et numerotees, gras. \
            N'utilise jamais de tableaux ni de balises HTML (pas de <br>) : la reponse s'affiche \
            dans une colonne etroite.
            """;

    private static final String GUIDE_POSTULYN = """
            Fonctionnement de l'application Postulyn (a utiliser pour repondre aux questions d'aide) :
            - Barre laterale a gauche : "Tableau de bord", "Offres d'emploi", "Candidatures", "CV et lettres", \
            "Parametres du compte", "Donner mon avis", "Aide et contact", "Page d'accueil" (et "Administration" pour \
            les administrateurs) ; en bas, le nom de l'utilisateur et le bouton de deconnexion. Sur telephone, la barre \
            s'ouvre avec le bouton a trois traits en haut a droite.
            - Offres d'emploi, onglet "Rechercher" : une grande barre (metier ou mot-cle, lieu, pays), puis des \
            pastilles de contrat (Tous, CDI, CDD, Alternance, Stage), la date de publication (24 h, 3, 7 ou 30 jours), \
            le domaine et le tri (plus pertinentes ou plus recentes). La recherche comprend le sens : abreviations \
            (dev, compta, RH), fautes, synonymes et traductions ("dev full stack" trouve aussi "Fullstack developer" \
            et "Developpeur web") ; elle indique ce qu'elle a compris et les intitules equivalents inclus (cliquables \
            pour affiner) ; seules les offres pertinentes sont gardees. Les resultats s'affichent en liste a gauche et \
            l'offre choisie en detail a droite (en plein ecran sur telephone) avec "Postuler" (site du recruteur), \
            "Adapter mon CV" et "Suivre" (ajoute la candidature avec le texte de l'offre). Les filtres sont retenus \
            pour la visite suivante. Si rien ne correspond, des intitules proches et "Retirer tous les filtres" sont \
            proposes. On ne peut pas envoyer sa candidature directement depuis Postulyn : elle se fait toujours sur \
            le site du recruteur. Postulyn ne mentionne pas d'ou viennent les offres.
            - Offres d'emploi, onglet "Selon mon CV" : on choisit ou importe son CV, "Analyser ce CV" ; ORVYNA en tire \
            un profil (metiers, competences, niveau) et Postulyn cherche les offres correspondantes, classees avec un \
            pourcentage de correspondance et les mots en commun avec le CV ; filtres Pays, Contrat, Lieu, tri par \
            correspondance ou par date ; "Reanalyser" apres avoir change de CV. Sur chaque offre, "Adapter mon CV" ajoute \
            l'offre au suivi et ouvre directement l'onglet "CV adapte".
            - CV adapte : 4 modeles compatibles ATS (Classique, Moderne, Elegant, Compact) a choisir avant \
            "Enregistrer le CV en PDF", et la case "Ajouter ma photo" (photo a enregistrer dans Parametres du compte \
            > "Photo pour mes CV", JPEG ou PNG, 3 Mo maximum ; facultative, deconseillee pour certains pays).
            - Onglet "Message" (carte ORVYNA de la candidature) : email de candidature (objet + texte, bouton "Ouvrir \
            dans ma messagerie"), message LinkedIn et note d'invitation LinkedIn, modifiables et copiables.
            - Favori "Ajouter a Postulyn" (en bas des pages Offres et Candidatures) : on le glisse dans la barre \
            de favoris du navigateur (Ctrl+Maj+B pour l'afficher) ; sur la page d'une offre de n'importe quel site, \
            un clic ouvre Postulyn avec la candidature preremplie (entreprise, poste, lien, texte de l'offre).
            - Ajouter une candidature : menu Candidatures > bouton "Nouvelle candidature". Champs : entreprise \
            et poste (obligatoires), lien de l'offre, texte de l'offre (a coller : il sert a l'IA), date, \
            CV et lettre de motivation (facultative) choisis parmi les documents importes.
            - La liste des candidatures se filtre par statut et se recherche par entreprise ou poste.
            - Statuts : En attente, En cours, Offre recue, Refusee, Abandonnee. Le statut se met a jour \
            tout seul selon la derniere etape ; "Abandonnee" se coche dans le formulaire de modification.
            - Etapes (page d'une candidature, formulaire "Ajouter une etape") : Candidature envoyee (creee \
            automatiquement), Echange RH, Test technique, Entretien technique, Entretien manager, Offre, \
            Refus, Relance envoyee. Chaque etape a une date et des notes (questions posees, ressenti) que l'IA reutilise.
            - Tableau de bord : carte "A relancer" = candidatures en attente ou en cours sans nouvelle etape \
            depuis 7 jours ; "Rediger la relance" fait ecrire l'email par ORVYNA, "J'ai relance" ajoute une etape \
            Relance (qui ne change pas le statut).
            - Page Candidatures : bascule "Liste" / "Kanban". En Kanban, glisser une carte dans une colonne ajoute \
            l'etape correspondante (En cours : choix de l'etape, Offre, Refus) ou marque la candidature abandonnee ; \
            sur telephone, le menu "Deplacer vers..." de chaque carte fait la meme chose.
            - CV & lettres : importer des fichiers (PDF, Word..., 10 Mo max) de type CV ou Lettre, les \
            ouvrir, les supprimer. Un document supprime est detache des candidatures.
            - Tableau de bord : total, candidatures en cours, taux de reponse (part des candidatures ayant \
            eu au moins une reponse), taux d'entretien, taux d'offre, graphique par mois, repartition par \
            statut et entonnoir de recrutement.
            - Onglet "CV adapte" de la carte "ORVYNA - CV & lettre" : ORVYNA reecrit le CV pour l'offre (titre, profil, \
            competences dans l'ordre de l'offre, y compris celles de l'offre proches du profil, missions avec ses mots-cles, projet le plus pertinent detaille, atouts, centres d'interet) sans inventer d'experience ni de diplome, sur une seule page ; tout est modifiable, puis "Enregistrer le CV en PDF" cree un CV compatible ATS (une colonne, \
            vrai texte), le rattache a la candidature et affiche sa note ATS estimee.
            - Carte "ORVYNA - CV & lettre" (page d'une candidature) : onglet "Compatibilite" = score sur 100 du \
            CV face a l'offre, points forts, competences manquantes, mots-cles a ajouter ; onglet "Lettre de \
            motivation" = lettre redigee depuis le CV et l'offre, avec un en-tete modifiable (coordonnees du \
            candidat en haut a gauche, prises dans "Mon compte" > "Mes coordonnees", destinataire a droite, \
            lieu et date), puis "Enregistrer en PDF" la \
            rattache a la candidature. Il faut le texte de l'offre et un CV importe (PDF ou Word .docx).
            - Coach d'entretien (sur la page d'une candidature) : choisir l'etape, "Preparer" genere une \
            preparation complete, "Simuler" lance un entretien blanc question par question. \
            L'interrupteur "Mode oral" (Chrome et Edge) fait lire les questions a voix haute et permet de repondre \
            au micro (bouton micro) ; apres deux reponses, "Terminer et obtenir mon bilan" donne une note sur 10, \
            les points forts et les axes d'amelioration.
            - ORVYNA (la bulle en bas a droite) repond aux questions sur l'application et sur la recherche d'emploi.
            - Compte : inscription avec nom, email, mot de passe et acceptation des conditions d'utilisation \
            (page /conditions, lien en bas de la page d'accueil). La session dure 24 h. "Parametres du compte" : \
            coordonnees, mot de passe, email de test, exporter ses donnees (fichier JSON), supprimer son compte, \
            "Deconnecter mes autres appareils".
            - "Donner mon avis" : une note de 1 a 5 etoiles et un commentaire, lus par l'equipe Postulyn.
            - "Aide et contact" (page /contact, accessible meme sans compte) : formulaire pour ecrire a l'equipe \
            (probleme, question, suggestion) ; un accuse de reception arrive par email.
            - Mot de passe oublie : lien "Mot de passe oublie ?" sous le formulaire de connexion ; un email avec un \
            lien valable 30 minutes est envoye. Mot de passe : 8 caracteres minimum avec une lettre et un chiffre ; \
            5 echecs de connexion bloquent le compte 15 minutes.
            - Statistiques du tableau de bord : aussi le taux de reponse par CV et par site d'offres, et le delai \
            moyen avant une premiere reponse.
            - Application installable : dans Chrome ou Edge, icone "Installer" dans la barre d'adresse ; sur \
            iPhone, Partager puis "Sur l'ecran d'accueil".
            Depannage (propose ces solutions, une ou deux a la fois, avant de renvoyer vers le support) :
            - Premier chargement tres lent ou message "le serveur se reveille" : le serveur gratuit s'endort apres \
            15 minutes, il faut patienter jusqu'a une minute puis recharger la page.
            - Page blanche, bouton sans effet ou affichage ancien : recharger avec Ctrl + F5 (Cmd + Maj + R sur Mac), \
            vider le cache du site, ou essayer en navigation privee ; si l'application est installee, la fermer et la rouvrir.
            - Deconnexion inattendue ("Session expiree") : la session dure 24 h, ou un changement de mot de passe a \
            deconnecte les autres appareils : se reconnecter.
            - Connexion refusee : verifier l'email et le mot de passe (majuscules), utiliser "Mot de passe oublie ?" ; \
            apres 5 echecs, attendre 15 minutes.
            - Email de reinitialisation non recu : regarder les spams, attendre quelques minutes, verifier l'adresse ; \
            au plus 3 demandes par heure.
            - Aucune offre : elargir les mots-cles, choisir "Tous les contrats" ou "Tous les domaines", essayer un \
            intitule proche (ceux proposes sous la recherche), retirer les filtres ou changer de pays ; l'alternance \
            n'existe qu'en France.
            - Import de document refuse : 10 Mo maximum ; pour l'IA, preferer un PDF avec du vrai texte (pas un scan) \
            ou un Word .docx.
            - Les fonctions IA repondent "service indisponible" : reessayer dans une minute (limite du fournisseur d'IA).
            - Micro du mode oral inactif : utiliser Chrome ou Edge et autoriser le micro dans le navigateur.
            Quand renvoyer vers le support : si le probleme persiste apres ces solutions, si c'est un bug, des donnees \
            perdues ou incorrectes, un compte bloque ou pirate, une demande sur les donnees personnelles, ou toute \
            question que tu ne peux pas resoudre, reponds : "Le plus simple est d'ecrire a l'equipe Postulyn : \
            [Contacter le support](/contact). Decris ce que tu faisais et le message d'erreur exact." N'invente \
            jamais de solution technique ni de fonctionnalite.
            Si une fonctionnalite demandee n'existe pas, dis-le simplement sans l'inventer.
            """;

    private final CandidatureService candidatureService;
    private final DocumentService documentService;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public IaService(CandidatureService candidatureService, DocumentService documentService,
            @Value("${app.ia.base-url}") String baseUrl,
            @Value("${app.ia.api-key}") String apiKey,
            @Value("${app.ia.model}") String model) {
        this.candidatureService = candidatureService;
        this.documentService = documentService;
        // Delais : 10 s pour se connecter, 90 s pour la reponse (un CV complet peut etre long a generer)
        SimpleClientHttpRequestFactory delais = new SimpleClientHttpRequestFactory();
        delais.setConnectTimeout(Duration.ofSeconds(10));
        delais.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(delais).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    record ChatRequest(String model, List<Message> messages, double temperature) {
    }

    /** Variante qui force une reponse JSON (mode "json_object" des API compatibles OpenAI). */
    record ChatRequestJson(String model, List<Message> messages, double temperature,
            Map<String, String> response_format) {
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    @Transactional(readOnly = true)
    public IaResponse assister(Long userId, Long candidatureId, IaRequest req) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        TypeEtape etape = req.typeEtape() != null ? req.typeEtape() : TypeEtape.ENTRETIEN_TECHNIQUE;

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", promptSysteme(c, etape)));
        if (req.messages() == null || req.messages().isEmpty()) {
            messages.add(new Message("user", """
                    Prepare-moi pour cette etape. Donne :
                    1. Un resume en 3 points de ce que l'entreprise recherche.
                    2. Les 8 questions les plus probables, chacune avec une proposition de reponse \
                    structuree (methode STAR pour les questions comportementales).
                    3. 3 questions pertinentes que je peux poser au recruteur.
                    4. Les points techniques a reviser en priorite."""));
        } else {
            messages.addAll(req.messages());
        }
        return new IaResponse(appeler(messages));
    }

    private String promptSysteme(Candidature c, TypeEtape etape) {
        StringBuilder sb = new StringBuilder("""
                Tu es ORVYNA, le coach IA de l'application Postulyn, expert en recrutement tech. Tu aides \
                un candidat a preparer ses entretiens. Pour chaque question, ecris la question en gras \
                puis la reponse conseillee en liste a puces. \
                Si le candidat te demande de simuler l'entretien, pose une seule question a la fois \
                puis donne un retour constructif sur sa reponse.
                """);
        sb.append(FORMAT);
        sb.append("\nEntreprise : ").append(c.getNomEntreprise());
        sb.append("\nPoste : ").append(c.getPoste());
        sb.append("\nEtape a preparer : ").append(etape.name());
        if (c.getTexteOffre() != null) {
            String texte = c.getTexteOffre();
            if (texte.length() > MAX_TEXTE_OFFRE) {
                texte = texte.substring(0, MAX_TEXTE_OFFRE);
            }
            sb.append("\n\nTexte de l'offre :\n").append(texte);
        }
        List<Etape> avecNotes = c.getEtapes().stream().filter(e -> e.getNotes() != null).toList();
        if (!avecNotes.isEmpty()) {
            sb.append("\n\nHistorique et notes du candidat :");
            avecNotes.forEach(e -> sb.append("\n- ").append(e.getDate()).append(" ")
                    .append(e.getType().name()).append(" : ").append(e.getNotes()));
        }
        return sb.toString();
    }

    /** ORVYNA repond aux questions sur l'application et sur les candidatures de l'utilisateur. */
    @Transactional(readOnly = true)
    public IaResponse aide(Long userId, List<Message> conversation) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", promptAide(userId)));
        messages.addAll(conversation);
        return new IaResponse(appeler(messages));
    }

    private String promptAide(Long userId) {
        StringBuilder sb = new StringBuilder("""
                Tu es ORVYNA, l'assistant IA de Postulyn, une application de suivi de candidatures. \
                Tu es chaleureux, encourageant et bref (en general moins de 150 mots). Tu reponds \
                aux questions sur l'utilisation de l'application, sur les candidatures de l'utilisateur \
                et, plus largement, sur la recherche d'emploi (CV, lettre, relances, entretiens). \
                Pour preparer un entretien precis, invite l'utilisateur a ouvrir la candidature et a \
                utiliser le coach d'entretien.
                """);
        sb.append(FORMAT).append('\n').append(GUIDE_POSTULYN);

        List<Candidature> candidatures = candidatureService.toutes(userId);
        sb.append("\nCandidatures de l'utilisateur (").append(candidatures.size())
                .append(" au total, les plus recentes d'abord) :");
        if (candidatures.isEmpty()) {
            sb.append("\n- aucune pour le moment");
        }
        candidatures.stream().limit(MAX_CANDIDATURES_CONTEXTE).forEach(c -> sb.append("\n- ")
                .append(c.getDateCandidature()).append(" | ").append(c.getNomEntreprise())
                .append(" | ").append(c.getPoste()).append(" | statut ").append(c.getStatut().name()));
        return sb.toString();
    }

    /**
     * Compare le CV a l'offre. Reponse JSON (texte) :
     * {score, resume, pointsForts[], competencesManquantes[], motsCles[], conseils[]}.
     */
    @Transactional(readOnly = true)
    public IaResponse compatibilite(Long userId, Long candidatureId, Long cvId) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        String cv = texteCv(userId, c, cvId);
        List<Message> messages = List.of(
                new Message("system", """
                        Tu es un recruteur tech experimente et un expert des logiciels de tri de CV (ATS). \
                        Tu compares objectivement un CV a une offre d'emploi, en francais, en tutoyant le candidat. \
                        Reponds UNIQUEMENT avec un objet JSON valide, sans texte autour, de la forme :
                        {"score": entier de 0 a 100 (adequation reelle du CV a l'offre, sois exigeant),
                         "resume": "2 phrases sur l'adequation globale",
                         "pointsForts": ["atout du CV qui correspond a l'offre", ...] (3 a 5),
                         "competencesManquantes": ["competence ou experience demandee absente du CV", ...] (0 a 6),
                         "motsCles": ["mot-cle de l'offre a ajouter au CV s'il est vrai pour le candidat", ...] (3 a 8),
                         "conseils": ["action concrete pour adapter le CV a cette offre", ...] (3 a 5)}
                        """),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV DU CANDIDAT :\n" + cv));
        String reponse = appeler(messages, true);
        int debut = reponse.indexOf('{');
        int fin = reponse.lastIndexOf('}');
        if (debut < 0 || fin <= debut) {
            throw new IaIndisponibleException("L'IA n'a pas renvoye d'analyse exploitable, reessaie");
        }
        return new IaResponse(reponse.substring(debut, fin + 1));
    }

    /**
     * Adapte le CV a cette offre, en deux temps : l'IA recopie d'abord fidelement le CV importe (JSON), puis propose
     * des ameliorations (missions reformulees et detaillees, competences et atouts de l'offre a ajouter). La fusion
     * est faite par CvAdaptation : rien du CV importe n'est retire ni change (postes, entreprises, dates, formations,
     * rubriques). Reponse : le CV adapte en JSON, avec les mots-cles et competences ajoutes.
     */
    @Transactional(readOnly = true)
    public IaResponse cvAdapte(Long userId, Long candidatureId, Long cvId) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        Long id = cvId != null ? cvId : c.getCv() != null ? c.getCv().getId() : null;
        if (id == null) {
            throw new IllegalArgumentException("Choisis un CV (ou importe-le dans CV & lettres)");
        }
        // Toujours repartir du CV importe, jamais d'un CV deja adapte (sinon il s'appauvrit a chaque passage)
        String cv = documentService.texte(userId, documentService.cvImporte(userId, id));

        String extraction = json(appeler(List.of(
                new Message("system", """
                        Tu recopies un CV en JSON, FIDELEMENT et EN ENTIER, sans rien resumer, reformuler, \
                        corriger ni retirer. Chaque experience, projet, formation, competence, langue, qualite, \
                        centre d'interet et lien du CV doit apparaitre, avec son texte d'origine. Regles :
                        - experiences : TOUTES les experiences professionnelles (stages, emplois, alternances, \
                        missions, jobs), dans l'ordre du CV, avec poste, entreprise, lieu, periode tels qu'ecrits et \
                        chaque mission ou tache dans "missions" ;
                        - projets : TOUS les projets (personnels, academiques, open source) ;
                        - formations : TOUTES les formations (diplomes, ecoles, baccalaureat, certifications \
                        presentees comme formations), avec diplome, etablissement, lieu (ville), periode et details ;
                        - competencesTechniques : outils, langages, logiciels, savoir-faire ; competencesTransverses : \
                        savoir-etre et competences humaines listees comme competences ; atouts : qualites ou atouts \
                        listes dans le CV ;
                        - autresRubriques : toute autre rubrique du CV (certifications, benevolat, associations, \
                        activites, prix, publications, references...), avec son titre exact et ses elements ;
                        - liens : une adresse entre parentheses apres un texte comme "Voir en ligne", "Demo" ou \
                        "GitHub" est le lien cache derriere ce texte ; mets-la dans le champ "lien" du projet ou de \
                        l'experience concernee (adresse complete, sans le texte "Voir en ligne") ; les liens \
                        personnels (LinkedIn, GitHub, portfolio) vont dans "liens" ;
                        - champ absent du CV : "" ou [].
                        Reponds UNIQUEMENT avec un objet JSON valide :
                        {"titre": "", "profil": "",
                         "competencesTechniques": [""], "competencesTransverses": [""],
                         "experiences": [{"poste": "", "entreprise": "", "lieu": "", "periode": "", "missions": [""], "lien": ""}],
                         "projets": [{"nom": "", "description": "", "points": [""], "technologies": "", "lien": ""}],
                         "formations": [{"diplome": "", "etablissement": "", "lieu": "", "periode": "", "details": ""}],
                         "langues": [""], "atouts": [""], "centresInteret": [""], "liens": [""],
                         "autresRubriques": [{"titre": "", "elements": [""]}]}
                        """),
                new Message("user", "CV DU CANDIDAT :\n" + cv)), true), "L'IA n'a pas pu lire ce CV, reessaie");
        CvPdf.Cv origine;
        try {
            origine = CvAdaptation.lireExtraction(extraction);
        } catch (RuntimeException e) {
            throw new IaIndisponibleException("L'IA n'a pas pu lire ce CV, reessaie");
        }

        String ameliorations = json(appeler(List.of(
                new Message("system", """
                        Tu es un expert en recrutement et en logiciels de tri de CV (ATS). Voici le CV du candidat \
                        en JSON (recopie fidele de son CV) et une offre. Tu AMELIORES la redaction pour cette offre, \
                        en francais, SANS RIEN SUPPRIMER NI INVENTER : les postes, entreprises, lieux, dates, \
                        diplomes et ecoles ne changent pas et ne doivent pas etre renvoyes. "index" = position de \
                        l'element dans sa liste du CV, a partir de 0. Reponds UNIQUEMENT avec un objet JSON valide :
                        {"titre": "intitule proche de celui de l'offre et coherent avec le profil",
                         "profil": "3 a 4 phrases percutantes orientees vers l'offre, fondees sur le CV, sans je repete",
                         "experiences": [{"index": 0, "missions": [""]}],
                         "projets": [{"index": 0, "description": "", "points": [""]}],
                         "formations": [{"index": 0, "details": ""}],
                         "autresRubriques": [{"index": 0, "elements": [""]}],
                         "competencesTechniquesAjoutees": [""], "competencesTransversesAjoutees": [""],
                         "atoutsAjoutes": [""], "motsClesAjoutes": [""]}
                        Regles :
                        - experiences : une entree pour CHAQUE experience du CV ; reformule et DETAILLE chacune de \
                        ses missions, dans le meme ordre et en gardant leur sens (au moins autant de missions que \
                        dans le CV, 3 a 5) : "verbe d'action + quoi + avec quels outils ou methodes + pour quel \
                        resultat", une ligne et demie maximum, en reprenant le vocabulaire de l'offre quand il \
                        decrit ce que le candidat a reellement fait ; aucune mission sans rapport avec l'experience, \
                        aucun chiffre absent du CV ;
                        - projets : une entree pour CHAQUE projet : une phrase de contexte et 2 a 4 points concrets \
                        (fonctionnalites, architecture, outils, resultats) fideles au projet ;
                        - formations : une entree pour CHAQUE formation ; "details" = la ligne existante amelioree \
                        ou une specialisation / des modules utiles pour l'offre SEULEMENT si le CV les donne, sinon "" ;
                        - autresRubriques : une entree pour CHAQUE rubrique, chaque element reformule, sans en retirer ;
                        - competencesTechniquesAjoutees : competences demandees par l'offre, ABSENTES du CV, mais \
                        proches de celles du candidat (meme famille, outil voisin : Spring Boot -> Spring Data JPA, \
                        React -> TypeScript, SQL -> PostgreSQL) ; jamais de competence trop eloignee de son profil ;
                        - competencesTransversesAjoutees et atoutsAjoutes : savoir-etre et qualites que l'offre \
                        demande, ABSENTS du CV, et credibles au vu de son parcours ; formules avec les mots de l'offre ;
                        - motsClesAjoutes : mots-cles de l'offre que tu as integres.
                        """),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV DU CANDIDAT (JSON) :\n" + extraction)), true),
                "L'IA n'a pas renvoye de CV exploitable, reessaie");
        CvAdaptation.Ameliorations a;
        try {
            a = CvAdaptation.lireAmeliorations(ameliorations);
        } catch (RuntimeException e) {
            // Ameliorations illisibles : on renvoie le CV importe tel quel plutot que de perdre son contenu
            a = new CvAdaptation.Ameliorations(null, null, null, null, null, null, null, null, null, null);
        }
        return new IaResponse(CvAdaptation.versJson(CvAdaptation.fusionner(origine, a)));
    }

    /** Redige une lettre de motivation a partir du CV et de l'offre (texte brut, pret a enregistrer). */
    @Transactional(readOnly = true)
    public IaResponse lettre(Long userId, Long candidatureId, Long cvId, String consignes) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        String cv = texteCv(userId, c, cvId);
        String nom = c.getUser().getNom();
        List<Message> messages = List.of(
                new Message("system", """
                        Tu es un expert en recrutement qui redige des lettres de motivation percutantes en francais. \
                        Redige une lettre de motivation pour le candidat, a partir de son CV et de l'offre. Regles :
                        - 250 a 350 mots, ton professionnel, sincere et naturel, comme ecrit par le candidat \
                        lui-meme : phrases simples et fluides, sans formules creuses ni cliches ("vivement \
                        interesse", "je me permets", "dynamique et motive"), sans exageration ;
                        - n'ecris NI coordonnees, NI adresse, NI date, NI bloc destinataire : ils sont ajoutes \
                        automatiquement en haut de la lettre ;
                        - structure : ligne "Objet : ...", "Madame, Monsieur,", accroche sur l'entreprise, \
                        2 paragraphes qui relient des experiences REELLES du CV aux besoins de l'offre \
                        (n'invente aucune experience ni diplome), motivation et demande d'entretien, formule de \
                        politesse, puis "Cordialement," et enfin le prenom et le nom du candidat seuls sur la \
                        derniere ligne ; chaque paragraphe sur une seule ligne, separe du suivant par une ligne vide ;
                        - texte brut uniquement : pas de Markdown, pas de titres, pas de crochets a completer ;
                        - le candidat s'appelle %s.
                        """.formatted(nom)),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV DU CANDIDAT :\n" + cv
                        + (consignes == null || consignes.isBlank() ? "" : "\n\nCONSIGNES DU CANDIDAT : " + consignes)));
        return new IaResponse(appeler(messages, false).strip());
    }

    /**
     * Lit un CV et en extrait le profil du candidat (JSON) : sert a chercher et classer les offres qui lui
     * correspondent. domaine : un des codes de Catalogue.Domaine.
     */
    public IaResponse profilCv(Long userId, Long cvId) {
        String cv = documentService.texte(userId, cvId);
        String domaines = java.util.Arrays.stream(com.jobtracker.offre.Catalogue.Domaine.values())
                .map(d -> d.name() + " (" + d.libelle() + ")").collect(java.util.stream.Collectors.joining(", "));
        List<Message> messages = List.of(
                new Message("system", """
                        Tu es un conseiller en recrutement. Tu lis le CV d'un candidat et tu en extrais son profil \
                        pour lui trouver des offres d'emploi. Reponds UNIQUEMENT avec un objet JSON valide :
                        {"titre": "intitule de poste vise, court (ex. Developpeur Java)",
                         "resume": "2 phrases sur le profil, en tutoyant le candidat",
                         "metiers": ["2 ou 3 intitules de poste courts a chercher dans les offres, du plus au moins proche"],
                         "competences": ["8 a 15 competences cles, techniques et metier, telles qu'ecrites dans les offres"],
                         "motsCles": ["5 a 10 mots-cles supplementaires (outils, secteurs, langues)"],
                         "domaine": "UN code parmi : %s",
                         "niveau": "Etudiant, Junior, Confirme ou Senior",
                         "contrats": ["contrats coherents avec le profil parmi ALTERNANCE, STAGE, CDI, CDD"]}
                        N'invente rien qui ne soit pas dans le CV.
                        """.formatted(domaines)),
                new Message("user", "CV DU CANDIDAT :\n" + (cv.length() > 12000 ? cv.substring(0, 12000) : cv)));
        return new IaResponse(json(appeler(messages, true), "L'IA n'a pas pu lire ce CV, reessaie"));
    }

    /** Vrai si une cle d'IA est configuree. */
    public boolean disponible() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Comprend une recherche d'offres (JSON) : intitule corrige, sa traduction anglaise, les intitules
     * equivalents, les competences typiques du poste, et le contrat ou le lieu s'ils sont dans la phrase.
     */
    public IaResponse comprendreRecherche(String requete, String nomPays) {
        String domaines = java.util.Arrays.stream(com.jobtracker.offre.Catalogue.Domaine.values())
                .map(Enum::name).collect(java.util.stream.Collectors.joining(", "));
        List<Message> messages = List.of(
                new Message("system", """
                        Tu es le moteur de recherche d'un site d'emploi. Tu analyses ce que le candidat tape pour \
                        retrouver les offres qui correspondent vraiment a son intention, meme si les recruteurs \
                        emploient d'autres mots. Corrige les fautes et les abreviations (dev -> developpeur, \
                        compta -> comptabilite, RH -> ressources humaines). Pays de la recherche : %s.
                        Reponds UNIQUEMENT avec un objet JSON valide :
                        {"intitule": "intitule de poste clair en francais, sans contrat ni lieu (ex. Developpeur full stack)",
                         "intituleEn": "le meme intitule en anglais (ex. Full stack developer)",
                         "synonymes": ["4 a 6 intitules equivalents utilises dans les offres, francais et anglais, \
                        du plus proche au moins proche"],
                         "competences": ["3 a 6 competences ou outils typiques de ce poste, pour reconnaitre les offres pertinentes"],
                         "domaine": "un code parmi %s, ou vide",
                         "contrat": "ALTERNANCE, STAGE, CDI ou CDD si la phrase le precise, sinon vide",
                         "lieu": "ville ou region si la phrase en cite une, sinon vide"}
                        """.formatted(nomPays, domaines)),
                new Message("user", requete));
        return new IaResponse(json(appeler(messages, true), "Recherche non comprise"));
    }

    /** Extrait l'objet JSON d'une reponse de l'IA. */
    private static String json(String reponse, String erreur) {
        int debut = reponse.indexOf('{');
        int fin = reponse.lastIndexOf('}');
        if (debut < 0 || fin <= debut) {
            throw new IaIndisponibleException(erreur);
        }
        return reponse.substring(debut, fin + 1);
    }

    /**
     * Messages prets a envoyer au recruteur (JSON) : un email de candidature (objet + corps), un message LinkedIn
     * court et une note de connexion LinkedIn de moins de 300 caracteres.
     */
    @Transactional(readOnly = true)
    public IaResponse messageRecruteur(Long userId, Long candidatureId, Long cvId, String consignes) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        String cv = texteCv(userId, c, cvId);
        List<Message> messages = List.of(
                new Message("system", """
                        Tu rediges, en francais, les messages qu'un candidat envoie a un recruteur pour presenter \
                        sa candidature. Ton poli, courtois et naturel, comme ecrit par le candidat lui-meme : \
                        vouvoiement, phrases simples et fluides, sans formules creuses ni cliches ("vivement \
                        interesse", "je me permets", "dynamique et motive"), sans exageration. Relie 1 ou 2 \
                        experiences REELLES du CV aux besoins de l'offre ; n'invente rien. Texte brut, sans Markdown, \
                        sans emoji ni crochets a completer. Si le nom du recruteur est connu (consignes), utilise \
                        "Bonjour Madame X," ou "Bonjour Monsieur X," ; sinon "Bonjour Madame, Monsieur,". \
                        Separe les paragraphes par une ligne vide (\\n\\n dans le JSON).
                        Le candidat s'appelle %s%s.
                        Reponds UNIQUEMENT avec un objet JSON valide :
                        {"objet": "objet de l'email : Candidature - intitule exact du poste - prenom et nom (moins de 90 caracteres)",
                         "email": "email de 130 a 190 mots, structure en paragraphes separes par une ligne vide : \
                        1) la salutation seule ; 2) le poste vise et comment le candidat a connu l'offre, en une ou \
                        deux phrases ; 3) ce qu'il apporte : 1 ou 2 experiences reelles liees aux missions de l'offre ; \
                        4) pourquoi cette entreprise l'interesse, en une phrase sincere ; 5) le CV et la lettre de \
                        motivation en piece jointe et sa disponibilite pour un entretien ; 6) un remerciement poli ; \
                        puis 'Cordialement,' et, sur les lignes suivantes, prenom et nom, telephone et email s'ils sont connus",
                         "linkedin": "message LinkedIn de 70 a 100 mots, en 3 courts paragraphes separes par une \
                        ligne vide (salutation et poste vise ; ce que le candidat apporte ; proposition d'echange), \
                        puis 'Bien cordialement,' et le prenom et nom",
                         "noteConnexion": "note d'invitation LinkedIn polie de moins de 280 caracteres : salutation, \
                        poste vise, une raison d'echanger, remerciement"}
                        """.formatted(c.getUser().getNom(), coordonnees(c.getUser()))),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV DU CANDIDAT :\n" + cv
                        + (consignes == null || consignes.isBlank() ? "" : "\n\nCONSIGNES DU CANDIDAT : " + consignes)));
        return new IaResponse(json(appeler(messages, true), "L'IA n'a pas renvoye de message exploitable, reessaie"));
    }

    /** ", telephone : ..., email : ..." pour la signature des messages, ou vide. */
    private static String coordonnees(com.jobtracker.user.User u) {
        StringBuilder sb = new StringBuilder();
        if (u.getTelephone() != null && !u.getTelephone().isBlank()) {
            sb.append(", telephone : ").append(u.getTelephone().strip());
        }
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            sb.append(", email : ").append(u.getEmail().strip());
        }
        return sb.toString();
    }

    /** Brouillon d'email de relance pour une candidature restee sans reponse. */
    @Transactional(readOnly = true)
    public IaResponse relance(Long userId, Long candidatureId) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        Etape derniere = c.getEtapes().stream()
                .max(Comparator.comparing(Etape::getDate))
                .orElse(null);
        StringBuilder contexte = new StringBuilder()
                .append("Entreprise : ").append(c.getNomEntreprise())
                .append("\nPoste : ").append(c.getPoste())
                .append("\nCandidature envoyee le : ").append(c.getDateCandidature());
        if (derniere != null) {
            contexte.append("\nDerniere etape : ").append(derniere.getType().name())
                    .append(" le ").append(derniere.getDate())
                    .append(" (il y a ").append(ChronoUnit.DAYS.between(derniere.getDate(), LocalDate.now()))
                    .append(" jours)");
            if (derniere.getNotes() != null) {
                contexte.append("\nNotes : ").append(derniere.getNotes());
            }
        }
        long relances = c.getEtapes().stream().filter(e -> e.getType() == TypeEtape.RELANCE).count();
        contexte.append("\nRelances deja envoyees : ").append(relances);

        List<Message> messages = List.of(
                new Message("system", """
                        Tu rediges des emails de relance de candidature en francais, courts (80 a 130 mots), \
                        polis, confiants et jamais insistants. Commence par une ligne "Objet : ...", puis \
                        "Bonjour,", rappelle la candidature et le poste, redis en une phrase ce que le candidat \
                        apporte, demande ou en est le processus, propose un echange, formule de politesse, \
                        signature "%s". Adapte le ton a la derniere etape (apres un entretien, remercie pour \
                        l'echange). Texte brut, sans Markdown ni crochets a completer.
                        """.formatted(c.getUser().getNom())),
                new Message("user", contexte.toString()));
        return new IaResponse(appeler(messages, false).strip());
    }

    private String texteOffre(Candidature c) {
        String texte = c.getTexteOffre();
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException(
                    "Ajoute d'abord le texte de l'offre a cette candidature (bouton Modifier)");
        }
        return texte.length() > MAX_TEXTE_OFFRE ? texte.substring(0, MAX_TEXTE_OFFRE) : texte;
    }

    private String texteCv(Long userId, Candidature c, Long cvId) {
        Long id = cvId != null ? cvId : c.getCv() != null ? c.getCv().getId() : null;
        if (id == null) {
            throw new IllegalArgumentException("Choisis un CV (ou importe-le dans CV & lettres)");
        }
        return documentService.texte(userId, id);
    }

    private String appeler(List<Message> messages) {
        return appeler(messages, false);
    }

    private String appeler(List<Message> messages, boolean json) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IaIndisponibleException(
                    "IA non configuree : definis la variable AI_API_KEY (cle gratuite sur https://console.groq.com)");
        }
        try {
            Object corps = json
                    ? new ChatRequestJson(model, messages, 0.3, Map.of("type", "json_object"))
                    : new ChatRequest(model, messages, 0.7);
            ChatResponse res = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(corps)
                    .retrieve()
                    .body(ChatResponse.class);
            if (res == null || res.choices() == null || res.choices().isEmpty()) {
                throw new IaIndisponibleException("Reponse vide du service d'IA");
            }
            return res.choices().get(0).message().content();
        } catch (RestClientResponseException e) {
            throw new IaIndisponibleException(messageErreur(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new IaIndisponibleException("ORVYNA met trop de temps a repondre. Reessaie dans un instant.");
        } catch (RestClientException e) {
            throw new IaIndisponibleException("Le service d'IA ne repond pas : " + e.getMessage());
        }
    }

    /** Message comprehensible pour les erreurs les plus courantes de l'API d'IA. */
    static String messageErreur(int statut) {
        return switch (statut) {
            case 429 -> "ORVYNA est tres sollicite en ce moment (limite de l'IA gratuite atteinte). "
                    + "Reessaie dans une minute.";
            case 401, 403 -> "La cle de l'IA est refusee : verifie la variable AI_API_KEY sur le serveur.";
            case 404 -> "Le modele d'IA configure n'existe plus : verifie la variable AI_MODEL sur le serveur.";
            case 413 -> "Le texte envoye a ORVYNA est trop long : raccourcis le texte de l'offre ou ton CV.";
            default -> statut >= 500
                    ? "Le service d'IA rencontre un probleme de son cote. Reessaie dans quelques minutes."
                    : "Le service d'IA a refuse la demande (erreur " + statut + ").";
        };
    }
}
