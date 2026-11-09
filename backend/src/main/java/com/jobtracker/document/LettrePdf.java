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

/**
 * Met en page une lettre de motivation dans un PDF A4, a la francaise : coordonnees de l'expediteur
 * en haut a gauche, destinataire a droite, lieu et date en dessous, puis le corps de la lettre.
 */
public final class LettrePdf {

    /** En-tete de la lettre ; chaque bloc peut contenir plusieurs lignes (separees par \n) ou etre vide. */
    public record EnTete(String expediteur, String destinataire, String lieuDate) {

        public static final EnTete VIDE = new EnTete(null, null, null);
    }

    private static final float MARGE = 64;
    private static final float TAILLE_POLICE = 11;
    private static final float INTERLIGNE = 15.5f;
    private static final float LARGEUR = PDRectangle.A4.getWidth() - 2 * MARGE;
    /** Le bloc destinataire et la date commencent a 55 % de la largeur de la page. */
    private static final float X_DROITE = PDRectangle.A4.getWidth() * 0.55f;

    private LettrePdf() {
    }

    static byte[] generer(String texte) {
        return generer(EnTete.VIDE, texte);
    }

    static byte[] generer(EnTete entete, String texte) {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Page page = new Page(doc);
            float haut = page.y;

            // Expediteur (gauche) : la premiere ligne, le nom, en gras
            List<String> expediteur = blocs(entete.expediteur(), police, X_DROITE - MARGE - 16);
            float yGauche = haut;
            for (int i = 0; i < expediteur.size(); i++) {
                page.ecrire(MARGE, yGauche, expediteur.get(i), i == 0 ? gras : police);
                yGauche -= INTERLIGNE;
            }

            // Destinataire (droite), legerement plus bas que le haut de l'expediteur
            List<String> destinataire = blocs(entete.destinataire(), police, PDRectangle.A4.getWidth() - MARGE - X_DROITE);
            float yDroite = haut - (expediteur.isEmpty() ? 0 : 3 * INTERLIGNE);
            for (int i = 0; i < destinataire.size(); i++) {
                page.ecrire(X_DROITE, yDroite, destinataire.get(i), i == 0 ? gras : police);
                yDroite -= INTERLIGNE;
            }

            float y = Math.min(yGauche, yDroite);
            String lieuDate = nettoyer(entete.lieuDate() == null ? "" : entete.lieuDate(), police).strip();
            if (!lieuDate.isEmpty()) {
                y -= INTERLIGNE;
                page.ecrire(X_DROITE, y, lieuDate, police);
                y -= INTERLIGNE;
            }
            if (y < haut) {
                y -= 2 * INTERLIGNE;
            }

            // Corps de la lettre, sur plusieurs pages si besoin
            page.y = y;
            for (String paragraphe : nettoyer(texte, police).split("\n", -1)) {
                for (String ligne : couper(paragraphe, police, LARGEUR, TAILLE_POLICE)) {
                    page.ligneSuivante(ligne, police);
                }
            }
            page.fermer();
            doc.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de generer le PDF", e);
        }
    }

    /** Ecrit le texte et ouvre une nouvelle page quand la precedente est pleine. */
    private static final class Page {

        private final PDDocument doc;
        private PDPageContentStream flux;
        float y;

        Page(PDDocument doc) throws IOException {
            this.doc = doc;
            nouvelle();
        }

        private void nouvelle() throws IOException {
            fermer();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            flux = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGE;
        }

        void ecrire(float x, float yLigne, String texte, PDType1Font police) throws IOException {
            if (texte.isEmpty()) {
                return;
            }
            flux.beginText();
            flux.setFont(police, TAILLE_POLICE);
            flux.newLineAtOffset(x, yLigne);
            flux.showText(texte);
            flux.endText();
        }

        void ligneSuivante(String texte, PDType1Font police) throws IOException {
            if (y < MARGE) {
                nouvelle();
            }
            ecrire(MARGE, y, texte, police);
            y -= INTERLIGNE;
        }

        void fermer() throws IOException {
            if (flux != null) {
                flux.close();
                flux = null;
            }
        }
    }

    private static List<String> blocs(String bloc, PDType1Font police, float largeur) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (bloc == null || bloc.isBlank()) {
            return lignes;
        }
        for (String ligne : nettoyer(bloc, police).strip().split("\n")) {
            if (!ligne.isBlank()) {
                lignes.addAll(couper(ligne, police, largeur, TAILLE_POLICE));
            }
        }
        return lignes;
    }

    /** Remplace ou retire les caracteres que la police standard ne sait pas afficher (emojis...). */
    static String nettoyer(String texte, PDType1Font police) {
        String t = texte.replace("\r", "")
                .replace('’', '\'').replace('‘', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-').replace('‐', '-').replace('‑', '-').replace('−', '-').replace("…", "...")
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

    static List<String> couper(String paragraphe, PDType1Font police, float largeur, float taille) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (paragraphe.isBlank()) {
            lignes.add("");
            return lignes;
        }
        StringBuilder ligne = new StringBuilder();
        for (String mot : paragraphe.strip().split(" +")) {
            String essai = ligne.isEmpty() ? mot : ligne + " " + mot;
            if (police.getStringWidth(essai) / 1000 * taille > largeur && !ligne.isEmpty()) {
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
