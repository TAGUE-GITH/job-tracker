package com.jobtracker.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Met en page une lettre de motivation (texte brut) dans un PDF A4. */
final class LettrePdf {

    private static final float MARGE = 64;
    private static final float TAILLE_POLICE = 11;
    private static final float INTERLIGNE = 15.5f;

    private LettrePdf() {
    }

    static byte[] generer(String texte) {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            float largeur = PDRectangle.A4.getWidth() - 2 * MARGE;
            List<String> lignes = new ArrayList<>();
            for (String paragraphe : nettoyer(texte, police).split("\n", -1)) {
                lignes.addAll(couper(paragraphe, police, largeur));
            }

            PDPage page = null;
            PDPageContentStream flux = null;
            float y = 0;
            for (String ligne : lignes) {
                if (flux == null || y < MARGE) {
                    if (flux != null) {
                        flux.endText();
                        flux.close();
                    }
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    flux = new PDPageContentStream(doc, page);
                    flux.beginText();
                    flux.setFont(police, TAILLE_POLICE);
                    y = PDRectangle.A4.getHeight() - MARGE;
                    flux.newLineAtOffset(MARGE, y);
                }
                flux.showText(ligne);
                flux.newLineAtOffset(0, -INTERLIGNE);
                y -= INTERLIGNE;
            }
            if (flux != null) {
                flux.endText();
                flux.close();
            }
            doc.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de generer le PDF", e);
        }
    }

    /** Remplace ou retire les caracteres que la police standard ne sait pas afficher (emojis...). */
    private static String nettoyer(String texte, PDType1Font police) {
        String t = texte.replace("\r", "")
                .replace('’', '\'').replace('‘', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-').replace("…", "...")
                .replace(' ', ' ').replace(' ', ' ').replace("\t", "    ")
                .replaceAll("\\*\\*|__", "").replaceAll("(?m)^#+\\s*", "");
        StringBuilder sb = new StringBuilder();
        t.codePoints().forEach(cp -> {
            String c = new String(Character.toChars(cp));
            if (c.equals("\n")) {
                sb.append(c);
                return;
            }
            try {
                police.encode(c);
                sb.append(c);
            } catch (IOException | IllegalArgumentException e) {
                // caractere absent de la police : ignore
            }
        });
        return sb.toString();
    }

    private static List<String> couper(String paragraphe, PDType1Font police, float largeur) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (paragraphe.isBlank()) {
            lignes.add("");
            return lignes;
        }
        StringBuilder ligne = new StringBuilder();
        for (String mot : paragraphe.strip().split(" +")) {
            String essai = ligne.isEmpty() ? mot : ligne + " " + mot;
            if (police.getStringWidth(essai) / 1000 * TAILLE_POLICE > largeur && !ligne.isEmpty()) {
                lignes.add(ligne.toString());
                ligne = new StringBuilder(mot);
            } else {
                ligne = new StringBuilder(essai);
            }
        }
        lignes.add(ligne.toString());
        return lignes;
    }
}
