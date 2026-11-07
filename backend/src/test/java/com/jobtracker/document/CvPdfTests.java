package com.jobtracker.document;

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
            List.of(new CvPdf.Projet("Taguora", "Application de suivi de candidatures avec assistant IA.",
                    "Java, Spring Boot, React, Docker, PostgreSQL")),
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
        String texte = TexteDocument.extraire(pdf, "cv.pdf", "application/pdf");
        // Texte reel, dans l'ordre de lecture attendu par les logiciels de tri
        int nom = texte.indexOf("Franck Tague");
        int competences = texte.indexOf("COMPÉTENCES");
        int experience = texte.indexOf("EXPÉRIENCE PROFESSIONNELLE");
        int formation = texte.indexOf("FORMATION");
        assertTrue(nom >= 0 && nom < competences && competences < experience && experience < formation, texte);
        assertTrue(texte.contains("Spring Boot"), texte);
        assertTrue(texte.contains("06 12 34 56 78"), texte);
        assertTrue(texte.contains("CENTRES D'INTÉRÊT"), texte);

        // Apercu visuel pour verification manuelle (target/cv-apercu.png)
        try (var doc = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 70), "png", new File("target/cv-apercu.png"));
        }
    }

    @Test
    void cvPresqueVideNePlantePas() {
        byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck", null, null, null),
                new CvPdf.Cv(null, "Profil court mais suffisant pour etre lu par un logiciel.", null, null, null,
                        null, null, null, null, null, null));
        assertTrue(TexteDocument.extraire(pdf, "cv.pdf", "application/pdf").contains("PROFIL"));
    }
}
