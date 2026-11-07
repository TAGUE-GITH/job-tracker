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
