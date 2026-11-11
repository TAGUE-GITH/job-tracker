package com.jobtracker.demo;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureRepository;
import com.jobtracker.candidature.StatutCandidature;
import com.jobtracker.document.CvPdf;
import com.jobtracker.document.Document;
import com.jobtracker.document.DocumentService;
import com.jobtracker.document.LettrePdf;
import com.jobtracker.etape.Etape;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.user.AuthDtos.AuthResponse;
import com.jobtracker.user.AuthService;
import com.jobtracker.user.CompteService;
import com.jobtracker.user.User;
import com.jobtracker.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comptes de demonstration : chaque visiteur recoit un compte neuf, rempli de candidatures d'exemple,
 * pour decouvrir l'application sans s'inscrire. Ces comptes sont supprimes automatiquement.
 */
@Service
public class DemoService {

    private static final Logger LOG = LoggerFactory.getLogger(DemoService.class);
    static final int MAX_DEMOS = 100;
    static final int DUREE_HEURES = 6;

    private final UserRepository userRepository;
    private final CandidatureRepository candidatureRepository;
    private final DocumentService documentService;
    private final CompteService compteService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public DemoService(UserRepository userRepository, CandidatureRepository candidatureRepository,
            DocumentService documentService, CompteService compteService, AuthService authService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidatureRepository = candidatureRepository;
        this.documentService = documentService;
        this.compteService = compteService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse creer() {
        if (userRepository.countByEmailEndingWith(AuthService.DOMAINE_DEMO) >= MAX_DEMOS) {
            throw new IllegalArgumentException("Trop de comptes de demonstration ouverts : reessaie dans une heure.");
        }
        String email = "demo-" + UUID.randomUUID().toString().substring(0, 8) + AuthService.DOMAINE_DEMO;
        User user = new User("Alex Martin", email, passwordEncoder.encode(UUID.randomUUID().toString()));
        user.modifierProfil("Alex Martin", "06 00 00 00 00", "1 rue de l'Exemple\n75001 Paris", "Paris");
        user = userRepository.save(user);
        remplir(user);
        return authService.toResponse(user);
    }

    /** Toutes les heures : supprime les comptes de demonstration crees il y a plus de 6 heures. */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void nettoyer() {
        List<User> anciens = userRepository.findByEmailEndingWithAndDateCreationBefore(
                AuthService.DOMAINE_DEMO, LocalDateTime.now().minusHours(DUREE_HEURES));
        anciens.forEach(u -> compteService.effacer(u.getId()));
        if (!anciens.isEmpty()) {
            LOG.info("{} compte(s) de demonstration supprime(s)", anciens.size());
        }
    }

    private void remplir(User user) {
        LocalDate t = LocalDate.now();
        CvPdf.Contact contact = new CvPdf.Contact(user.getNom(), user.getEmail(), user.getTelephone(), user.getAdresse());
        Document cv = documentService.creerCvPdf(user.getId(), "CV Alex Martin.pdf", contact, CV);
        Document lettre = documentService.creerLettrePdf(user.getId(), "Lettre de motivation - Atlas Studio.pdf",
                new LettrePdf.EnTete("Alex Martin\n1 rue de l'Exemple\n75001 Paris\n06 00 00 00 00",
                        "Atlas Studio\nService Recrutement\nParis", "À Paris, le " + t.minusDays(35).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))),
                LETTRE);

        Candidature atlas = candidature(user, "Atlas Studio", "Alternant Développeur Java / Spring Boot",
                "https://www.welcometothejungle.com/fr/companies/atlas-studio/jobs/alternant-java", t.minusDays(35),
                offre("Paris", "Alternance", "Au sein de l'équipe produit, tu développes des API REST en Java 21 et "
                        + "Spring Boot, avec PostgreSQL, Docker et une intégration continue GitHub Actions. Tests "
                        + "automatisés (JUnit), code review et méthodes agiles."));
        atlas.setCv(cv);
        atlas.setLettre(lettre);
        etape(atlas, TypeEtape.ECHANGE_RH, t.minusDays(28), "Appel de 20 min avec la RH : motivation, rythme de "
                + "l'alternance, projet Postulyn apprécié.");
        etape(atlas, TypeEtape.ENTRETIEN_TECHNIQUE, t.minusDays(20), "Questions sur Spring Security, JPA (lazy "
                + "loading), tests JUnit. Exercice : concevoir une API de réservation.");
        etape(atlas, TypeEtape.OFFRE, t.minusDays(6), "Offre reçue ! Début en septembre, 3 jours en entreprise.");

