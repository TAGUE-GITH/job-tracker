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
    private static final float LARGEUR = PDRectangle.A4.getWidth() - 2 * MARGE;
    /** Le bloc destinataire et la date commencent a 55 % de la largeur de la page. */
    private static final float X_DROITE = PDRectangle.A4.getWidth() * 0.55f;
    /** Tailles de police essayees dans l'ordre pour que la lettre tienne sur une page. */
    private static final float[] TAILLES = {11, 10.5f, 10, 9.6f};

    private LettrePdf() {
    }

    static byte[] generer(String texte) {
        return generer(EnTete.VIDE, texte);
    }

    static byte[] generer(EnTete entete, String texte) {
        byte[] pdf = null;
        for (float taille : TAILLES) {
            Rendu r = rendre(entete, texte, taille);
            pdf = r.pdf();
            if (r.pages() == 1) {
                break;
            }
        }
        return pdf;
    }

    private record Rendu(byte[] pdf, int pages) {
    }

    private static Rendu rendre(EnTete entete, String texte, float taille) {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        float interligne = taille * 1.42f;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Page page = new Page(doc, taille, interligne);
            float haut = page.y;

            // Expediteur (gauche) : la premiere ligne, le nom, en gras
            List<String> expediteur = blocs(entete.expediteur(), police, X_DROITE - MARGE - 16, taille);
            float yGauche = haut;
            for (int i = 0; i < expediteur.size(); i++) {
                page.ecrire(MARGE, yGauche, expediteur.get(i), i == 0 ? gras : police);
                yGauche -= interligne;
            }

            // Destinataire (droite), aligne sur la meme colonne que la date, sous le haut de l'expediteur
            List<String> destinataire = blocs(entete.destinataire(), police,
                    PDRectangle.A4.getWidth() - MARGE - X_DROITE, taille);
            float yDroite = haut - (expediteur.isEmpty() ? 0 : 3 * interligne);
            for (int i = 0; i < destinataire.size(); i++) {
                page.ecrire(X_DROITE, yDroite, destinataire.get(i), i == 0 ? gras : police);
                yDroite -= interligne;
            }

            float y = Math.min(yGauche, yDroite);
            String lieuDate = nettoyer(entete.lieuDate() == null ? "" : entete.lieuDate(), police).strip();
            if (!lieuDate.isEmpty()) {
                y -= interligne;
                page.ecrire(X_DROITE, y, lieuDate, police);
                y -= interligne;
            }
            if (y < haut) {
                y -= 1.5f * interligne;
            }

            // Corps : paragraphes justifies, separes par un espace ; "Objet :" en gras ; signature a droite
            page.y = y;
            String nom = expediteur.isEmpty() ? null : expediteur.getFirst().strip();
            List<String> paragraphes = paragraphes(nettoyer(texte, police));
            for (int i = 0; i < paragraphes.size(); i++) {
                String para = paragraphes.get(i);
                boolean signature = i == paragraphes.size() - 1 && nom != null && para.strip().equalsIgnoreCase(nom);
                if (signature) {
                    page.y -= interligne * 0.5f;
                    page.ligneAlignee(para.strip(), gras, true);
                } else if (para.regionMatches(true, 0, "Objet", 0, 5)) {
                    for (String ligne : couper(para, gras, LARGEUR, taille)) {
                        page.ligne(ligne, gras, false);
                    }
                } else {
                    List<String> lignes = couper(para, police, LARGEUR, taille);
                    for (int j = 0; j < lignes.size(); j++) {
                        // Justifie toutes les lignes sauf la derniere de chaque paragraphe
                        page.ligne(lignes.get(j), police, j < lignes.size() - 1);
                    }
                }
                page.y -= interligne * 0.55f;
            }
            page.fermer();
            doc.save(sortie);
            return new Rendu(sortie.toByteArray(), doc.getNumberOfPages());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de generer le PDF", e);
        }
    }

    /** Decoupe le texte en paragraphes (lignes vides ignorees, espaces superflus retires). */
    static List<String> paragraphes(String texte) {
        List<String> resultat = new ArrayList<>();
        for (String ligne : texte.split("\n")) {
            String l = ligne.strip().replaceAll("\\s{2,}", " ");
            if (!l.isEmpty()) {
                resultat.add(l);
            }
        }
        return resultat;
    }

    /** Ecrit le texte et ouvre une nouvelle page quand la precedente est pleine. */
    private static final class Page {

        private final PDDocument doc;
        private final float taille;
        private final float interligne;
        private PDPageContentStream flux;
        float y;

        Page(PDDocument doc, float taille, float interligne) throws IOException {
            this.doc = doc;
            this.taille = taille;
            this.interligne = interligne;
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
            ecrire(x, yLigne, texte, police, 0);
        }

        /** espaceMots : espace ajoute a chaque espace entre les mots (justification). */
        private void ecrire(float x, float yLigne, String texte, PDType1Font police, float espaceMots)
                throws IOException {
            if (texte.isEmpty()) {
                return;
            }
            flux.beginText();
            flux.setFont(police, taille);
            flux.setWordSpacing(espaceMots);
            flux.newLineAtOffset(x, yLigne);
            flux.showText(texte);
            flux.endText();
        }

        /** Ligne du corps, justifiee sur toute la largeur si demande. */
        void ligne(String texte, PDType1Font police, boolean justifier) throws IOException {
            if (y < MARGE) {
                nouvelle();
            }
            float espace = 0;
            long espaces = texte.chars().filter(c -> c == ' ').count();
            if (justifier && espaces > 0) {
                float reste = LARGEUR - police.getStringWidth(texte) / 1000 * taille;
                // Pas d'etirement exagere sur une ligne courte
                espace = reste > 0 && reste / espaces < taille * 0.9f ? reste / espaces : 0;
            }
            ecrire(MARGE, y, texte, police, espace);
            y -= interligne;
        }

        /** Ligne alignee a droite (signature). */
        void ligneAlignee(String texte, PDType1Font police, boolean droite) throws IOException {
            if (y < MARGE) {
                nouvelle();
            }
            float l = police.getStringWidth(texte) / 1000 * taille;
            ecrire(droite ? MARGE + LARGEUR - l : MARGE, y, texte, police);
            y -= interligne;
        }

        void fermer() throws IOException {
            if (flux != null) {
                flux.close();
                flux = null;
            }
        }
    }

    private static List<String> blocs(String bloc, PDType1Font police, float largeur, float taille) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (bloc == null || bloc.isBlank()) {
            return lignes;
        }
        for (String ligne : nettoyer(bloc, police).strip().split("\n")) {
            if (!ligne.isBlank()) {
                lignes.addAll(couper(ligne, police, largeur, taille));
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
