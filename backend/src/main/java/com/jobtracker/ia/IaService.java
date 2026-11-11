package com.jobtracker.ia;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureService;
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
 * ORVYNA, l'assistant IA de Postilyn : coach d'entretien et aide sur l'application.
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

    private static final String GUIDE_POSTILYN = """
            Fonctionnement de l'application Postilyn (a utiliser pour repondre aux questions d'aide) :
            - Menu du haut : "Tableau de bord", "Offres", "Candidatures", "CV & lettres", et le bouton \
            "Deconnexion" (sur telephone, ils sont dans le menu a trois traits en haut a droite).
            - Offres : recherche d'offres en direct par mots-cles, lieu (ville ou numero de departement) \
            et contrat. La bonne alternance fournit les offres d'alternance de toute la France (recherche \
            par metier, codes ROME) ; Jooble couvre les stages, CDI et CDD si sa cle est configuree. \
            "Postuler" ouvre l'annonce sur le site d'origine pour candidater ; "Ajouter au suivi" cree \
            automatiquement la candidature avec le texte de l'offre. On ne peut pas envoyer sa candidature \
            directement depuis Postilyn : la candidature se fait toujours sur le site du recruteur.
            - Favori "Ajouter a Postilyn" (en bas des pages Offres et Candidatures) : on le glisse dans la barre \
            de favoris du navigateur (Ctrl+Maj+B pour l'afficher) ; sur la page d'une offre de n'importe quel site, \
            un clic ouvre Postilyn avec la candidature preremplie (entreprise, poste, lien, texte de l'offre).
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
            - Compte : inscription avec nom, email et mot de passe (6 caracteres minimum). La session dure \
            24 h. Page "Mon compte" (clic sur son nom en haut) : changer de mot de passe, exporter ses donnees \
            (fichier JSON), supprimer son compte, "Deconnecter mes autres appareils".
            - Mot de passe oublie : lien "Mot de passe oublie ?" sous le formulaire de connexion ; un email avec un \
            lien valable 30 minutes est envoye. Mot de passe : 8 caracteres minimum avec une lettre et un chiffre ; \
            5 echecs de connexion bloquent le compte 15 minutes.
            - Statistiques du tableau de bord : aussi le taux de reponse par CV et par site d'offres, et le delai \
            moyen avant une premiere reponse.
            - Application installable : dans Chrome ou Edge, icone "Installer" dans la barre d'adresse ; sur \
            iPhone, Partager puis "Sur l'ecran d'accueil".
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
                Tu es ORVYNA, le coach IA de l'application Postilyn, expert en recrutement tech. Tu aides \
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
                Tu es ORVYNA, l'assistant IA de Postilyn, une application de suivi de candidatures. \
                Tu es chaleureux, encourageant et bref (en general moins de 150 mots). Tu reponds \
                aux questions sur l'utilisation de l'application, sur les candidatures de l'utilisateur \
                et, plus largement, sur la recherche d'emploi (CV, lettre, relances, entretiens). \
                Pour preparer un entretien precis, invite l'utilisateur a ouvrir la candidature et a \
                utiliser le coach d'entretien.
                """);
        sb.append(FORMAT).append('\n').append(GUIDE_POSTILYN);

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
     * Reecrit le CV pour cette offre, au format JSON (voir CvPdf.Cv) : titre, profil, competences, experiences,
     * formations, projets, langues, atouts, centres d'interet. Le contenu reste fidele au CV d'origine.
     */
    @Transactional(readOnly = true)
    public IaResponse cvAdapte(Long userId, Long candidatureId, Long cvId) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        String cv = texteCv(userId, c, cvId);
        List<Message> messages = List.of(
                new Message("system", """
                        Tu es un expert en recrutement et en logiciels de tri de CV (ATS). Tu reecris le CV du \
                        candidat pour qu'il corresponde au mieux a l'offre et passe les filtres ATS, en francais.
                        Regles absolues :
                        - n'invente JAMAIS d'experience, d'entreprise, de diplome ni de date ; tu peux reformuler, \
                        reordonner, mettre en avant et reprendre le vocabulaire exact de l'offre ;
                        - competences : AJOUTE les competences demandees par l'offre qui sont proches de celles du \
                        candidat (meme famille, outil voisin, prolongement naturel : ex. Spring Boot -> Spring Data JPA, \
                        JUnit ; React -> TypeScript ; SQL -> PostgreSQL ; Git -> GitHub Actions). N'ajoute PAS celles qui \
                        sont trop eloignees de son profil (autre langage ou domaine sans lien, ex. Java -> SAP ABAP, \
                        developpeur -> comptabilite) ;
                        - titre : intitule proche de celui de l'offre (ex. "Developpeur Java / Spring Boot - Alternance") ;
                        - profil : 3 a 4 phrases percutantes, orientees vers l'offre, sans "je" repete ;
                        - competences : integre TOUTES les competences demandees par l'offre, sauf celles trop \
                        eloignees du profil ; les plus pertinentes pour l'offre en premier, avec les termes exacts de l'offre ;
                        - experiences : garde EXACTEMENT les 2 experiences professionnelles les PLUS pertinentes pour \
                        l'offre (toutes s'il y en a moins de 2) et ignore les autres ; pour chacune, 3 missions qui \
                        reprennent les mots-cles de l'offre ;
                        - projets : garde EXACTEMENT les 2 projets personnels (ou academiques, open source) les PLUS \
                        pertinents pour l'offre (tous s'il y en a moins de 2) et ignore les autres ; detaille chacun : \
                        une phrase de contexte (description), puis 3 points concrets (fonctionnalites realisees, \
                        architecture, outils, resultats) qui reprennent les mots-cles de l'offre, et la liste des \
                        technologies ;
                        - PAS DE DOUBLON : un travail realise pendant une experience professionnelle (meme entreprise, \
                        meme mission, meme client) se decrit UNIQUEMENT dans cette experience, jamais dans les projets ; \
                        les 2 projets sont donc des projets distincts des experiences ; s'il n'y en a aucun, renvoie \
                        "projets": [] ;
                        - experiences et formations : du plus recent au plus ancien, chacune avec sa periode \
                        ("Juin 2026 - Sept. 2026") et son lieu tels qu'ecrits dans le CV ; garde l'orthographe exacte \
                        des noms propres (villes, entreprises, ecoles) ;
                        - le CV doit tenir sur UNE page : sois concis, phrases courtes, garde les formations les plus \
                        utiles pour l'offre ;
                        - missions : verbes d'action, resultats chiffres si le CV les donne ;
                        - atouts : 3 a 5 qualites prouvees par le CV ; centres d'interet : ceux du CV (ou vide) ;
                        - motsClesAjoutes : mots-cles de l'offre que tu as integres ;
                        - competencesAjoutees : uniquement les competences que tu as ajoutees et qui ne figuraient pas \
                        dans le CV d'origine (elles apparaissent aussi dans competencesTechniques ou Transverses).
                        Reponds UNIQUEMENT avec un objet JSON valide :
                        {"titre": "", "profil": "",
                         "competencesTechniques": [""], "competencesTransverses": [""],
                         "experiences": [{"poste": "", "entreprise": "", "lieu": "", "periode": "", "missions": [""]}],
                         "projets": [{"nom": "", "description": "", "points": [""], "technologies": ""}],
                         "formations": [{"diplome": "", "etablissement": "", "periode": "", "details": ""}],
                         "langues": [""], "atouts": [""], "centresInteret": [""], "liens": [""],
                         "motsClesAjoutes": [""], "competencesAjoutees": [""]}
                        """),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV ACTUEL DU CANDIDAT :\n" + cv));
        String reponse = appeler(messages, true);
        int debut = reponse.indexOf('{');
        int fin = reponse.lastIndexOf('}');
        if (debut < 0 || fin <= debut) {
            throw new IaIndisponibleException("L'IA n'a pas renvoye de CV exploitable, reessaie");
        }
        return new IaResponse(reponse.substring(debut, fin + 1));
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
                        - 250 a 350 mots, ton professionnel, sincere et dynamique, sans formules creuses ;
                        - n'ecris NI coordonnees, NI adresse, NI date, NI bloc destinataire : ils sont ajoutes \
                        automatiquement en haut de la lettre ;
                        - structure : ligne "Objet : ...", "Madame, Monsieur,", accroche sur l'entreprise, \
                        2 paragraphes qui relient des experiences REELLES du CV aux besoins de l'offre \
                        (n'invente aucune experience ni diplome), motivation, formule de politesse, signature ;
                        - texte brut uniquement : pas de Markdown, pas de titres, pas de crochets a completer ;
                        - le candidat s'appelle %s.
                        """.formatted(nom)),
                new Message("user", "OFFRE (" + c.getPoste() + " chez " + c.getNomEntreprise() + ") :\n"
                        + texteOffre(c) + "\n\nCV DU CANDIDAT :\n" + cv
                        + (consignes == null || consignes.isBlank() ? "" : "\n\nCONSIGNES DU CANDIDAT : " + consignes)));
        return new IaResponse(appeler(messages, false).strip());
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
