package com.jobtracker.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Le CV tient sur une page : si le contenu deborde, la mise en page est resserree progressivement.
 */
public final class CvPdf {

    public record Contact(String nom, String email, String telephone, String adresse) {
    }

    public record Experience(String poste, String entreprise, String lieu, String periode, List<String> missions) {
    }

    /** points : realisations detaillees (fonctionnalites, architecture, resultats). */
    public record Projet(String nom, String description, String technologies, List<String> points) {
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

    /** Facteurs de resserrement essayes dans l'ordre jusqu'a tenir sur une page. */
    private static final float[] ECHELLES = {1f, 0.95f, 0.9f, 0.85f, 0.8f, 0.76f, 0.72f, 0.68f};
    private static final float ACCENT_R = 0.31f;
    private static final float ACCENT_G = 0.27f;
    private static final float ACCENT_B = 0.90f;

    private CvPdf() {
    }

    public static byte[] generer(Contact contact, Cv cv) {
        byte[] pdf = null;
        for (float echelle : ECHELLES) {
            Rendu rendu = rendre(contact, cv, echelle);
            pdf = rendu.pdf();
            if (rendu.pages() == 1) {
                break;
            }
        }
        return pdf;
    }

    /** Nombre de pages d'un CV deja genere (utile pour verifier qu'il tient sur une page). */
    public static int pages(byte[] pdf) {
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Rendu(byte[] pdf, int pages) {
    }

    private static Rendu rendre(Contact contact, Cv cv, float echelle) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Ecrivain e = new Ecrivain(doc, echelle);

            e.ligne(contact.nom(), e.gras, 18);
            e.espace(3);
            if (!vide(cv.titre())) {
                e.paragraphe(cv.titre(), e.gras, 12.5f, true);
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
                    e.normal, 9.5f, false);

            if (!vide(cv.profil())) {
                e.rubrique("PROFIL");
                e.paragraphe(cv.profil(), e.normal, e.texte, false);
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
                    e.espace(3);
                }
            }
            if (!liste(cv.projets()).isEmpty()) {
                e.rubrique(liste(cv.projets()).size() > 1 ? "PROJETS" : "PROJET");
                for (Projet p : liste(cv.projets())) {
                    e.entete(p.nom(), null);
                    if (!vide(p.description())) {
                        e.paragraphe(p.description(), e.normal, e.texte, false);
                    }
                    for (String point : liste(p.points())) {
                        e.puce(point);
                    }
                    if (!vide(p.technologies())) {
                        e.libelle("Technologies : ", p.technologies());
                    }
                    e.espace(3);
                }
            }
            if (!liste(cv.formations()).isEmpty()) {
                e.rubrique("FORMATION");
                for (Formation f : liste(cv.formations())) {
                    e.entete(joindre(" - ", f.diplome(), f.etablissement()), f.periode());
                    if (!vide(f.details())) {
                        e.paragraphe(f.details(), e.normal, e.texte, false);
                    }
                    e.espace(2);
                }
            }
            section(e, "LANGUES", cv.langues());
            section(e, "ATOUTS", cv.atouts());
            section(e, "CENTRES D'INTÉRÊT", cv.centresInteret());

