package com.jobtracker.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class ModelesCvTests {

    private static byte[] photo() throws Exception {
        BufferedImage img = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 110, 160));
        g.fillRect(0, 0, 300, 400);
        g.setColor(new Color(235, 200, 170));
        g.fillOval(90, 80, 120, 150);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private static CvPdf.Cv cv(String modele, boolean avecPhoto) {
        CvPdf.Cv base = CvPdfTests.CV;
        return new CvPdf.Cv(base.titre(), base.profil(), base.competencesTechniques(), base.competencesTransverses(),
                List.of(new CvPdf.Experience("Développeur fullstack (stage)", "DITS Group", "Livry-sur-Seine",
                                "Juin 2026 - Sept. 2026",
                                List.of("Développé une API REST sécurisée (JWT) en Python/Flask pour un site vitrine client",
                                        "Mis en place l'intégration continue GitHub Actions et le déploiement Render/Vercel",
                                        "Rédigé la documentation fonctionnelle et technique pour l'équipe commerciale",
                                        "Participé aux rituels agiles Scrum avec l'équipe produit")),
                        new CvPdf.Experience("Développeur (stage)", "Yamo Group Sarl", "Yaoundé", "Févr. 2025 - Juil. 2025",
                                List.of("Conçu une API Flask de recherche documentaire intégrée au middleware interne",
                                        "Automatisé les tests (pytest) et conteneurisé l'application avec Docker"))),
                base.projets(),
                List.of(new CvPdf.Formation("Cycle Expert en architecture logicielle", "3IL Ingénieurs", "Sept. 2025 - présent",
                                "Spécialisation architectures SOA et intégration d'APIs", "Limoges"),
                        new CvPdf.Formation("Diplôme d'ingénieur en génie informatique", "École polytechnique de Douala",
                                "2020 - 2025", null, "Douala"),
                        new CvPdf.Formation("Baccalauréat scientifique", "Lycée de Bafang", "2019", "Mention bien", "Bafang")),
                base.langues(), base.atouts(), base.centresInteret(), base.liens(),
                modele, avecPhoto);
    }

    @Test
    void chaqueModeleResteLisibleParUnAtsEtTientSurUnePage() throws Exception {
        byte[] photo = photo();
        for (String modele : CvPdf.MODELES) {
            for (boolean avecPhoto : new boolean[] {false, true}) {
                byte[] pdf = CvPdf.generer(new CvPdf.Contact("Franck Tague", "franck@exemple.fr", "06 12 34 56 78",
                        "Paris", photo), cv(modele, avecPhoto));
                assertEquals(1, CvPdf.pages(pdf), modele);
                String texte = TexteDocument.extraire(pdf, "cv.pdf", "application/pdf").replace("\r", "")
                        .replaceAll(" +", " ");
                int nom = texte.toUpperCase().indexOf("FRANCK TAGUE");
                int experience = texte.indexOf("EXPÉRIENCES PROFESSIONNELLES");
                if (experience < 0) {
                    experience = texte.indexOf("EXPÉRIENCE PROFESSIONNELLE");
                }
                assertTrue(nom >= 0 && nom < experience && texte.contains("FORMATION"), modele + "\n" + texte);
                // Toutes les formations sont gardees
                assertTrue(texte.contains("Cycle Expert") && texte.contains("génie informatique")
                        && texte.contains("Baccalauréat scientifique"), modele + "\n" + texte);
                assertTrue(texte.toUpperCase().contains("DITS GROUP") && texte.contains("Juin 2026 - Sept. 2026"), modele + "\n" + texte);
                assertTrue(texte.contains("\n• Développé une API REST"), modele + "\n" + texte);
                if (avecPhoto) {
                    try (var doc = Loader.loadPDF(pdf)) {
                        ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 80), "png",
                                new File("target/cv-" + modele.toLowerCase() + ".png"));
                    }
                }
            }
        }
    }

    @Test
    void lettreJustifieeSurUnePageSansSignature() throws Exception {
        String corps = """
                Objet : Candidature au poste de développeur Java en alternance

                Madame, Monsieur,

                %s

                %s

                Je reste à votre disposition pour un entretien. Je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées.

                Cordialement,

                Franck Tague
                """.formatted("Votre entreprise conçoit des applications utilisées par des milliers de clients. ".repeat(6),
                "Lors de mon stage, j'ai développé une API REST sécurisée et mis en place l'intégration continue. ".repeat(6));
        byte[] pdf = LettrePdf.generer(new LettrePdf.EnTete("Franck Tague\n12 rue des Lilas\n75011 Paris",
                "Atlas Studio\nService recrutement\n10 avenue de France\n75013 Paris", "À Paris, le 12 novembre 2026"),
                corps);
        try (var doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 80), "png", new File("target/lettre.png"));
        }
        String texte = TexteDocument.extraire(pdf, "lettre.pdf", "application/pdf");
        assertTrue(texte.contains("Objet : Candidature"), texte);
        assertTrue(texte.strip().endsWith("Franck Tague"), texte);
        assertEquals(List.of("a", "b c"), LettrePdf.paragraphes("  a \n\n\n b   c \n"));
    }
}
