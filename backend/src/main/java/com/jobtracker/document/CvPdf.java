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
 * CV en PDF, en vrai texte selectionnable (lisible par les logiciels de tri ATS), selon 4 modeles :
 * <ul>
 * <li>CLASSIQUE : nom centre, formation en tete, entreprise et lieu / poste et dates aux extremites des lignes ;</li>
 * <li>MODERNE : dates et lieux dans une colonne a gauche, photo carree a droite, titres soulignes ;</li>
 * <li>ELEGANT : colonne laterale beige (contact, profil, competences), nom en grandes capitales ;</li>
 * <li>COMPACT : bande grise a gauche, photo ronde, titres en gras, rubriques en paragraphes.</li>
 * </ul>
 * Le CV tient sur une page : si le contenu deborde, la mise en page est resserree progressivement.
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

    /** lieu : ville de l'etablissement (facultatif). */
    public record Formation(String diplome, String etablissement, String periode, String details, String lieu) {

        public Formation(String diplome, String etablissement, String periode, String details) {
            this(diplome, etablissement, periode, details, null);
        }
    }

    /** Rubrique libre du CV (certifications, benevolat, activites...) : un titre et ses elements. */
    public record Rubrique(String titre, List<String> elements) {
    }

    /**
     * modele : CLASSIQUE (defaut), MODERNE, ELEGANT ou COMPACT. avecPhoto : ajoute la photo du profil.
     * autresRubriques : rubriques libres, ajoutees apres les rubriques principales.
     */
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
            Boolean avecPhoto,
            List<Rubrique> autresRubriques) {

        public Cv(String titre, String profil, List<String> competencesTechniques, List<String> competencesTransverses,
                List<Experience> experiences, List<Projet> projets, List<Formation> formations, List<String> langues,
                List<String> atouts, List<String> centresInteret, List<String> liens, String modele, Boolean avecPhoto) {
            this(titre, profil, competencesTechniques, competencesTransverses, experiences, projets, formations,
                    langues, atouts, centresInteret, liens, modele, avecPhoto, null);
        }

        public Cv(String titre, String profil, List<String> competencesTechniques, List<String> competencesTransverses,
                List<Experience> experiences, List<Projet> projets, List<Formation> formations, List<String> langues,
                List<String> atouts, List<String> centresInteret, List<String> liens) {
            this(titre, profil, competencesTechniques, competencesTransverses, experiences, projets, formations,
                    langues, atouts, centresInteret, liens, null, false, null);
        }
    }

    public static final List<String> MODELES = List.of("CLASSIQUE", "MODERNE", "ELEGANT", "COMPACT");

    static final float[] NOIR = {0.09f, 0.09f, 0.1f};
    static final float[] GRIS = {0.45f, 0.46f, 0.49f};
    static final float[] BEIGE = {0.953f, 0.914f, 0.863f};
    static final float[] OCRE = {0.83f, 0.6f, 0.36f};
    static final float[] GRIS_BANDE = {0.9f, 0.9f, 0.91f};
    /** Liens cliquables : bleu vif, souligne. */
    static final float[] LIEN = {0.04f, 0.36f, 0.85f};
    /** Couleur des titres de rubrique et de l'intitule de poste, par modele (assez foncees pour bien imprimer). */
    static final float[] BLEU_NUIT = {0.1f, 0.23f, 0.47f};
    static final float[] PETROLE = {0.03f, 0.38f, 0.45f};
    static final float[] BRUN = {0.5f, 0.31f, 0.13f};
    static final float[] BLEU_ROI = {0.12f, 0.31f, 0.66f};

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    /** Largeur de la colonne beige du modele ELEGANT. */
    private static final float LARGEUR_COTE = 200;

    /** Facteurs de resserrement essayes dans l'ordre jusqu'a tenir sur une page. */
    private static final float[] ECHELLES = {1f, 0.95f, 0.9f, 0.86f, 0.82f, 0.78f, 0.74f, 0.7f, 0.66f};

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

    static String modele(String modele) {
        String m = modele == null ? "" : modele.toUpperCase(Locale.ROOT);
        return MODELES.contains(m) ? m : "CLASSIQUE";
    }

    private record Rendu(byte[] pdf, int pages) {
    }

    private static Rendu rendre(Contact contact, Cv cv, float echelle) {
        String modele = modele(cv.modele());
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Toile t = new Toile(doc, echelle, switch (modele) {
                case "ELEGANT" -> f -> rectangle(f, 0, 0, LARGEUR_COTE, HAUTEUR_PAGE, BEIGE);
                case "COMPACT" -> f -> rectangle(f, 0, 0, 20, HAUTEUR_PAGE, GRIS_BANDE);
                default -> f -> { };
            });
            byte[] photo = Boolean.TRUE.equals(cv.avecPhoto()) ? contact.photo() : null;
            switch (modele) {
                case "MODERNE" -> moderne(t, contact, cv, photo);
                case "ELEGANT" -> elegant(t, contact, cv, photo);
                case "COMPACT" -> compact(t, contact, cv, photo);
                default -> classique(t, contact, cv);
            }
            t.fermer();
            doc.save(sortie);
            return new Rendu(sortie.toByteArray(), doc.getNumberOfPages());
        } catch (IOException ex) {
            throw new UncheckedIOException("Impossible de generer le CV", ex);
        }
    }

    // ------------------------------------------------------------------ CLASSIQUE

    /** Nom centre en capitales, coordonnees sur une ligne, formation en premier, dates a droite. */
    private static void classique(Toile t, Contact c, Cv cv) throws IOException {
        float e = t.echelle;
        float marge = t.marge(46);
        Colonne col = t.colonne(marge, LARGEUR_PAGE - 2 * marge, t.haut(40));
        float tx = 10.2f * e;
        float il = tx * 1.3f;

        col.riche(List.of(t.g(NOIR, majuscules(c.nom()))), 20 * e, 24 * e, Aligne.CENTRE);
        if (!vide(cv.titre())) {
            col.riche(List.of(t.g(BLEU_NUIT, cv.titre())), 11 * e, 14 * e, Aligne.CENTRE);
        }
        List<String> coordonnees = new ArrayList<>();
        coordonnees.add(une(c.adresse()));
        coordonnees.add(c.telephone());
        coordonnees.add(c.email());
        coordonnees.addAll(liste(cv.liens()));
        List<Morceau> ligneContact = new ArrayList<>();
        for (String element : coordonnees) {
            if (!vide(element)) {
                if (!ligneContact.isEmpty()) {
                    ligneContact.add(t.n(NOIR, " | "));
                }
                ligneContact.add(t.contact(element));
            }
        }
        col.riche(ligneContact, 9.6f * e, 12.5f * e, Aligne.CENTRE);

        if (!vide(cv.profil())) {
            rubriqueClassique(t, col, "PROFIL");
            col.riche(List.of(t.n(NOIR, cv.profil())), tx, il, Aligne.GAUCHE);
        }
        if (!liste(cv.formations()).isEmpty()) {
            rubriqueClassique(t, col, "FORMATION");
            for (Formation f : liste(cv.formations())) {
                col.place(il * 2);
                if (!vide(f.etablissement())) {
                    col.ligne(List.of(t.g(NOIR, majuscules(f.etablissement()))), t.n(NOIR, f.lieu()), tx, il);
                    col.ligne(List.of(t.n(GRIS, f.diplome())), t.n(NOIR, f.periode()), tx, il);
                } else {
                    col.ligne(List.of(t.g(NOIR, majuscules(f.diplome()))), t.n(NOIR, f.periode()), tx, il);
                }
                if (!vide(f.details())) {
                    col.riche(List.of(t.n(NOIR, f.details())), tx, il, Aligne.GAUCHE);
                }
                col.espace(5);
            }
        }
        if (!liste(cv.experiences()).isEmpty()) {
            rubriqueClassique(t, col, "EXPÉRIENCE PROFESSIONNELLE");
            for (Experience x : liste(cv.experiences())) {
                col.place(il * 3);
                if (!vide(x.entreprise())) {
                    col.ligne(List.of(t.g(NOIR, majuscules(x.entreprise()))), t.n(NOIR, x.lieu()), tx, il);
                    col.ligne(List.of(t.n(NOIR, x.poste())), t.n(NOIR, x.periode()), tx, il);
                } else {
                    col.ligne(List.of(t.g(NOIR, x.poste())), t.n(NOIR, joindre(" | ", x.lieu(), x.periode())), tx, il);
                }
                for (String mission : liste(x.missions())) {
                    col.puce(t, mission, tx, il, 6 * e);
                }
                col.espace(5);
            }
        }
        if (!liste(cv.projets()).isEmpty()) {
            rubriqueClassique(t, col, liste(cv.projets()).size() > 1 ? "PROJETS" : "PROJET");
            for (Projet p : liste(cv.projets())) {
                col.place(il * 3);
                col.riche(List.of(t.g(NOIR, majuscules(p.nom()))), tx, il, Aligne.GAUCHE);
                if (!vide(p.description())) {
                    col.riche(List.of(t.n(NOIR, p.description())), tx, il, Aligne.GAUCHE);
                }
                for (String point : liste(p.points())) {
                    col.puce(t, point, tx, il, 6 * e);
                }
                if (!vide(p.technologies())) {
                    col.riche(List.of(t.g(NOIR, "Technologies : "), t.n(NOIR, p.technologies())), tx, il, Aligne.GAUCHE);
                }
                col.espace(5);
            }
        }
        for (Rubrique r : rubriques(cv)) {
            rubriqueClassique(t, col, majuscules(r.titre()));
            for (String element : liste(r.elements())) {
                col.puce(t, element, tx, il, 6 * e);
            }
        }
        List<List<Morceau>> infos = new ArrayList<>();
        ajouterLibelle(t, infos, "Compétences techniques : ", cv.competencesTechniques(), ", ");
        ajouterLibelle(t, infos, "Compétences transverses : ", cv.competencesTransverses(), ", ");
        ajouterLibelle(t, infos, "Langues : ", cv.langues(), " ; ");
        ajouterLibelle(t, infos, "Atouts : ", cv.atouts(), ", ");
        ajouterLibelle(t, infos, "Centres d'intérêt : ", cv.centresInteret(), ", ");
        if (!infos.isEmpty()) {
            rubriqueClassique(t, col, "COMPÉTENCES ET INFORMATIONS");
            for (List<Morceau> ligne : infos) {
                col.riche(ligne, tx, il, Aligne.GAUCHE);
                col.espace(1.5f);
            }
        }
    }

    private static void ajouterLibelle(Toile t, List<List<Morceau>> lignes, String libelle, List<String> elements,
            String separateur) {
        if (!liste(elements).isEmpty()) {
            lignes.add(List.of(t.g(NOIR, libelle), t.n(NOIR, String.join(separateur, liste(elements)))));
        }
    }

    private static void rubriqueClassique(Toile t, Colonne col, String titre) throws IOException {
        float e = t.echelle;
        float taille = 11.5f * e;
        col.place(taille * 4);
        col.espace(9);
        col.riche(List.of(t.g(BLEU_NUIT, titre)), taille, taille + 3 * e, Aligne.GAUCHE);
        col.trait(BLEU_NUIT, 0.8f);
        col.espace(5);
    }

    // ------------------------------------------------------------------ MODERNE

    /** Nom et coordonnees a gauche, photo carree a droite ; dates et lieux dans une colonne a gauche. */
    private static void moderne(Toile t, Contact c, Cv cv, byte[] photo) throws IOException {
        float e = t.echelle;
        float marge = t.marge(42);
        float largeur = LARGEUR_PAGE - 2 * marge;
        Colonne col = t.colonne(marge, largeur, t.haut(40));
        float tx = 10 * e;
        float il = tx * 1.38f;

        float basPhoto = col.y;
        if (photo != null) {
            float cote = 118 * e;
            t.image(col.page, photo, marge + largeur - cote, col.y - cote, cote, cote, false);
            col.retrait(cote + 18 * e, col.y - cote);
            basPhoto = col.y - cote;
        }
        col.riche(List.of(t.g(NOIR, c.nom())), 23 * e, 27 * e, Aligne.GAUCHE);
        if (!vide(cv.titre())) {
            col.riche(List.of(t.g(PETROLE, cv.titre())), 12.5f * e, 16 * e, Aligne.GAUCHE);
        }
        col.espace(11);
        List<String> coordonnees = new ArrayList<>(List.of(texte(c.email()), texte(c.telephone()), une(c.adresse())));
        coordonnees.addAll(liste(cv.liens()));
        for (String ligne : coordonnees) {
            if (!vide(ligne)) {
                col.riche(List.of(t.contact(ligne)), tx, il, Aligne.GAUCHE);
            }
        }
        col.y = Math.min(col.y, basPhoto - 4 * e);
        col.retrait(0, 0);

        if (!vide(cv.profil())) {
            rubriqueModerne(t, col, "PROFIL PROFESSIONNEL");
            col.riche(List.of(t.n(NOIR, cv.profil())), tx, il, Aligne.GAUCHE);
        }
        float gauche = largeur * 0.245f;
        float decalage = largeur * 0.255f;
        if (!liste(cv.experiences()).isEmpty()) {
            rubriqueModerne(t, col, "EXPÉRIENCES PROFESSIONNELLES");
            for (Experience x : liste(cv.experiences())) {
                col.place(il * 3);
                Colonne g = col.sous(0, gauche);
                Colonne d = col.sous(decalage, largeur - decalage);
                g.riche(List.of(t.n(NOIR, x.periode())), tx, il, Aligne.GAUCHE);
                g.riche(List.of(t.n(GRIS, x.lieu())), tx, il, Aligne.GAUCHE);
                d.riche(List.of(t.g(NOIR, joindre(" - ", x.poste(), x.entreprise()))), tx, il, Aligne.GAUCHE);
                d.espace(1.5f);
                for (String mission : liste(x.missions())) {
                    d.puce(t, mission, tx, il, 6 * e);
                }
                col.apres(g, d);
                col.espace(9);
            }
        }
        if (!liste(cv.projets()).isEmpty()) {
            rubriqueModerne(t, col, liste(cv.projets()).size() > 1 ? "PROJETS PERSONNELS" : "PROJET PERSONNEL");
            for (Projet p : liste(cv.projets())) {
                col.place(il * 3);
                Colonne g = col.sous(0, gauche);
                Colonne d = col.sous(decalage, largeur - decalage);
                g.riche(List.of(t.n(GRIS, "Projet")), tx, il, Aligne.GAUCHE);
                d.riche(List.of(t.g(NOIR, p.nom())), tx, il, Aligne.GAUCHE);
                if (!vide(p.description())) {
                    d.riche(List.of(t.n(NOIR, p.description())), tx, il, Aligne.GAUCHE);
                }
                for (String point : liste(p.points())) {
                    d.puce(t, point, tx, il, 6 * e);
                }
                if (!vide(p.technologies())) {
                    d.riche(List.of(t.g(NOIR, "Technologies : "), t.n(NOIR, p.technologies())), tx, il, Aligne.GAUCHE);
                }
                col.apres(g, d);
                col.espace(9);
            }
        }
        if (!liste(cv.formations()).isEmpty()) {
            rubriqueModerne(t, col, "FORMATION");
            for (Formation f : liste(cv.formations())) {
                col.place(il * 2);
                Colonne g = col.sous(0, gauche);
                Colonne d = col.sous(decalage, largeur - decalage);
                g.riche(List.of(t.n(NOIR, f.periode())), tx, il, Aligne.GAUCHE);
                g.riche(List.of(t.n(GRIS, f.lieu())), tx, il, Aligne.GAUCHE);
                d.riche(List.of(t.g(NOIR, f.diplome())), tx, il, Aligne.GAUCHE);
                d.riche(List.of(t.n(NOIR, f.etablissement())), tx, il, Aligne.GAUCHE);
                if (!vide(f.details())) {
                    d.riche(List.of(t.n(NOIR, f.details())), tx, il, Aligne.GAUCHE);
                }
                col.apres(g, d);
                col.espace(8);
            }
        }
        for (Rubrique r : rubriques(cv)) {
            rubriqueModerne(t, col, majuscules(r.titre()));
            for (String element : liste(r.elements())) {
                col.puce(t, element, tx, il, 0);
            }
        }
        List<String> competences = new ArrayList<>(liste(cv.competencesTechniques()));
        competences.addAll(liste(cv.competencesTransverses()));
        grille(t, col, "COMPÉTENCES", competences, tx, il);
        grille(t, col, "LANGUES", liste(cv.langues()).stream().map(l -> l.replace(" : ", " | ")).toList(), tx, il);
        grille(t, col, "ATOUTS", liste(cv.atouts()), tx, il);
        grille(t, col, "CENTRES D'INTÉRÊT", liste(cv.centresInteret()), tx, il);
    }

    /** Elements ranges sur 3 colonnes. */
    private static void grille(Toile t, Colonne col, String titre, List<String> elements, float tx, float il)
            throws IOException {
        if (elements.isEmpty()) {
            return;
        }
        rubriqueModerne(t, col, titre);
        float cellule = col.largeur / 3;
        for (int i = 0; i < elements.size(); i += 3) {
            col.place(il);
            List<Colonne> ligne = new ArrayList<>();
            for (int j = i; j < Math.min(i + 3, elements.size()); j++) {
                Colonne c = col.sous((j - i) * cellule, cellule - 10 * t.echelle);
                c.riche(List.of(t.n(NOIR, elements.get(j))), tx, il, Aligne.GAUCHE);
                ligne.add(c);
            }
            col.apres(ligne.toArray(Colonne[]::new));
            col.espace(4);
        }
    }

    private static void rubriqueModerne(Toile t, Colonne col, String titre) throws IOException {
        float e = t.echelle;
        float taille = 10.5f * e;
        col.place(taille * 4);
        col.espace(11);
        col.riche(List.of(t.g(PETROLE, titre)), taille, taille + 3 * e, Aligne.GAUCHE);
        col.trait(PETROLE, 1.4f);
        col.espace(7);
    }

    // ------------------------------------------------------------------ ELEGANT

    /** Colonne laterale beige (contact, profil, competences...) et colonne principale (experiences, formation). */
    private static void elegant(Toile t, Contact c, Cv cv, byte[] photo) throws IOException {
        float e = t.echelle;
        float xPrincipal = LARGEUR_COTE + 26;
        Colonne col = t.colonne(xPrincipal, LARGEUR_PAGE - xPrincipal - 32, t.haut(42));
        float tx = 9.6f * e;
        float il = tx * 1.36f;

        // Colonne principale d'abord : les logiciels de tri lisent le nom et les experiences en premier
        col.riche(List.of(t.g(NOIR, majuscules(c.nom()))), 28 * e, 31 * e, Aligne.GAUCHE);
        if (!vide(cv.titre())) {
            col.espace(2);
            col.riche(List.of(t.n(BRUN, majuscules(cv.titre()))), 12.5f * e, 16 * e, Aligne.GAUCHE);
        }
        if (!liste(cv.experiences()).isEmpty()) {
            rubriqueElegante(t, col, "EXPÉRIENCES PROFESSIONNELLES", 14.5f);
            for (Experience x : liste(cv.experiences())) {
                col.place(il * 3);
                col.riche(List.of(t.g(NOIR, texte(x.poste()) + (vide(x.periode()) ? "" : ", ")),
                        t.n(OCRE, x.periode())), 10.4f * e, il, Aligne.GAUCHE);
                col.riche(List.of(t.i(NOIR, x.entreprise()), t.n(NOIR, vide(x.lieu()) ? "" : ", " + x.lieu())),
                        tx, il, Aligne.GAUCHE);
                for (String mission : liste(x.missions())) {
                    col.puce(t, mission, tx, il, 0);
                }
                col.espace(8);
            }
        }
        if (!liste(cv.projets()).isEmpty()) {
            rubriqueElegante(t, col, liste(cv.projets()).size() > 1 ? "PROJETS PERSONNELS" : "PROJET PERSONNEL", 14.5f);
            for (Projet p : liste(cv.projets())) {
                col.place(il * 3);
                col.riche(List.of(t.g(NOIR, p.nom())), 10.4f * e, il, Aligne.GAUCHE);
                if (!vide(p.description())) {
                    col.riche(List.of(t.i(NOIR, p.description())), tx, il, Aligne.GAUCHE);
                }
                for (String point : liste(p.points())) {
                    col.puce(t, point, tx, il, 0);
                }
                if (!vide(p.technologies())) {
                    col.riche(List.of(t.g(NOIR, "Technologies : "), t.n(NOIR, p.technologies())), tx, il, Aligne.GAUCHE);
                }
                col.espace(8);
            }
        }
        if (!liste(cv.formations()).isEmpty()) {
            rubriqueElegante(t, col, "FORMATION", 14.5f);
            for (Formation f : liste(cv.formations())) {
                col.place(il * 2);
                col.riche(List.of(t.g(NOIR, texte(f.diplome()) + (vide(f.periode()) ? "" : ", ")),
                        t.n(OCRE, f.periode())), 10.4f * e, il, Aligne.GAUCHE);
                col.riche(List.of(t.i(NOIR, f.etablissement()), t.n(NOIR, vide(f.lieu()) ? "" : ", " + f.lieu())),
                        tx, il, Aligne.GAUCHE);
                if (!vide(f.details())) {
                    col.riche(List.of(t.n(NOIR, f.details())), tx, il, Aligne.GAUCHE);
                }
                col.espace(7);
            }
        }

        for (Rubrique r : rubriques(cv)) {
            rubriqueElegante(t, col, majuscules(r.titre()), 14.5f);
            for (String element : liste(r.elements())) {
                col.puce(t, element, tx, il, 0);
            }
        }

        // Colonne beige
        Colonne cote = t.colonne(22, LARGEUR_COTE - 42, t.haut(42));
        if (photo != null) {
            float d = 104 * e;
            t.image(0, photo, (LARGEUR_COTE - d) / 2, cote.y - d, d, d, true);
            cote.y -= d + 4 * e;
        }
        float ts = 9.3f * e;
        float ils = ts * 1.4f;
        rubriqueElegante(t, cote, "CONTACT", 13);
        List<String> coordonnees = new ArrayList<>(List.of(texte(c.telephone()), une(c.adresse()), texte(c.email())));
        coordonnees.addAll(liste(cv.liens()));
        for (String ligne : coordonnees) {
            if (!vide(ligne)) {
                cote.riche(List.of(t.contact(ligne)), ts, ils, Aligne.GAUCHE);
                cote.espace(2);
            }
        }
        if (!vide(cv.profil())) {
            rubriqueElegante(t, cote, "PROFIL", 13);
            cote.riche(List.of(t.n(NOIR, cv.profil())), ts, ils, Aligne.GAUCHE);
        }
        listeCote(t, cote, "COMPÉTENCES", cv.competencesTechniques(), ts, ils);
        listeCote(t, cote, "SAVOIR-ÊTRE", cv.competencesTransverses(), ts, ils);
        listeCote(t, cote, "LANGUES", cv.langues(), ts, ils);
        listeCote(t, cote, "ATOUTS", cv.atouts(), ts, ils);
        listeCote(t, cote, "CENTRES D'INTÉRÊT", cv.centresInteret(), ts, ils);
    }

    private static void listeCote(Toile t, Colonne col, String titre, List<String> elements, float ts, float ils)
            throws IOException {
        if (!liste(elements).isEmpty()) {
            rubriqueElegante(t, col, titre, 13);
            for (String element : liste(elements)) {
                col.puce(t, element, ts, ils, 0);
            }
        }
    }

    private static void rubriqueElegante(Toile t, Colonne col, String titre, float taille) throws IOException {
        float e = t.echelle;
        float tr = taille * e;
        col.place(tr * 3.5f);
        col.espace(14);
        col.riche(List.of(t.g(BRUN, titre)), tr, tr + 3 * e, Aligne.GAUCHE);
        col.espace(6);
    }

    // ------------------------------------------------------------------ COMPACT

    /** Bande grise a gauche, photo ronde a droite, titres en gras sans trait, competences en paragraphe. */
    private static void compact(Toile t, Contact c, Cv cv, byte[] photo) throws IOException {
        float e = t.echelle;
        float x = 52;
        Colonne col = t.colonne(x, LARGEUR_PAGE - x - t.marge(40), t.haut(38));
        float tx = 10 * e;
        float il = tx * 1.36f;

        float basPhoto = col.y;
        if (photo != null) {
            float d = 124 * e;
            t.image(col.page, photo, x + col.largeur - d, col.y - d, d, d, true);
            col.retrait(d + 18 * e, col.y - d);
            basPhoto = col.y - d;
        }
        col.riche(List.of(t.g(NOIR, nomFamilleEnCapitales(c.nom()))), 25 * e, 29 * e, Aligne.GAUCHE);
        if (!vide(cv.titre())) {
            col.riche(List.of(t.n(BLEU_ROI, majuscules(cv.titre()))), 16.5f * e, 21 * e, Aligne.GAUCHE);
        }
        col.espace(9);
        List<String> coordonnees = new ArrayList<>(List.of(texte(c.email()), texte(c.telephone())));
        coordonnees.addAll(liste(cv.liens()));
        coordonnees.add(une(c.adresse()));
        for (String ligne : coordonnees) {
            if (!vide(ligne)) {
                col.riche(List.of(t.contact(ligne)), tx, tx * 1.3f, Aligne.GAUCHE);
            }
        }
        if (!vide(cv.profil())) {
            col.espace(10);
            col.riche(List.of(t.n(NOIR, cv.profil())), tx, il, Aligne.GAUCHE);
        }
        col.y = Math.min(col.y, basPhoto - 2 * e);
        col.retrait(0, 0);

        if (!liste(cv.experiences()).isEmpty()) {
            rubriqueCompacte(t, col, "EXPÉRIENCES PROFESSIONNELLES");
            for (Experience xp : liste(cv.experiences())) {
                col.place(il * 3);
                col.riche(List.of(t.g(NOIR, xp.poste())), 10.6f * e, il, Aligne.GAUCHE);
                col.riche(List.of(t.i(NOIR, xp.entreprise()),
                        t.n(NOIR, vide(xp.lieu()) ? "" : (vide(xp.entreprise()) ? "" : ", ") + xp.lieu()),
                        t.n(GRIS, vide(xp.periode()) ? "" : " | " + xp.periode())), tx, il, Aligne.GAUCHE);
                for (String mission : liste(xp.missions())) {
                    col.puce(t, mission, tx, il, 2 * e);
                }
                col.espace(7);
            }
        }
        if (!liste(cv.projets()).isEmpty()) {
            rubriqueCompacte(t, col, liste(cv.projets()).size() > 1 ? "PROJETS PERSONNELS" : "PROJET PERSONNEL");
            for (Projet p : liste(cv.projets())) {
                col.place(il * 3);
                col.riche(List.of(t.g(NOIR, p.nom())), 10.6f * e, il, Aligne.GAUCHE);
                if (!vide(p.technologies())) {
                    col.riche(List.of(t.i(NOIR, p.technologies())), tx, il, Aligne.GAUCHE);
                }
                if (!vide(p.description())) {
                    col.puce(t, p.description(), tx, il, 2 * e);
                }
                for (String point : liste(p.points())) {
                    col.puce(t, point, tx, il, 2 * e);
                }
                col.espace(7);
            }
        }
        if (!liste(cv.formations()).isEmpty()) {
            rubriqueCompacte(t, col, "FORMATION");
            for (Formation f : liste(cv.formations())) {
                col.place(il * 2);
                List<Morceau> ligne = new ArrayList<>();
                ligne.add(t.g(NOIR, f.diplome()));
                if (!vide(f.etablissement())) {
                    ligne.add(t.n(NOIR, ", "));
                    ligne.add(t.i(NOIR, f.etablissement()));
                }
                if (!vide(f.lieu())) {
                    ligne.add(t.n(NOIR, ", " + f.lieu()));
                }
                if (!vide(f.periode())) {
                    ligne.add(t.n(GRIS, " | " + f.periode()));
                }
                col.riche(ligne, tx, il, Aligne.GAUCHE);
                if (!vide(f.details())) {
                    col.puce(t, f.details(), tx, il, 2 * e);
                }
                col.espace(6);
            }
        }
        for (Rubrique r : rubriques(cv)) {
            rubriqueCompacte(t, col, majuscules(r.titre()));
            for (String element : liste(r.elements())) {
                col.puce(t, element, tx, il, 2 * e);
            }
        }
        List<String> competences = new ArrayList<>(liste(cv.competencesTechniques()));
        competences.addAll(liste(cv.competencesTransverses()));
        paragrapheCompact(t, col, "COMPÉTENCES", competences, tx, il);
        if (!liste(cv.langues()).isEmpty()) {
            rubriqueCompacte(t, col, "LANGUES");
            List<Morceau> ligne = new ArrayList<>();
            for (String langue : liste(cv.langues())) {
                if (!ligne.isEmpty()) {
                    ligne.add(t.n(NOIR, " | "));
                }
                int deuxPoints = langue.indexOf(':');
                if (deuxPoints > 0) {
                    ligne.add(t.g(NOIR, langue.substring(0, deuxPoints).strip()));
                    ligne.add(t.n(NOIR, " : " + langue.substring(deuxPoints + 1).strip()));
                } else {
                    ligne.add(t.g(NOIR, langue));
                }
            }
            col.riche(ligne, tx, il, Aligne.GAUCHE);
        }
        paragrapheCompact(t, col, "ATOUTS", liste(cv.atouts()), tx, il);
        paragrapheCompact(t, col, "CENTRES D'INTÉRÊT", liste(cv.centresInteret()), tx, il);
    }

    private static void paragrapheCompact(Toile t, Colonne col, String titre, List<String> elements, float tx, float il)
            throws IOException {
        if (!elements.isEmpty()) {
            rubriqueCompacte(t, col, titre);
            col.riche(List.of(t.n(NOIR, String.join(", ", elements) + ".")), tx, il, Aligne.GAUCHE);
        }
    }

    private static void rubriqueCompacte(Toile t, Colonne col, String titre) throws IOException {
        float e = t.echelle;
        float taille = 15 * e;
        col.place(taille * 3.5f);
        col.espace(13);
        col.riche(List.of(t.g(BLEU_ROI, titre)), taille, taille + 4 * e, Aligne.GAUCHE);
        col.espace(4);
    }

    // ------------------------------------------------------------------ Outils

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l.stream()
                .filter(Objects::nonNull)
                .filter(x -> !(x instanceof String s) || !s.isBlank())
                .toList();
    }

    /** Rubriques libres qui ont un titre et au moins un element. */
    private static List<Rubrique> rubriques(Cv cv) {
        return liste(cv.autresRubriques()).stream()
                .filter(r -> !vide(r.titre()) && !liste(r.elements()).isEmpty())
                .toList();
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String texte(String s) {
        return s == null ? "" : s.strip();
    }

    /** Adresse sur une seule ligne. */
    private static String une(String s) {
        return vide(s) ? "" : String.join(", ", Arrays.stream(s.split("\n")).map(String::strip)
                .filter(x -> !x.isEmpty()).toList());
    }

    private static String majuscules(String s) {
        return texte(s).toUpperCase(Locale.FRENCH);
    }

    /** "Franck Tague" -> "Franck TAGUE" (dernier mot en capitales). */
    static String nomFamilleEnCapitales(String nom) {
        String n = texte(nom);
        int espace = n.lastIndexOf(' ');
        return espace < 0 ? n : n.substring(0, espace + 1) + n.substring(espace + 1).toUpperCase(Locale.FRENCH);
    }

    private static String joindre(String separateur, String... parties) {
        return String.join(separateur, Arrays.stream(parties).filter(p -> !vide(p)).map(String::strip).toList());
    }

    private static void rectangle(PDPageContentStream f, float x, float y, float l, float h, float[] c)
            throws IOException {
        f.setNonStrokingColor(c[0], c[1], c[2]);
        f.addRect(x, y, l, h);
        f.fill();
    }

    enum Aligne { GAUCHE, CENTRE }

    /** Morceau de texte d'une police et d'une couleur donnees. */
    /** lien : adresse ouverte au clic (https:, mailto:, tel:), ou null. */
    record Morceau(PDType1Font police, float[] couleur, String texte, String lien) {

        Morceau(PDType1Font police, float[] couleur, String texte) {
            this(police, couleur, texte, null);
        }
    }

    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final java.util.regex.Pattern SITE =
            java.util.regex.Pattern.compile("^(https?://)?(www\\.)?[\\w-]+(\\.[\\w-]+)+(/\\S*)?$",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    /** Adresse cliquable d'un email, d'un site ou d'un numero de telephone, ou null. */
    static String lienDe(String s) {
        String v = texte(s);
        if (EMAIL.matcher(v).matches()) {
            return "mailto:" + v;
        }
        if (SITE.matcher(v).matches() && !v.matches("^[\\d.\\s]+$")) {
            return v.toLowerCase(Locale.ROOT).startsWith("http") ? v : "https://" + v;
        }
        String chiffres = v.replaceAll("[\\s.()-]", "");
        if (chiffres.matches("^\\+?\\d{8,15}$")) {
            return "tel:" + chiffres;
        }
        return null;
    }

    /** "https://www.github.com/moi/" -> "github.com/moi" : plus lisible sur le CV. */
    static String affichageLien(String s) {
        return texte(s).replaceFirst("(?i)^https?://", "").replaceFirst("(?i)^www\\.", "").replaceAll("/+$", "");
    }

    /** Dessin de fond de chaque page (bande, colonne de couleur). */
    @FunctionalInterface
    interface Decor {
        void dessiner(PDPageContentStream flux) throws IOException;
    }

    /** Document en cours : pages, polices et echelle. Toutes les tailles sont multipliees par l'echelle. */
    private static final class Toile {

        final PDDocument doc;
        final float echelle;
        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font italique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        private final Decor decor;
        private final List<PDPageContentStream> flux = new ArrayList<>();
        private float basPage;
        private float hautPage;

        Toile(PDDocument doc, float echelle, Decor decor) throws IOException {
            this.doc = doc;
            this.echelle = echelle;
            this.decor = decor;
            this.basPage = marge(38);
            this.hautPage = HAUTEUR_PAGE - marge(40);
            page(0);
        }

        /** Marge resserree avec l'echelle. */
        float marge(float base) {
            return base * (0.72f + 0.28f * echelle);
        }

        /** Haut de la zone de texte (et des pages suivantes). */
        float haut(float marge) {
            hautPage = HAUTEUR_PAGE - marge(marge);
            return hautPage;
        }

        Colonne colonne(float x, float largeur, float y) {
            return new Colonne(this, x, largeur, y, 0);
        }

        PDPageContentStream page(int index) throws IOException {
            while (flux.size() <= index) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                PDPageContentStream f = new PDPageContentStream(doc, page);
                decor.dessiner(f);
                flux.add(f);
            }
            return flux.get(index);
        }

        Morceau n(float[] c, String s) {
            return new Morceau(normal, c, s == null ? "" : s);
        }

        Morceau g(float[] c, String s) {
            return new Morceau(gras, c, s == null ? "" : s);
        }

        Morceau i(float[] c, String s) {
            return new Morceau(italique, c, s == null ? "" : s);
        }

        /** Element de contact : les emails et les sites sont bleus, soulignes et cliquables ; le telephone est cliquable. */
        Morceau contact(String s) {
            String lien = lienDe(s);
            if (lien == null) {
                return n(NOIR, s);
            }
            if (lien.startsWith("tel:")) {
                return new Morceau(normal, NOIR, texte(s), lien);
            }
            return new Morceau(normal, LIEN, lien.startsWith("mailto:") ? texte(s) : affichageLien(s), lien);
        }

        /** Zone cliquable sur la page. */
        void lien(int page, float x, float y, float l, float h, String uri) {
            org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink a =
                    new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink();
            a.setRectangle(new PDRectangle(x, y, l, h));
            org.apache.pdfbox.pdmodel.interactive.action.PDActionURI action =
                    new org.apache.pdfbox.pdmodel.interactive.action.PDActionURI();
            action.setURI(uri);
            a.setAction(action);
            org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary bord =
                    new org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary();
            bord.setWidth(0);
            a.setBorderStyle(bord);
            try {
                doc.getPage(page).getAnnotations().add(a);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Photo recadree au centre (sans deformation), carree ou ronde. */
        void image(int page, byte[] octets, float x, float y, float l, float h, boolean rond) throws IOException {
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, octets, "photo");
            PDPageContentStream f = page(page);
            float ratio = (float) img.getWidth() / img.getHeight();
            float cible = l / h;
            float dessinL = ratio > cible ? h * ratio : l;
            float dessinH = ratio > cible ? h : l / ratio;
            f.saveGraphicsState();
            if (rond) {
                float k = 0.5523f;
                float rx = l / 2;
                float ry = h / 2;
                float cx = x + rx;
                float cy = y + ry;
                f.moveTo(cx + rx, cy);
                f.curveTo(cx + rx, cy + k * ry, cx + k * rx, cy + ry, cx, cy + ry);
                f.curveTo(cx - k * rx, cy + ry, cx - rx, cy + k * ry, cx - rx, cy);
                f.curveTo(cx - rx, cy - k * ry, cx - k * rx, cy - ry, cx, cy - ry);
                f.curveTo(cx + k * rx, cy - ry, cx + rx, cy - k * ry, cx + rx, cy);
                f.closePath();
            } else {
                f.addRect(x, y, l, h);
            }
            f.clip();
            // Recadrage : on garde le haut de la photo (le visage), centre horizontalement
            f.drawImage(img, x - (dessinL - l) / 2, y + h - dessinH, dessinL, dessinH);
            f.restoreGraphicsState();
        }

        void fermer() throws IOException {
            for (PDPageContentStream f : flux) {
                f.close();
            }
            flux.clear();
        }
    }

    /** Zone de texte verticale ; y descend a mesure qu'on ecrit, et passe a la page suivante si besoin. */
    private static final class Colonne {

        private final Toile t;
        final float x;
        final float largeur;
        float y;
        int page;
        /** Largeur reservee a droite (photo) tant que y est au-dessus de seuilRetrait. */
        private float retrait;
        private float seuilRetrait;

        Colonne(Toile t, float x, float largeur, float y, int page) {
            this.t = t;
            this.x = x;
            this.largeur = largeur;
            this.y = y;
            this.page = page;
        }

        void retrait(float largeurReservee, float jusqua) {
            retrait = largeurReservee;
            seuilRetrait = jusqua;
        }

        private float largeurUtile() {
            return retrait > 0 && y > seuilRetrait ? largeur - retrait : largeur;
        }

        /** Sous-colonne qui commence a la meme hauteur (mise en page en colonnes). */
        Colonne sous(float decalage, float l) {
            return new Colonne(t, x + decalage, l, y, page);
        }

        /** Reprend sous la plus longue des sous-colonnes. */
        void apres(Colonne... colonnes) {
            int dernierePage = page;
            for (Colonne c : colonnes) {
                dernierePage = Math.max(dernierePage, c.page);
            }
            float bas = Float.MAX_VALUE;
            for (Colonne c : colonnes) {
                if (c.page == dernierePage) {
                    bas = Math.min(bas, c.y);
                }
            }
            if (dernierePage != page) {
                page = dernierePage;
                y = bas;
            } else {
                y = Math.min(y, bas);
            }
        }

        void espace(float points) {
            y -= points * t.echelle;
        }

        void place(float hauteur) {
            if (y - hauteur < t.basPage) {
                page++;
                y = t.hautPage;
            }
        }

        void trait(float[] c, float epaisseur) throws IOException {
            PDPageContentStream f = t.page(page);
            f.setStrokingColor(c[0], c[1], c[2]);
            f.setLineWidth(epaisseur * t.echelle);
            f.moveTo(x, y);
            f.lineTo(x + largeur, y);
            f.stroke();
            y -= epaisseur * t.echelle;
        }

        /** Puce "•" et texte dans le meme bloc : chaque element reste une ligne a part pour les ATS. */
        void puce(Toile t, String texte, float taille, float interligne, float decalage) throws IOException {
            String puce = "•";
            float retraitTexte = decalage + (t.normal.getStringWidth(puce + "  ") / 1000) * taille;
            riche(List.of(t.n(NOIR, puce + "  "), t.n(NOIR, texte)), taille, interligne, decalage, retraitTexte,
                    Aligne.GAUCHE, null);
        }

        void riche(List<Morceau> morceaux, float taille, float interligne, Aligne aligne) throws IOException {
            riche(morceaux, taille, interligne, 0, 0, aligne, null);
        }

        /** Texte a gauche et element cale a droite sur la premiere ligne (lieu, dates). */
        void ligne(List<Morceau> gauche, Morceau droite, float taille, float interligne) throws IOException {
            riche(gauche, taille, interligne, 0, 0, Aligne.GAUCHE, droite == null || vide(droite.texte()) ? null : droite);
        }

        private record Mot(Morceau style, String mot, boolean espaceAvant) {
        }

        /**
         * Ecrit des morceaux de polices et couleurs differentes, coupes en lignes a la largeur de la colonne.
         * retraitPremiere / retraitSuite : decalage de la premiere ligne et des suivantes (puces).
         */
        void riche(List<Morceau> morceaux, float taille, float interligne, float retraitPremiere, float retraitSuite,
                Aligne aligne, Morceau droite) throws IOException {
            List<Mot> mots = new ArrayList<>();
            boolean espace = false;
            for (Morceau m : morceaux) {
                String s = LettrePdf.nettoyer(m.texte(), m.police()).replace('\n', ' ');
                StringBuilder courant = new StringBuilder();
                for (char ch : s.toCharArray()) {
                    if (ch == ' ') {
                        if (!courant.isEmpty()) {
                            mots.add(new Mot(m, courant.toString(), espace));
                            courant.setLength(0);
                        }
                        espace = true;
                    } else {
                        courant.append(ch);
                    }
                }
                if (!courant.isEmpty()) {
                    mots.add(new Mot(m, courant.toString(), espace));
                    espace = false;
                }
            }
            if (mots.isEmpty()) {
                return;
            }
            String droiteTexte = droite == null ? "" : LettrePdf.nettoyer(droite.texte(), droite.police()).strip();
            float largeurDroite = droiteTexte.isEmpty() ? 0 : largeurDe(droiteTexte, droite.police(), taille);

            int i = 0;
            boolean premiere = true;
            while (i < mots.size()) {
                place(interligne);
                float decalage = premiere ? retraitPremiere : retraitSuite;
                float disponible = largeurUtile() - decalage - (premiere && largeurDroite > 0 ? largeurDroite + 12 : 0);
                List<Mot> ligne = new ArrayList<>();
                float l = 0;
                while (i < mots.size()) {
                    Mot m = mots.get(i);
                    float ajout = largeurDe(m.mot(), m.style().police(), taille)
                            + (ligne.isEmpty() || !m.espaceAvant() ? 0 : largeurDe(" ", m.style().police(), taille));
                    if (!ligne.isEmpty() && l + ajout > disponible) {
                        break;
                    }
                    ligne.add(m);
                    l += ajout;
                    i++;
                }
                float xLigne = x + decalage;
                if (aligne == Aligne.CENTRE) {
                    xLigne = x + (largeurUtile() - l) / 2;
                }
                ecrireLigne(xLigne, ligne, taille);
                if (premiere && largeurDroite > 0) {
                    ecrireLigne(x + largeurUtile() - largeurDroite,
                            List.of(new Mot(droite, droiteTexte, false)), taille);
                }
                y -= interligne;
                premiere = false;
            }
        }

        private record Segment(Morceau style, String texte) {
        }

        private void ecrireLigne(float xLigne, List<Mot> mots, float taille) throws IOException {
            List<Segment> segments = new ArrayList<>();
            Morceau style = null;
            StringBuilder tampon = new StringBuilder();
            for (int k = 0; k < mots.size(); k++) {
                Mot m = mots.get(k);
                if (style != null && m.style() != style) {
                    segments.add(new Segment(style, tampon.toString()));
                    tampon.setLength(0);
                }
                style = m.style();
                if (k > 0 && m.espaceAvant()) {
                    tampon.append(' ');
                }
                tampon.append(m.mot());
            }
            if (style != null) {
                segments.add(new Segment(style, tampon.toString()));
            }
            PDPageContentStream f = t.page(page);
            f.beginText();
            f.newLineAtOffset(xLigne, y - taille);
            for (Segment seg : segments) {
                montrer(f, seg.style(), seg.texte(), taille);
            }
            f.endText();
            // Liens : soulignes (sauf le telephone) et cliquables, sans l'espace qui les precede
            float xCourant = xLigne;
            for (Segment seg : segments) {
                float l = largeurDe(seg.texte(), seg.style().police(), taille);
                if (seg.style().lien() != null) {
                    float avant = seg.texte().startsWith(" ") ? largeurDe(" ", seg.style().police(), taille) : 0;
                    float debut = xCourant + avant;
                    float fin = xCourant + l;
                    float base = y - taille;
                    if (!seg.style().lien().startsWith("tel:")) {
                        float[] c = seg.style().couleur();
                        f.setStrokingColor(c[0], c[1], c[2]);
                        f.setLineWidth(Math.max(0.4f, taille * 0.05f));
                        f.moveTo(debut, base - taille * 0.14f);
                        f.lineTo(fin, base - taille * 0.14f);
                        f.stroke();
                    }
                    t.lien(page, debut, base - taille * 0.25f, fin - debut, taille * 1.2f, seg.style().lien());
                }
                xCourant += l;
            }
        }

        private static void montrer(PDPageContentStream f, Morceau style, String s, float taille) throws IOException {
            if (s.isEmpty()) {
                return;
            }
            float[] c = style.couleur();
            f.setNonStrokingColor(c[0], c[1], c[2]);
            f.setFont(style.police(), taille);
            f.showText(s);
        }

        private static float largeurDe(String s, PDType1Font police, float taille) throws IOException {
            return police.getStringWidth(s) / 1000 * taille;
        }
    }
}