            e.fermer();
            doc.save(sortie);
            return new Rendu(sortie.toByteArray(), doc.getNumberOfPages());
        } catch (IOException ex) {
            throw new UncheckedIOException("Impossible de generer le CV", ex);
        }
    }

    private static void section(Ecrivain e, String titre, List<String> elements) throws IOException {
        if (!liste(elements).isEmpty()) {
            e.rubrique(titre);
            e.paragraphe(String.join("  |  ", liste(elements)), e.normal, e.texte, false);
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
        return String.join(separateur, Arrays.stream(parties).filter(p -> !vide(p)).map(String::strip).toList());
    }

    /** Ecrit de haut en bas ; toutes les tailles et marges sont multipliees par l'echelle. */
    private static final class Ecrivain {

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final float echelle;
        final float texte;
        private final float interligne;
        private final float marge;
        private final float largeur;
        private final PDDocument doc;
        private PDPageContentStream flux;
        private float y;

        Ecrivain(PDDocument doc, float echelle) throws IOException {
            this.doc = doc;
            this.echelle = echelle;
            this.texte = 10.5f * echelle;
            this.interligne = 14f * echelle;
            this.marge = 36 + 14 * echelle;
            this.largeur = PDRectangle.A4.getWidth() - 2 * marge;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            fermer();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            flux = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - marge;
        }

        private void place(float hauteur) throws IOException {
            if (y - hauteur < marge) {
                nouvellePage();
            }
        }

        void espace(float points) {
            y -= points * echelle;
        }

        void ligne(String contenu, PDType1Font police, float taille) throws IOException {
            float t = taille * echelle;
            place(t + 3);
            ecrire(marge, y - t, LettrePdf.nettoyer(contenu == null ? "" : contenu, police), police, t, false);
            y -= t + 3 * echelle;
        }

        private void ecrire(float x, float yLigne, String contenu, PDType1Font police, float taille, boolean accent)
                throws IOException {
            if (contenu.isEmpty()) {
                return;
            }
            flux.beginText();
            if (accent) {
                flux.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
            }
            flux.setFont(police, taille);
            flux.newLineAtOffset(x, yLigne);
            flux.showText(contenu);
            flux.endText();
            if (accent) {
                flux.setNonStrokingColor(0f, 0f, 0f);
            }
        }

        /** taille : deja a l'echelle pour e.texte, sinon taille de reference multipliee ici. */
        void paragraphe(String contenu, PDType1Font police, float taille, boolean accent) throws IOException {
            float t = taille == texte ? taille : taille * echelle;
            for (String para : LettrePdf.nettoyer(contenu, police).split("\n")) {
                for (String l : LettrePdf.couper(para, police, largeur, t)) {
                    place(interligne);
                    ecrire(marge, y - t, l, police, t, accent);
                    y -= interligne * t / texte;
                }
            }
        }

        /** Titre de rubrique en capitales, souligne d'un trait. */
        void rubrique(String titre) throws IOException {
            float t = 11.5f * echelle;
            place(t * 3);
            y -= 10 * echelle;
            ecrire(marge, y - t, titre, gras, t, true);
            y -= t + 4 * echelle;
            flux.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
            flux.setLineWidth(0.8f);
            flux.moveTo(marge, y);
            flux.lineTo(marge + largeur, y);
            flux.stroke();
            y -= 5 * echelle;
        }

        /** Ligne en gras a gauche (poste, diplome, projet) et texte a droite (dates). */
        void entete(String gauche, String droite) throws IOException {
            place(interligne * 2);
            String d = vide(droite) ? "" : LettrePdf.nettoyer(droite, normal).strip();
            float largeurDroite = d.isEmpty() ? 0 : normal.getStringWidth(d) / 1000 * texte + 12;
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(vide(gauche) ? "" : gauche, gras), gras,
                    largeur - largeurDroite, texte);
            for (int i = 0; i < lignes.size(); i++) {
                ecrire(marge, y - texte, lignes.get(i), gras, texte, false);
                if (i == 0 && !d.isEmpty()) {
                    ecrire(marge + largeur - (largeurDroite - 12), y - texte, d, normal, texte, false);
                }
                y -= interligne;
            }
        }

        void puce(String contenu) throws IOException {
            float retrait = 14 * echelle;
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(contenu, normal), normal, largeur - retrait, texte);
            for (int i = 0; i < lignes.size(); i++) {
                place(interligne);
                if (i == 0) {
                    ecrire(marge + 2, y - texte, "-", normal, texte, false);
                }
                ecrire(marge + retrait, y - texte, lignes.get(i), normal, texte, false);
                y -= interligne;
            }
        }

        /** "Techniques : Java, Spring Boot..." avec le libelle en gras. */
        void libelle(String libelle, String contenu) throws IOException {
            float decalage = gras.getStringWidth(libelle) / 1000 * texte;
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(contenu, normal), normal, largeur - decalage, texte);
            for (int i = 0; i < lignes.size(); i++) {
                place(interligne);
                if (i == 0) {
                    ecrire(marge, y - texte, libelle, gras, texte, false);
                }
                ecrire(marge + decalage, y - texte, lignes.get(i), normal, texte, false);
                y -= interligne;
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