        Candidature nordlys = candidature(user, "Nordlys", "Développeur Full-Stack React / Java (alternance)",
                "https://www.hellowork.com/fr-fr/emplois/nordlys-fullstack.html", t.minusDays(22),
                offre("Lyon", "Alternance", "Tu interviens sur une application SaaS : front React et TypeScript, "
                        + "back Java Spring Boot, déploiement sur le cloud. Autonomie et curiosité appréciées."));
        nordlys.setCv(cv);
        etape(nordlys, TypeEtape.ECHANGE_RH, t.minusDays(15), "Bon échange, ils doivent revenir vers moi sous 10 jours.");
        etape(nordlys, TypeEtape.RELANCE, t.minusDays(8), "Relance envoyée par email.");

        Candidature kaliop = candidature(user, "Kaliop Labs", "Développeur Back-end Java (stage)",
                "https://labonnealternance.apprentissage.beta.gouv.fr/emploi/offres_emploi_lba/kaliop/offre",
                t.minusDays(12), offre("Nantes", "Stage", "Stage de 6 mois : microservices Java, Kafka, Docker."));
        kaliop.setCv(cv);

        Candidature orbital = candidature(user, "Orbital Systems", "Développeur Java (alternance)",
                "https://fr.indeed.com/viewjob?jk=orbital-java", t.minusDays(9),
                offre("Toulouse", "Alternance", "Développement d'outils internes en Java et Spring Boot, base "
                        + "PostgreSQL, tests et documentation."));
        orbital.setCv(cv);
        etape(orbital, TypeEtape.ECHANGE_RH, t.minusDays(5), "Entretien RH en visio, très positif.");
        etape(orbital, TypeEtape.TEST_TECHNIQUE, t.minusDays(1), "Test en ligne : algorithmique et SQL, 1 h.");

        Candidature brightway = candidature(user, "Brightway", "Développeur Java Spring (stage)",
                "https://www.welcometothejungle.com/fr/companies/brightway/jobs/stage-java", t.minusDays(3),
                offre("Paris", "Stage", "Stage en équipe agile : API REST Spring Boot, React, tests automatisés."));
        brightway.setCv(cv);

        Candidature hexa = candidature(user, "Hexa Data", "Data engineer junior (alternance)",
                "https://www.linkedin.com/jobs/view/hexa-data-engineer", t.minusDays(30),
                offre("Lille", "Alternance", "Pipelines de données en Python et SQL, Airflow, cloud AWS."));
        etape(hexa, TypeEtape.REFUS, t.minusDays(18), "Refus : profil plus orienté data recherché.");

        Candidature lumen = candidature(user, "Lumen Health", "Alternant développeur Angular / Java",
                "https://www.welcometothejungle.com/fr/companies/lumen-health/jobs/alternant-angular-java",
                t.minusDays(50), offre("Bordeaux", "Alternance", "Application de santé : Angular et Java Spring."));
        lumen.setCv(cv);
        etape(lumen, TypeEtape.ECHANGE_RH, t.minusDays(44), null);
        etape(lumen, TypeEtape.ENTRETIEN_MANAGER, t.minusDays(38), "Entretien avec le CTO : questions sur Angular.");
        etape(lumen, TypeEtape.REFUS, t.minusDays(33), "Refus : candidat plus expérimenté sur Angular retenu.");

