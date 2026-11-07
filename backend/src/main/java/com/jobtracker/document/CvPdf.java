package com.jobtracker.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * CV au format "ATS" : une seule colonne, vrai texte selectionnable, police standard, titres de rubriques
 * classiques, puces simples, sans tableau ni image. C'est ce que les logiciels de tri lisent le mieux.
 */
public final class CvPdf {

    public record Contact(String nom, String email, String telephone, String adresse) {
    }

    public record Experience(String poste, String entreprise, String lieu, String periode, List<String> missions) {
    }

    public record Projet(String nom, String description, String technologies) {
    }

    public record Formation(String diplome, String etablissement, String periode, String details) {
    }

    public record Cv(
            String titre,
            String profil,
            List<String> competencesTechniques,
            List<String> competencesTransverses,
            List<Experience> experiences,
            List<Projet> projets,
            List<Formation> formations,
            List<String> langues,
            List<String> atouts,
            List<String> centresInteret,
            List<String> liens) {
    }

    private static final float MARGE = 50;
    private static final float LARGEUR = PDRectangle.A4.getWidth() - 2 * MARGE;
    private static final float TEXTE = 10.5f;
    private static final float INTERLIGNE = 14;
    private static final float ACCENT_R = 0.31f;
    private static final float ACCENT_G = 0.27f;
    private static final float ACCENT_B = 0.90f;

    private CvPdf() {
    }

    public static byte[] generer(Contact contact, Cv cv) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Ecrivain e = new Ecrivain(doc);

            e.ligne(contact.nom(), e.gras, 18, 0);
            e.espace(4);
            if (!vide(cv.titre())) {
                e.paragraphe(cv.titre(), e.gras, 12.5f, 0, true);
            }
            List<String> coordonnees = new ArrayList<>();
            coordonnees.add(contact.telephone());
            coordonnees.add(contact.email());
            if (!vide(contact.adresse())) {
                coordonnees.add(contact.adresse().replace("\n", ", "));
            }
            if (cv.liens() != null) {
                coordonnees.addAll(cv.liens());
            }
            e.paragraphe(String.join("  |  ", coordonnees.stream().filter(s -> !vide(s)).toList()),
                    e.normal, 9.5f, 0, false);

            if (!vide(cv.profil())) {
                e.rubrique("PROFIL");
                e.paragraphe(cv.profil(), e.normal, TEXTE, 0, false);
            }
            if (!liste(cv.competencesTechniques()).isEmpty() || !liste(cv.competencesTransverses()).isEmpty()) {
                e.rubrique("COMPÉTENCES");
                if (!liste(cv.competencesTechniques()).isEmpty()) {
                    e.libelle("Techniques : ", String.join(", ", liste(cv.competencesTechniques())));
                }
                if (!liste(cv.competencesTransverses()).isEmpty()) {
                    e.libelle("Transverses : ", String.join(", ", liste(cv.competencesTransverses())));
                }
            }
            if (!liste(cv.experiences()).isEmpty()) {
                e.rubrique("EXPÉRIENCE PROFESSIONNELLE");
                for (Experience x : liste(cv.experiences())) {
                    e.entete(joindre(" - ", x.poste(), x.entreprise()), joindre(" | ", x.lieu(), x.periode()));
                    for (String mission : liste(x.missions())) {
                        e.puce(mission);
                    }
                    e.espace(4);
                }
            }
            if (!liste(cv.projets()).isEmpty()) {
                e.rubrique("PROJETS");
                for (Projet p : liste(cv.projets())) {
                    e.entete(p.nom(), null);
                    if (!vide(p.description())) {
                        e.paragraphe(p.description(), e.normal, TEXTE, 0, false);
                    }
                    if (!vide(p.technologies())) {
                        e.libelle("Technologies : ", p.technologies());
                    }
                    e.espace(4);
                }
            }
            if (!liste(cv.formations()).isEmpty()) {
                e.rubrique("FORMATION");
                for (Formation f : liste(cv.formations())) {
                    e.entete(joindre(" - ", f.diplome(), f.etablissement()), f.periode());
                    if (!vide(f.details())) {
                        e.paragraphe(f.details(), e.normal, TEXTE, 0, false);
                    }
                    e.espace(3);
                }
            }
            section(e, "LANGUES", cv.langues());
            section(e, "ATOUTS", cv.atouts());
            section(e, "CENTRES D'INTÉRÊT", cv.centresInteret());

