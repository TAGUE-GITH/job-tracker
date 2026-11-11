package com.jobtracker.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * CV au format "ATS" : une seule colonne, vrai texte selectionnable, polices standard, titres de rubriques
 * classiques, puces simples, sans tableau. Plusieurs modeles changent l'apparence (couleurs, police, alignement)
 * sans changer l'ordre de lecture. Photo facultative en haut a droite. Le CV tient sur une page : si le contenu
 * deborde, la mise en page est resserree progressivement.
 */
public final class CvPdf {

    /** photo : image JPEG ou PNG, ou null. */
    public record Contact(String nom, String email, String telephone, String adresse, byte[] photo) {

        public Contact(String nom, String email, String telephone, String adresse) {
            this(nom, email, telephone, adresse, null);
        }
    }

    public record Experience(String poste, String entreprise, String lieu, String periode, List<String> missions) {
    }

    /** points : realisations detaillees (fonctionnalites, architecture, resultats). */
    public record Projet(String nom, String description, String technologies, List<String> points) {
    }

    public record Formation(String diplome, String etablissement, String periode, String details) {
    }

    /** modele : CLASSIQUE (defaut), MODERNE, ELEGANT ou COMPACT. avecPhoto : ajoute la photo du profil. */
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
            List<String> liens,
            String modele,
            Boolean avecPhoto) {

        public Cv(String titre, String profil, List<String> competencesTechniques, List<String> competencesTransverses,
                List<Experience> experiences, List<Projet> projets, List<Formation> formations, List<String> langues,
                List<String> atouts, List<String> centresInteret, List<String> liens) {
            this(titre, profil, competencesTechniques, competencesTransverses, experiences, projets, formations,
                    langues, atouts, centresInteret, liens, null, false);
        }
    }

    /** Apparence d'un modele de CV. Couleurs en RVB de 0 a 1. */
    record Style(String nom, Standard14Fonts.FontName police, Standard14Fonts.FontName policeGrasse, float[] accent,
            float tailleNom, float tailleTexte, float margeBase, boolean centre, Rubrique rubrique, boolean nomEnAccent) {
    }

    enum Rubrique { TRAIT_ACCENT, BARRE_GAUCHE, TRAIT_FIN_CENTRE, BANDEAU_GRIS }

    private static final float[] BLEU = {0.12f, 0.23f, 0.54f};
    private static final float[] BLEU_VIF = {0.11f, 0.31f, 0.85f};
    private static final float[] ANTHRACITE = {0.17f, 0.2f, 0.25f};
    private static final float[] VERT_SAPIN = {0.06f, 0.37f, 0.33f};

    public static final List<String> MODELES = List.of("CLASSIQUE", "MODERNE", "ELEGANT", "COMPACT");

    static Style style(String modele) {
        String m = modele == null ? "" : modele.toUpperCase(Locale.ROOT);
        return switch (m) {
            case "MODERNE" -> new Style(m, Standard14Fonts.FontName.HELVETICA, Standard14Fonts.FontName.HELVETICA_BOLD,
                    VERT_SAPIN, 21, 10.5f, 50, false, Rubrique.BARRE_GAUCHE, true);
            case "ELEGANT" -> new Style(m, Standard14Fonts.FontName.TIMES_ROMAN, Standard14Fonts.FontName.TIMES_BOLD,
                    ANTHRACITE, 21, 11.2f, 52, true, Rubrique.TRAIT_FIN_CENTRE, false);
            case "COMPACT" -> new Style(m, Standard14Fonts.FontName.HELVETICA, Standard14Fonts.FontName.HELVETICA_BOLD,
                    BLEU_VIF, 17, 9.8f, 38, false, Rubrique.BANDEAU_GRIS, false);
            default -> new Style("CLASSIQUE", Standard14Fonts.FontName.HELVETICA,
                    Standard14Fonts.FontName.HELVETICA_BOLD, BLEU, 18, 10.5f, 50, false, Rubrique.TRAIT_ACCENT, false);
        };
    }

    /** Facteurs de resserrement essayes dans l'ordre jusqu'a tenir sur une page. */
    private static final float[] ECHELLES = {1f, 0.95f, 0.9f, 0.85f, 0.8f, 0.76f, 0.72f, 0.68f};

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
        Style s = style(cv.modele());
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Ecrivain e = new Ecrivain(doc, echelle, s);
            boolean photo = Boolean.TRUE.equals(cv.avecPhoto()) && contact.photo() != null;
            if (photo) {
                e.photo(contact.photo());
            }

            e.titreNom(contact.nom());
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
            e.paragraphe(String.join("  |  ", coordonnees.stream().filter(x -> !vide(x)).toList()),
                    e.normal, 9.5f, false);
            e.finEnTete();

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

        final PDType1Font normal;
        final PDType1Font gras;
        final float echelle;
        final float texte;
        private final Style style;
        private final float interligne;
        private final float marge;
        private final float largeur;
        private final PDDocument doc;
        private PDPageContentStream flux;
        private float y;
        /** Place reservee a droite pour la photo pendant l'en-tete. */
        private float retraitDroit;
        private float basPhoto;

        Ecrivain(PDDocument doc, float echelle, Style style) throws IOException {
            this.doc = doc;
            this.echelle = echelle;
            this.style = style;
            this.normal = new PDType1Font(style.police());
            this.gras = new PDType1Font(style.policeGrasse());
            this.texte = style.tailleTexte() * echelle;
            this.interligne = style.tailleTexte() * 1.33f * echelle;
            this.marge = style.margeBase() * (0.72f + 0.28f * echelle);
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

        private float largeurUtile() {
            return largeur - retraitDroit;
        }

        void espace(float points) {
            y -= points * echelle;
        }

        /** Photo d'identite en haut a droite (format 3:4), l'en-tete s'ecrit a sa gauche. */
        void photo(byte[] image) throws IOException {
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, image, "photo");
            float h = 92 * echelle;
            float l = h * 0.78f;
            // Recadrage au centre pour garder le format 3:4 sans deformer
            float ratio = (float) img.getWidth() / img.getHeight();
            float dessinL = ratio > 0.78f ? h * ratio : l;
            float dessinH = ratio > 0.78f ? h : l / ratio;
            float x = marge + largeur - l;
            float haut = y + 4 * echelle;
            flux.saveGraphicsState();
            flux.addRect(x, haut - h, l, h);
            flux.clip();
            flux.drawImage(img, x - (dessinL - l) / 2, haut - h - (dessinH - h) / 2, dessinL, dessinH);
            flux.restoreGraphicsState();
            retraitDroit = l + 14 * echelle;
            basPhoto = haut - h;
        }

        /** Fin de l'en-tete : on passe sous la photo eventuelle et on reprend toute la largeur. */
        void finEnTete() {
            if (retraitDroit > 0) {
                y = Math.min(y, basPhoto - 2 * echelle);
                retraitDroit = 0;
            }
        }

        private boolean centrer() {
            return style.centre() && retraitDroit == 0;
        }

        void titreNom(String nom) throws IOException {
            float t = style.tailleNom() * echelle;
            place(t + 3);
            String n = LettrePdf.nettoyer(nom == null ? "" : nom, gras);
            if (style.centre()) {
                n = n.toUpperCase(Locale.ROOT);
            }
            float x = centrer() ? marge + (largeur - largeurTexte(n, gras, t)) / 2 : marge;
            ecrire(x, y - t, n, gras, t, style.nomEnAccent());
            y -= t + 3 * echelle;
        }

        private float largeurTexte(String s, PDType1Font police, float taille) throws IOException {
            return police.getStringWidth(s) / 1000 * taille;
        }

        private void couleur(boolean accent) throws IOException {
            float[] c = accent ? style.accent() : new float[] {0.08f, 0.09f, 0.11f};
            flux.setNonStrokingColor(c[0], c[1], c[2]);
        }

        private void ecrire(float x, float yLigne, String contenu, PDType1Font police, float taille, boolean accent)
                throws IOException {
            if (contenu.isEmpty()) {
                return;
            }
            flux.beginText();
            couleur(accent);
            flux.setFont(police, taille);
            flux.newLineAtOffset(x, yLigne);
            flux.showText(contenu);
            flux.endText();
        }

        /** taille : deja a l'echelle pour texte, sinon taille de reference multipliee ici. */
        void paragraphe(String contenu, PDType1Font police, float taille, boolean accent) throws IOException {
            float t = taille == texte ? taille : taille * echelle;
            for (String para : LettrePdf.nettoyer(contenu, police).split("\n")) {
                for (String l : LettrePdf.couper(para, police, largeurUtile(), t)) {
                    place(interligne);
                    float x = centrer() && taille != texte ? marge + (largeur - largeurTexte(l, police, t)) / 2 : marge;
                    ecrire(x, y - t, l, police, t, accent);
                    y -= interligne * t / texte;
                }
            }
        }

        /** Titre de rubrique en capitales, dans le style du modele. */
        void rubrique(String titre) throws IOException {
            float t = (style.tailleTexte() + 1) * echelle;
            place(t * 3);
            y -= 10 * echelle;
            float[] a = style.accent();
            switch (style.rubrique()) {
                case BARRE_GAUCHE -> {
                    flux.setNonStrokingColor(a[0], a[1], a[2]);
                    flux.addRect(marge, y - t - 1, 3 * echelle, t + 2);
                    flux.fill();
                    ecrire(marge + 8 * echelle, y - t, titre, gras, t, false);
                    y -= t + 4 * echelle;
                    trait(0.82f, 0.5f);
                }
                case TRAIT_FIN_CENTRE -> {
                    float l = largeurTexte(titre, gras, t);
                    ecrire(marge + (largeur - l) / 2, y - t, titre, gras, t, true);
                    y -= t + 4 * echelle;
                    trait(0.35f, 0.5f);
                }
                case BANDEAU_GRIS -> {
                    flux.setNonStrokingColor(0.93f, 0.94f, 0.96f);
                    flux.addRect(marge, y - t - 3 * echelle, largeur, t + 6 * echelle);
                    flux.fill();
                    ecrire(marge + 5 * echelle, y - t, titre, gras, t, true);
                    y -= t + 6 * echelle;
                }
                default -> {
                    ecrire(marge, y - t, titre, gras, t, true);
                    y -= t + 4 * echelle;
                    flux.setStrokingColor(a[0], a[1], a[2]);
                    flux.setLineWidth(0.8f);
                    flux.moveTo(marge, y);
                    flux.lineTo(marge + largeur, y);
                    flux.stroke();
                }
            }
            y -= 5 * echelle;
        }

        private void trait(float gris, float epaisseur) throws IOException {
            flux.setStrokingColor(gris, gris, gris);
            flux.setLineWidth(epaisseur);
            flux.moveTo(marge, y);
            flux.lineTo(marge + largeur, y);
            flux.stroke();
        }

        /** Ecrit sur une meme ligne, dans un seul bloc de texte, des morceaux de polices differentes. */
        private void ecrireMorceaux(float x, String... morceaux) throws IOException {
            flux.beginText();
            couleur(false);
            flux.newLineAtOffset(x, y - texte);
            for (int i = 0; i + 1 < morceaux.length; i += 2) {
                if (!morceaux[i + 1].isEmpty()) {
                    flux.setFont("g".equals(morceaux[i]) ? gras : normal, texte);
                    flux.showText(morceaux[i + 1]);
                }
            }
            flux.endText();
        }

        private float largeurTexte(String s, PDType1Font police) throws IOException {
            return largeurTexte(s, police, texte);
        }

        /**
         * "Poste - Entreprise | Lieu | Periode" sur la meme ligne, dans l'ordre de lecture : les ATS lisent mal
         * les dates calees a droite (elles finissent souvent a la fin du CV). Si c'est trop long, le lieu et la
         * periode passent a la ligne suivante.
         */
        void entete(String gauche, String droite) throws IOException {
            place(interligne * 2);
            String g = LettrePdf.nettoyer(vide(gauche) ? "" : gauche, gras).strip();
            String d = vide(droite) ? "" : "  |  " + LettrePdf.nettoyer(droite, normal).strip();
            if (largeurTexte(g, gras) + largeurTexte(d, normal) <= largeur) {
                ecrireMorceaux(marge, "g", g, "n", d);
                y -= interligne;
                return;
            }
            for (String l : LettrePdf.couper(g, gras, largeur, texte)) {
                place(interligne);
                ecrireMorceaux(marge, "g", l);
                y -= interligne;
            }
            if (!d.isEmpty()) {
                place(interligne);
                ecrireMorceaux(marge, "n", d.substring(5));
                y -= interligne;
            }
        }

        /** Puce "•" et texte dans le meme bloc : chaque mission reste une ligne a part pour les ATS. */
        void puce(String contenu) throws IOException {
            String puce = "•  ";
            float retrait = 4 + largeurTexte(puce, normal);
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(contenu, normal), normal, largeur - retrait, texte);
            for (int i = 0; i < lignes.size(); i++) {
                place(interligne);
                if (i == 0) {
                    ecrireMorceaux(marge + 4, "n", puce + lignes.get(i));
                } else {
                    ecrireMorceaux(marge + retrait, "n", lignes.get(i));
                }
                y -= interligne;
            }
        }

        /** "Techniques : Java, Spring Boot..." avec le libelle en gras, dans le meme bloc de texte. */
        void libelle(String libelle, String contenu) throws IOException {
            float decalage = largeurTexte(libelle, gras);
            List<String> lignes = LettrePdf.couper(LettrePdf.nettoyer(contenu, normal), normal, largeur - decalage, texte);
            for (int i = 0; i < lignes.size(); i++) {
                place(interligne);
                if (i == 0) {
                    ecrireMorceaux(marge, "g", libelle, "n", lignes.get(i));
                } else {
                    ecrireMorceaux(marge + decalage, "n", lignes.get(i));
                }
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
