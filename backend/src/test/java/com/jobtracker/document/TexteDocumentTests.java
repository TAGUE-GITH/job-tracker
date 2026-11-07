package com.jobtracker.document;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class TexteDocumentTests {

    private static final String LETTRE = """
            Objet : Candidature au poste de développeur Java en alternance

            Madame, Monsieur,

            Passionné par le développement backend, j’ai réalisé une application complète avec Spring Boot, \
            React et Docker, déployée en production avec une intégration continue GitHub Actions. 🚀
            Franck""";

    @Test
    void lettrePdfRelisibleAvecAccents() {
        byte[] pdf = LettrePdf.generer(LETTRE);
        String texte = TexteDocument.extraire(pdf, "lettre.pdf", "application/pdf");
        assertTrue(texte.contains("Madame, Monsieur,"), texte);
        assertTrue(texte.contains("développeur Java"), texte);
        assertTrue(texte.contains("j'ai réalisé"), texte);
    }

    @Test
    void enTeteAvecCoordonneesDestinataireEtDate() throws Exception {
        LettrePdf.EnTete entete = new LettrePdf.EnTete(
                "Franck Tague\n12 rue des Lilas\n75011 Paris\n06 12 34 56 78\nfranck@exemple.fr",
                "Liksi\nService Recrutement\n44000 Nantes",
                "À Paris, le 7 novembre 2026");
        byte[] pdf = LettrePdf.generer(entete, LETTRE);
        String texte = TexteDocument.extraire(pdf, "lettre.pdf", "application/pdf");
        assertTrue(texte.contains("06 12 34 56 78"), texte);
        assertTrue(texte.contains("Service Recrutement"), texte);
        assertTrue(texte.contains("À Paris, le 7 novembre 2026"), texte);
        assertTrue(texte.indexOf("franck@exemple.fr") < texte.indexOf("Madame, Monsieur"), texte);

        // Apercu visuel pour verification manuelle (target/lettre-apercu.png)
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            var image = new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 60);
            javax.imageio.ImageIO.write(image, "png", new java.io.File("target/lettre-apercu.png"));
        }
    }

    @Test
    void lettreLonguePasseSurPlusieursPages() {
        String longue = (LETTRE + "\n\n").repeat(12);
        String texte = TexteDocument.extraire(LettrePdf.generer(longue), "lettre.pdf", "application/pdf");
        assertTrue(texte.contains("Franck"));
    }

    @Test
    void litUnFichierWord() throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(sortie)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document><w:body><w:p><w:r><w:t>Franck Tague</w:t></w:r></w:p>"
                    + "<w:p><w:r><w:t>Développeur Java &amp; React, Spring Boot, Docker, GitHub Actions</w:t></w:r></w:p>"
                    + "</w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        String texte = TexteDocument.extraire(sortie.toByteArray(), "cv.docx", null);
        assertTrue(texte.contains("Franck Tague\nDéveloppeur Java & React"), texte);
    }

    @Test
    void refuseLesFormatsIllisibles() {
        assertThrows(IllegalArgumentException.class,
                () -> TexteDocument.extraire(new byte[] {1, 2, 3}, "cv.odt", "application/vnd.oasis"));
        assertThrows(IllegalArgumentException.class,
                () -> TexteDocument.extraire("court".getBytes(StandardCharsets.UTF_8), "cv.txt", "text/plain"));
    }
}
