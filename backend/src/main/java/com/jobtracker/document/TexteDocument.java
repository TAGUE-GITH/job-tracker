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

/**
 * Extrait le texte d'un CV (PDF, Word .docx ou texte) pour que l'IA puisse le lire. Les liens caches derriere un
 * texte (ex. "Voir en ligne") sont ecrits juste apres ce texte, entre parentheses, pour ne pas etre perdus.
 */
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
            return new LecteurAvecLiens().getText(doc);
        }
    }

    private record ZoneLien(org.apache.pdfbox.pdmodel.common.PDRectangle zone, String uri) {
    }

    /** Lecteur de PDF qui ajoute l'adresse des liens apres le texte cliquable : "Voir en ligne (https://...)". */
    private static final class LecteurAvecLiens extends PDFTextStripper {

        private java.util.List<ZoneLien> zones = java.util.List.of();

        @Override
        protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
            java.util.List<ZoneLien> liste = new java.util.ArrayList<>();
            for (var a : page.getAnnotations()) {
                if (a instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink lien
                        && lien.getAction() instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionURI u
                        && u.getURI() != null && !u.getURI().isBlank()) {
                    liste.add(new ZoneLien(lien.getRectangle(), u.getURI().strip()));
                }
            }
            zones = liste;
            super.startPage(page);
        }

        @Override
        protected void writeString(String texte, java.util.List<org.apache.pdfbox.text.TextPosition> positions)
                throws IOException {
            if (zones.isEmpty() || positions.isEmpty()) {
                super.writeString(texte, positions);
                return;
            }
            float hauteurPage = getCurrentPage().getMediaBox().getHeight();
            StringBuilder sb = new StringBuilder();
            ZoneLien courante = null;
            for (org.apache.pdfbox.text.TextPosition tp : positions) {
                float x = tp.getXDirAdj() + tp.getWidthDirAdj() / 2;
                float y = hauteurPage - (tp.getYDirAdj() - tp.getHeightDir() / 2);
                ZoneLien zone = null;
                for (ZoneLien z : zones) {
                    if (z.zone().contains(x, y)) {
                        zone = z;
                        break;
                    }
                }
                if (courante != null && zone != courante) {
                    ajouterAdresse(sb, courante);
                }
                courante = zone;
                sb.append(tp.getUnicode());
            }
            if (courante != null) {
                ajouterAdresse(sb, courante);
            }
            super.writeString(sb.toString(), positions);
        }

        /** N'ajoute pas l'adresse si le texte cliquable est deja l'adresse elle-meme. */
        private static void ajouterAdresse(StringBuilder sb, ZoneLien z) {
            String court = z.uri().replaceFirst("(?i)^(https?://|mailto:|tel:)", "").replaceFirst("(?i)^www\\.", "")
                    .replaceAll("/+$", "");
            if (!sb.toString().toLowerCase(Locale.ROOT).contains(court.toLowerCase(Locale.ROOT))) {
                sb.append(" (").append(z.uri()).append(')');
            }
        }
    }

    /**
     * Un .docx est un zip : le texte est dans word/document.xml, un paragraphe par balise w:p ; l'adresse des
     * liens (w:hyperlink) est dans word/_rels/document.xml.rels.
     */
    private static String docx(byte[] donnees) throws IOException {
        String xml = null;
        String relations = "";
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(donnees))) {
            ZipEntry entree;
            while ((entree = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entree.getName())) {
                    xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                } else if ("word/_rels/document.xml.rels".equals(entree.getName())) {
                    relations = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        if (xml != null) {
            java.util.Map<String, String> cibles = new java.util.HashMap<>();
            java.util.regex.Matcher r = java.util.regex.Pattern
                    .compile("<Relationship [^>]*?Id=\"([^\"]+)\"[^>]*?Target=\"(https?:[^\"]+|mailto:[^\"]+)\"")
                    .matcher(relations);
            while (r.find()) {
                cibles.put(r.group(1), r.group(2).replace("&amp;", "&"));
            }
            java.util.regex.Matcher h = java.util.regex.Pattern
                    .compile("(?s)<w:hyperlink [^>]*?r:id=\"([^\"]+)\"[^>]*>(.*?)</w:hyperlink>").matcher(xml);
            StringBuilder avecLiens = new StringBuilder();
            while (h.find()) {
                String cible = cibles.get(h.group(1));
                h.appendReplacement(avecLiens, java.util.regex.Matcher.quoteReplacement(
                        h.group(2) + (cible == null ? "" : "<w:t> (" + cible + ")</w:t>")));
            }
            h.appendTail(avecLiens);
            xml = avecLiens.toString();
        }
        if (xml == null) {
            throw new IOException("document.xml absent");
        }
        return xml.replaceAll("</w:p>", "\n")
                .replaceAll("<w:tab/>", " ")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