            e.fermer();
            doc.save(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Impossible de generer le CV", ex);
        }
    }

    private static void section(Ecrivain e, String titre, List<String> elements) throws IOException {
        if (!liste(elements).isEmpty()) {
            e.rubrique(titre);
            e.paragraphe(String.join("  |  ", liste(elements)), e.normal, TEXTE, 0, false);
        }
    }

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l.stream()
                .filter(Objects::nonNull)
                .filter(x -> !(x instanceof String s) || !s.isBlank())
                .toList();
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String joindre(String separateur, String... parties) {
        return String.join(separateur, java.util.Arrays.stream(parties).filter(p -> !vide(p)).map(String::strip).toList());
    }

    /** Ecrit de haut en bas et passe a la page suivante quand il n'y a plus de place. */
    private static final class Ecrivain {

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDDocument doc;
        private PDPageContentStream flux;
        private float y;

        Ecrivain(PDDocument doc) throws IOException {
            this.doc = doc;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            fermer();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            flux = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGE;
        }

        private void place(float hauteur) throws IOException {
            if (y - hauteur < MARGE) {
                nouvellePage();
            }
        }

        void espace(float points) {
            y -= points;
        }

        void ligne(String texte, PDType1Font police, float taille, float retrait) throws IOException {
            place(taille + 3);
            ecrire(MARGE + retrait, y - taille, LettrePdf.nettoyer(texte == null ? "" : texte, police), police, taille, false);
            y -= taille + 3;
        }

        private void ecrire(float x, float yLigne, String texte, PDType1Font police, float taille, boolean accent)
                throws IOException {
            if (texte.isEmpty()) {
                return;
            }
            flux.beginText();
            if (accent) {
                flux.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
            }
            flux.setFont(police, taille);
            flux.newLineAtOffset(x, yLigne);
            flux.showText(texte);
            flux.endText();
            if (accent) {
                flux.setNonStrokingColor(0f, 0f, 0f);
            }
        }

        void paragraphe(String texte, PDType1Font police, float taille, float retrait, boolean accent)
                throws IOException {
            for (String para : LettrePdf.nettoyer(texte, police).split("\n")) {
                for (String l : LettrePdf.couper(para, police, LARGEUR - retrait, taille)) {
                    place(INTERLIGNE);
                    ecrire(MARGE + retrait, y - taille, l, police, taille, accent);
                    y -= INTERLIGNE * taille / TEXTE;
                }
            }
        }

        /** Titre de rubrique en capitales, souligne d'un trait. */
        void rubrique(String titre) throws IOException {
            place(40);
            y -= 12;
            ecrire(MARGE, y - 11.5f, titre, gras, 11.5f, true);
            y -= 16;
            flux.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
            flux.setLineWidth(0.8f);
            flux.moveTo(MARGE, y);
            flux.lineTo(MARGE + LARGEUR, y);
            flux.stroke();
            y -= 6;
        }

        /** Ligne en gras a gauche (poste, diplome) et texte a droite (dates). */
        void entete(String gauche, String droite) throws IOException {
            place(INTERLIGNE * 2);
            String d = vide(droite) ? "" : LettrePdf.nettoyer(droite, normal).strip();
            float largeurDroite = d.isEmpty() ? 0 : normal.getStringWidth(d) / 1000 * TEXTE + 12;
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(vide(gauche) ? "" : gauche, gras), gras,
                    LARGEUR - largeurDroite, TEXTE);
            for (int i = 0; i < lignes.size(); i++) {
                ecrire(MARGE, y - TEXTE, lignes.get(i), gras, TEXTE, false);
                if (i == 0 && !d.isEmpty()) {
                    ecrire(MARGE + LARGEUR - (largeurDroite - 12), y - TEXTE, d, normal, TEXTE, false);
                }
                y -= INTERLIGNE;
            }
        }

        void puce(String texte) throws IOException {
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(texte, normal), normal, LARGEUR - 14, TEXTE);
            for (int i = 0; i < lignes.size(); i++) {
                place(INTERLIGNE);
                if (i == 0) {
                    ecrire(MARGE + 2, y - TEXTE, "-", normal, TEXTE, false);
                }
                ecrire(MARGE + 14, y - TEXTE, lignes.get(i), normal, TEXTE, false);
                y -= INTERLIGNE;
            }
        }

        /** "Techniques : Java, Spring Boot..." avec le libelle en gras. */
        void libelle(String libelle, String texte) throws IOException {
            float decalage = gras.getStringWidth(libelle) / 1000 * TEXTE;
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(texte, normal), normal, LARGEUR - decalage, TEXTE);
            for (int i = 0; i < lignes.size(); i++) {
                place(INTERLIGNE);
                if (i == 0) {
                    ecrire(MARGE, y - TEXTE, libelle, gras, TEXTE, false);
                }
                ecrire(MARGE + decalage, y - TEXTE, lignes.get(i), normal, TEXTE, false);
                y -= INTERLIGNE;
            }
        }

        void fermer() throws IOException {
            if (flux != null) {
                flux.close();
                flux = null;
            }
        }
    }
}