        Candidature pixel = candidature(user, "Pixel Factory", "Développeur web PHP", null, t.minusDays(40),
                offre("Marseille", "CDD", "Maintenance de sites PHP / Symfony."));
        pixel.setStatut(StatutCandidature.ABANDONNEE);

        candidatureRepository.saveAll(List.of(atlas, nordlys, kaliop, orbital, brightway, hexa, lumen, pixel));
    }

    private static Candidature candidature(User user, String entreprise, String poste, String lien, LocalDate date,
            String texte) {
        Candidature c = new Candidature(user);
        c.setNomEntreprise(entreprise);
        c.setPoste(poste);
        c.setLienOffre(lien);
        c.setTexteOffre(texte);
        c.setDateCandidature(date);
        c.ajouterEtape(new Etape(TypeEtape.CANDIDATURE_ENVOYEE, date, null));
        return c;
    }

    private static void etape(Candidature c, TypeEtape type, LocalDate date, String notes) {
        c.ajouterEtape(new Etape(type, date, notes));
    }

    private static String offre(String lieu, String contrat, String description) {
        return "Lieu : " + lieu + "\nContrat : " + contrat + "\n\n" + description;
    }

    private static final String LETTRE = """
            Objet : Candidature au poste d'alternant développeur Java / Spring Boot

            Madame, Monsieur,

            Votre équipe produit développe des API REST en Java et Spring Boot avec une vraie culture de la qualité : \
            c'est exactement l'environnement dans lequel je veux progresser pendant mon alternance.

            J'ai conçu et déployé Postulyn, une application complète de suivi de candidatures : API Spring Boot \
            sécurisée par JWT, base PostgreSQL, interface React, tests automatisés et intégration continue GitHub \
            Actions. Ce projet m'a appris à livrer un produit utilisé, de la conception au déploiement.

            Je serais ravi d'échanger avec vous sur la façon dont je peux contribuer à vos projets.

            Je vous prie d'agréer, Madame, Monsieur, mes salutations distinguées.

            Alex Martin""";

    private static final CvPdf.Cv CV = new CvPdf.Cv(
            "Développeur Java / Spring Boot - Alternance",
            "Développeur en formation orienté back-end, auteur d'une application complète de suivi de candidatures "
                    + "(Java, Spring Boot, React, Docker) déployée en production avec intégration continue. "
                    + "Recherche une alternance pour développer des API robustes en équipe agile.",
            List.of("Java 21", "Spring Boot", "API REST", "Spring Security", "JPA / Hibernate", "PostgreSQL", "React",
                    "Docker", "GitHub Actions", "JUnit"),
            List.of("Autonomie", "Travail en équipe agile", "Curiosité", "Rigueur"),
            List.of(new CvPdf.Experience("Développeur web (stage)", "Agence Web Exemple", "Paris", "Avril - Juin 2025",
                    List.of("Développement de fonctionnalités en PHP et JavaScript pour des sites clients",
                            "Mise en place de tests et correction d'anomalies en équipe"))),
            List.of(new CvPdf.Projet("Postulyn - suivi de candidatures avec IA",
                    "Application web complète pour organiser sa recherche d'alternance.",
                    "Java 21, Spring Boot, PostgreSQL, React, Docker, GitHub Actions",
                    List.of("API REST Spring Boot sécurisée par JWT, migrations Flyway, base PostgreSQL",
                            "Interface React responsive : tableau de bord, Kanban, statistiques",
                            "Assistant IA : analyse CV / offre, lettres et entretiens simulés",
                            "CI/CD GitHub Actions, images Docker, déploiement Render et Vercel"))),
            List.of(new CvPdf.Formation("Bachelor Concepteur Développeur d'Applications", "École Exemple", "2025 - 2027",
                    "Rythme alternance : 3 semaines en entreprise, 1 semaine en formation")),
            List.of("Français : langue maternelle", "Anglais : B2"),
            List.of("Apprend vite", "Sens du produit", "Persévérant"),
            List.of("Football", "Veille technologique"),
            List.of("github.com/exemple"));
}
