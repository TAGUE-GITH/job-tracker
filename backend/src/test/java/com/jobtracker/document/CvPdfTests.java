package com.jobtracker.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class CvPdfTests {

    static final CvPdf.Cv CV = new CvPdf.Cv(
            "Développeur Java / Spring Boot - Alternance",
            "Développeur en formation passionné par le backend, auteur d'une application complète de suivi de "
                    + "candidatures (Spring Boot, React, Docker) déployée en production avec intégration continue.",
            List.of("Java 21", "Spring Boot", "API REST", "React", "PostgreSQL", "Docker", "GitHub Actions"),
            List.of("Autonomie", "Travail en équipe agile", "Curiosité"),
            List.of(new CvPdf.Experience("Développeur web (stage)", "Atlas Studio", "Paris", "2025",
                    List.of("Développement d'API REST en Java avec Spring Boot",
                            "Mise en place de tests automatisés et d'une CI GitHub Actions"))),
            List.of(new CvPdf.Projet("Postulyn", "Application de suivi de candidatures avec assistant IA.",
                    "Java, Spring Boot, React, Docker, PostgreSQL",
                    List.of("API REST Spring Boot securisee par JWT, base PostgreSQL",
                            "Interface React responsive avec tableau de bord et Kanban",
                            "CI/CD GitHub Actions, images Docker, deploiement Render et Vercel"))),
            List.of(new CvPdf.Formation("Bachelor Développeur d'applications", "École du Numérique", "2024 - 2027",
                    null)),
            List.of("Français : natif", "Anglais : B2"),
            List.of("Rigoureux", "Force de proposition"),
            List.of("Football", "Veille technologique"),
            List.of("github.com/TAGUE-GITH"));

    @Test
    void cvLisibleParUnAts() throws Exception {
        byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck Tague", "franck@exemple.fr", "06 12 34 56 78",
                "12 rue des Lilas\n75011 Paris"), CV);
        String texte = TexteDocument.extraire(pdf, "cv.pdf", "application/pdf").replace("\r", "").replaceAll(" +", " ");
        // Texte reel, dans l'ordre de lecture attendu : formation en tete (modele classique), puis experiences
        int nom = texte.indexOf("FRANCK TAGUE");
        int formation = texte.indexOf("FORMATION");
        int experience = texte.indexOf("EXPÉRIENCE PROFESSIONNELLE");
        int infos = texte.indexOf("COMPÉTENCES ET INFORMATIONS");
        assertTrue(nom >= 0 && nom < formation && formation < experience && experience < infos, texte);
        assertTrue(texte.contains("Spring Boot"), texte);
        assertTrue(texte.contains("06 12 34 56 78"), texte);
        assertTrue(texte.contains("Centres d'intérêt : Football"), texte);
        // Entreprise et lieu, puis poste et dates, sur la meme ligne (lieu et dates calés à droite)
        assertTrue(texte.contains("ATLAS STUDIO Paris"), texte);
        assertTrue(texte.contains("Développeur web (stage) 2025"), texte);
        assertTrue(texte.contains("Bachelor Développeur d'applications 2024 - 2027"), texte);
        // Chaque mission sur sa propre ligne, precedee de sa puce
        assertTrue(texte.contains("\n• Développement d'API REST en Java avec Spring Boot"), texte);
        assertTrue(texte.contains("\n• Mise en place de tests automatisés"), texte);

        // Apercu visuel pour verification manuelle (target/cv-apercu.png)
        try (var doc = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 70), "png", new File("target/cv-apercu.png"));
        }
    }

    /** Format demande : 2 experiences pro et 2 projets perso detailles, sur une seule page. */
    @Test
    void deuxExperiencesEtDeuxProjetsTiennentSurUnePage() throws Exception {
        CvPdf.Cv cv = new CvPdf.Cv(
                "Développeur Python / Anvil - Stage 6 mois",
                "Étudiant en ingénierie logicielle, spécialisé en développement Python et intégration d'APIs REST. "
                        + "Expérience confirmée dans la création de sites vitrines et d'interfaces modernes. Maîtrise "
                        + "des architectures orientées services (SOA) et des environnements lowcode.",
                List.of("Python", "Flask", "Django REST Framework", "API REST", "Anvil (lowcode)", "Tryton",
                        "Middleware", "Git", "GitHub Actions", "Docker", "CSS", "UX design", "PostgreSQL", "SQLite"),
                List.of("Analyse fonctionnelle", "Documentation technique et fonctionnelle", "Tests automatisés (pytest)",
                        "Méthodes agiles Scrum/Kanban", "Intégration continue"),
                List.of(new CvPdf.Experience("Développeur fullstack (stage)", "DITS Group", "Livry-sur-Seine",
                                "Juin 2026 - Sept. 2026",
                                List.of("Conception d'un site vitrine client avec API REST sécurisée (JWT) en Python/Flask et React",
                                        "Intégration continue via GitHub Actions et déploiement sur Render et Vercel",
                                        "Rédaction de la documentation fonctionnelle et technique pour les équipes commerciales")),
                        new CvPdf.Experience("Développeur fullstack (stage)", "Yamo Group Sarl", "Yaoundé",
                                "Févr. 2025 - Juil. 2025",
                                List.of("Développement d'une API Flask de recherche documentaire intégrée au middleware interne",
                                        "Mise en place de tests automatisés (pytest) et conteneurisation Docker",
                                        "Documentation technique et guide d'utilisation pour les équipes internes"))),
                List.of(new CvPdf.Projet("ShoeManager - interface de gestion (Django)",
                                "Application web de gestion de magasin de chaussures, avec API REST et interface utilisateur.",
                                "Python, Django REST Framework, React, Tailwind CSS, PostgreSQL, Docker",
                                List.of("API Django REST documentée (OpenAPI) et gestion des rôles utilisateurs",
                                        "Interface responsive en React et Tailwind CSS orientée expérience utilisateur",
                                        "Tests unitaires pytest et conteneurisation Docker")),
                        new CvPdf.Projet("Postulyn - suivi de candidatures avec IA",
                                "Application complète de suivi de candidatures déployée en production.",
                                "Java, Spring Boot, React, PostgreSQL, Docker, GitHub Actions",
                                List.of("API REST sécurisée par JWT, migrations Flyway et base PostgreSQL",
                                        "Intégration d'APIs d'offres d'emploi et d'un assistant IA",
                                        "CI/CD GitHub Actions, déploiement Render et Vercel"))),
                List.of(new CvPdf.Formation("Cycle Expert en architecture et développement logiciel",
                                "3IL Ingénieurs, Limoges", "Sept. 2025 - présent",
                                "Spécialisation en architectures SOA, développement fullstack et intégration d'APIs"),
                        new CvPdf.Formation("Diplôme d'ingénieur en génie informatique et télécommunications",
                                "École nationale supérieure polytechnique de Douala", "2020 - 2025", null)),
                List.of("Français (natif)", "Anglais (B1)", "Allemand (débutant)"),
                List.of("Rigueur et sens de l'analyse", "Autonomie", "Esprit d'équipe", "Orientation UX"),
                List.of("Enseignement des mathématiques et de l'informatique", "Football", "Tennis de table"),
                List.of("https://linkedin.com/in/tague-franck", "https://github.com/TAGUE-GITH",
                        "https://portfoliofranck-tague.vercel.app"));
        byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck Tague", "francktague.devfull@gmail.com",
                "06 59 90 93 37", "rue du Pont Saint-Martial"), cv);
        org.junit.jupiter.api.Assertions.assertEquals(1, CvPdf.pages(pdf));
        String texte = TexteDocument.extraire(pdf, "cv.pdf", "application/pdf");
        assertTrue(texte.contains("Livry-sur-Seine"), texte);
        try (var doc = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 70), "png", new File("target/cv-2x2-apercu.png"));
        }
    }

    @Test
    void cvPresqueVideNePlantePas() {
        byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck", null, null, null),
                new CvPdf.Cv(null, "Profil court mais suffisant pour etre lu par un logiciel.", null, null, null,
                        null, null, null, null, null, null));
        assertTrue(TexteDocument.extraire(pdf, "cv.pdf", "application/pdf").contains("PROFIL"));
        for (String modele : CvPdf.MODELES) {
            assertEquals(1, CvPdf.pages(CvPdf.generer(new CvPdf.Contact("Franck", null, null, null),
                    new CvPdf.Cv(null, null, null, null, null, null, null, null, null, null, null, modele, false))));
        }
    }

    @Test
    void unCvChargeTientSurUnePage() throws Exception {
        List<CvPdf.Experience> experiences = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            experiences.add(new CvPdf.Experience("Developpeur Java " + i, "Entreprise " + i, "Paris", "202" + i,
                    List.of("Conception et developpement d'API REST avec Spring Boot et PostgreSQL pour une equipe agile",
                            "Mise en place de tests unitaires JUnit et d'integration continue avec GitHub Actions",
                            "Amelioration des performances et documentation technique de l'application")));
        }
        CvPdf.Cv charge = new CvPdf.Cv(CV.titre(), CV.profil() + " " + CV.profil(), CV.competencesTechniques(),
                CV.competencesTransverses(), experiences, CV.projets(), CV.formations(), CV.langues(), CV.atouts(),
                CV.centresInteret(), CV.liens());
        byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck Tague", "franck@exemple.fr", "06 12 34 56 78", "Paris"),
                charge);
        org.junit.jupiter.api.Assertions.assertEquals(1, CvPdf.pages(pdf));
        try (var doc = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 70), "png", new File("target/cv-charge-apercu.png"));
        }
    }

    @Test
    void lesLiensSontDesAdressesWebCliquablesMemeAvecUnLibelle() throws Exception {
        assertEquals("https://linkedin.com/in/tague-franck", CvPdf.lienDansTexte("linkedin.com/in/tague-franck"));
        assertEquals(null, CvPdf.lienDansTexte("Node.js"));
        assertEquals(null, CvPdf.lienDansTexte("ASP.NET"));
        assertEquals(null, CvPdf.lienDansTexte("Render/Vercel"));
        CvPdf.Cv cv = new CvPdf.Cv(CV.titre(), CV.profil() + " Démo en ligne : postulyn.vercel.app.", CV.competencesTechniques(),
                CV.competencesTransverses(), CV.experiences(), CV.projets(), CV.formations(), CV.langues(), CV.atouts(),
                CV.centresInteret(), List.of("LinkedIn : linkedin.com/in/tague-franck", "https://github.com/TAGUE-GITH/"),
                null, false);
        for (String modele : CvPdf.MODELES) {
            CvPdf.Cv m = new CvPdf.Cv(cv.titre(), cv.profil(), cv.competencesTechniques(), cv.competencesTransverses(),
                    cv.experiences(), cv.projets(), cv.formations(), cv.langues(), cv.atouts(), cv.centresInteret(),
                    cv.liens(), modele, false);
            byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck Tague", "franck@exemple.fr", "06 12 34 56 78", "Paris"), m);
            try (var doc = Loader.loadPDF(pdf)) {
                List<String> liens = new java.util.ArrayList<>();
                for (var a : doc.getPage(0).getAnnotations()) {
                    if (a instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink l
                            && l.getAction() instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionURI u) {
                        liens.add(u.getURI());
                    }
                }
                assertTrue(liens.contains("https://linkedin.com/in/tague-franck")
                        && liens.contains("https://github.com/TAGUE-GITH/")
                        && liens.contains("https://postulyn.vercel.app"), modele + " " + liens);
                // Aucun lien relatif (que le lecteur PDF ouvrirait comme un fichier)
                assertTrue(liens.stream().allMatch(x -> x.matches("^(https?|mailto|tel):.+")), modele + " " + liens);
            }
        }
    }
}
