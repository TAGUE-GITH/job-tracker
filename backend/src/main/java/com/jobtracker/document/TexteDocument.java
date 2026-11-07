package com.jobtracker.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Extrait le texte d'un CV (PDF, Word .docx ou texte) pour que l'IA puisse le lire. */
final class TexteDocument {

    static final int TAILLE_MAX = 12000;

    private TexteDocument() {
    }

    static String extraire(byte[] donnees, String nomFichier, String contentType) {
        String nom = nomFichier == null ? "" : nomFichier.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String texte;
        try {
            if (nom.endsWith(".pdf") || type.contains("pdf")) {
                texte = pdf(donnees);
            } else if (nom.endsWith(".docx") || type.contains("wordprocessingml")) {
                texte = docx(donnees);
            } else if (nom.endsWith(".txt") || nom.endsWith(".md") || type.startsWith("text/")) {
                texte = new String(donnees, StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException(
                        "Format de CV non lu : importe ton CV en PDF ou en Word (.docx)");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier " + nomFichier);
        }
        texte = texte.replaceAll("[ \\t\\x0B\\f]+", " ").replaceAll("\\s*\\n\\s*\\n\\s*", "\n\n").strip();
        if (texte.length() < 50) {
            throw new IllegalArgumentException("Le CV ne contient pas de texte lisible (PDF scanne en image ?) : "
                    + "exporte-le en PDF depuis Word ou Google Docs");
        }
        return texte.length() > TAILLE_MAX ? texte.substring(0, TAILLE_MAX) : texte;
    }

    private static String pdf(byte[] donnees) throws IOException {
        try (PDDocument doc = Loader.loadPDF(donnees)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    /** Un .docx est un zip : le texte est dans word/document.xml, un paragraphe par balise w:p. */
    private static String docx(byte[] donnees) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(donnees))) {
            ZipEntry entree;
            while ((entree = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entree.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return xml.replaceAll("</w:p>", "\n")
                            .replaceAll("<w:tab/>", " ")
                            .replaceAll("<[^>]+>", "")
                            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                            .replace("&apos;", "'").replace("&amp;", "&");
                }
            }
        }
        throw new IOException("document.xml absent");
    }
}
