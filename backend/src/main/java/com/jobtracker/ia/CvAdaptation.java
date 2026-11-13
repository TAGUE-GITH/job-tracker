package com.jobtracker.ia;

import com.jobtracker.document.CvPdf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Assemble le CV adapte a partir de deux reponses de l'IA : le CV importe recopie fidelement (extraction), puis les
 * ameliorations proposees pour l'offre. La fusion est faite ici, pas par l'IA : aucune experience, formation, projet
 * ou rubrique du CV importe ne peut disparaitre, et les postes, entreprises, lieux et dates restent ceux du CV.
 * L'IA ne fait que reformuler les missions et completer les competences et atouts.
 */
final class CvAdaptation {

    static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    record ExperienceAmelioree(Integer index, List<String> missions) {
    }

    record ProjetAmeliore(Integer index, String description, List<String> points) {
    }

    record FormationAmelioree(Integer index, String details) {
    }

    record RubriqueAmelioree(Integer index, List<String> elements) {
    }

    record Ameliorations(String titre, String profil, List<ExperienceAmelioree> experiences,
            List<ProjetAmeliore> projets, List<FormationAmelioree> formations,
            List<RubriqueAmelioree> autresRubriques, List<String> competencesTechniquesAjoutees,
            List<String> competencesTransversesAjoutees, List<String> atoutsAjoutes, List<String> motsClesAjoutes) {
    }

    /** CV adapte envoye au formulaire de l'application (champs du CV + ajouts a signaler). */
    record CvAdapte(String titre, String profil, List<String> competencesTechniques,
            List<String> competencesTransverses, List<CvPdf.Experience> experiences, List<CvPdf.Projet> projets,
            List<CvPdf.Formation> formations, List<String> langues, List<String> atouts, List<String> centresInteret,
            List<String> liens, List<CvPdf.Rubrique> autresRubriques, List<String> motsClesAjoutes,
            List<String> competencesAjoutees) {
    }

    private CvAdaptation() {
    }

    static CvPdf.Cv lireExtraction(String json) {
        return JSON.readValue(json, CvPdf.Cv.class);
    }

    static Ameliorations lireAmeliorations(String json) {
        return JSON.readValue(json, Ameliorations.class);
    }

    static String versJson(CvAdapte cv) {
        return JSON.writeValueAsString(cv);
    }

    static CvAdapte fusionner(CvPdf.Cv origine, Ameliorations a) {
        Map<Integer, ExperienceAmelioree> exps = parIndex(a.experiences(), ExperienceAmelioree::index);
        Map<Integer, ProjetAmeliore> projs = parIndex(a.projets(), ProjetAmeliore::index);
        Map<Integer, FormationAmelioree> forms = parIndex(a.formations(), FormationAmelioree::index);
        Map<Integer, RubriqueAmelioree> rubs = parIndex(a.autresRubriques(), RubriqueAmelioree::index);

        List<CvPdf.Experience> experiences = new ArrayList<>();
        List<CvPdf.Experience> exO = liste(origine.experiences());
        for (int i = 0; i < exO.size(); i++) {
            CvPdf.Experience x = exO.get(i);
            ExperienceAmelioree m = exps.get(i);
            experiences.add(new CvPdf.Experience(x.poste(), x.entreprise(), x.lieu(), x.periode(),
                    meilleure(liste(x.missions()), m == null ? null : m.missions())));
        }
        List<CvPdf.Projet> projets = new ArrayList<>();
        List<CvPdf.Projet> prO = liste(origine.projets());
        for (int i = 0; i < prO.size(); i++) {
            CvPdf.Projet p = prO.get(i);
            ProjetAmeliore m = projs.get(i);
            projets.add(new CvPdf.Projet(p.nom(),
                    m != null && !vide(m.description()) ? m.description() : p.description(),
                    p.technologies(), meilleure(liste(p.points()), m == null ? null : m.points())));
        }
        List<CvPdf.Formation> formations = new ArrayList<>();
        List<CvPdf.Formation> foO = liste(origine.formations());
        for (int i = 0; i < foO.size(); i++) {
            CvPdf.Formation f = foO.get(i);
            FormationAmelioree m = forms.get(i);
            formations.add(new CvPdf.Formation(f.diplome(), f.etablissement(), f.periode(),
                    m != null && !vide(m.details()) ? m.details() : f.details(), f.lieu()));
        }
        List<CvPdf.Rubrique> rubriques = new ArrayList<>();
        List<CvPdf.Rubrique> ruO = liste(origine.autresRubriques());
        for (int i = 0; i < ruO.size(); i++) {
            CvPdf.Rubrique r = ruO.get(i);
            RubriqueAmelioree m = rubs.get(i);
            rubriques.add(new CvPdf.Rubrique(r.titre(), meilleure(liste(r.elements()), m == null ? null : m.elements())));
        }

        List<String> techniques = new ArrayList<>(liste(origine.competencesTechniques()));
        List<String> transverses = new ArrayList<>(liste(origine.competencesTransverses()));
        List<String> ajoutees = new ArrayList<>();
        for (String c : liste(a.competencesTechniquesAjoutees())) {
            if (absente(c, techniques, transverses)) {
                techniques.add(c.strip());
                ajoutees.add(c.strip());
            }
        }
        for (String c : liste(a.competencesTransversesAjoutees())) {
            if (absente(c, techniques, transverses)) {
                transverses.add(c.strip());
                ajoutees.add(c.strip());
            }
        }
        List<String> atouts = new ArrayList<>(liste(origine.atouts()));
        for (String c : liste(a.atoutsAjoutes())) {
            if (absente(c, atouts, transverses)) {
                atouts.add(c.strip());
            }
        }
        return new CvAdapte(
                vide(a.titre()) ? origine.titre() : a.titre(),
                vide(a.profil()) ? origine.profil() : a.profil(),
                techniques, transverses, experiences, projets, formations,
                liste(origine.langues()), atouts, liste(origine.centresInteret()), liste(origine.liens()), rubriques,
                liste(a.motsClesAjoutes()), ajoutees);
    }

    /**
     * Elements reformules par l'IA, sauf s'il en manque par rapport au CV (une mission ne doit pas disparaitre) :
     * on garde alors ceux du CV.
     */
    private static List<String> meilleure(List<String> origine, List<String> reformules) {
        List<String> r = liste(reformules);
        return r.isEmpty() || r.size() < origine.size() ? origine : r;
    }

    @SafeVarargs
    private static boolean absente(String competence, List<String>... listes) {
        if (vide(competence)) {
            return false;
        }
        String c = competence.strip().toLowerCase(Locale.ROOT);
        for (List<String> l : listes) {
            if (l.stream().anyMatch(x -> x.strip().toLowerCase(Locale.ROOT).equals(c))) {
                return false;
            }
        }
        return true;
    }

    private static <T> Map<Integer, T> parIndex(List<T> elements, Function<T, Integer> index) {
        Map<Integer, T> m = new LinkedHashMap<>();
        for (T e : liste(elements)) {
            if (index.apply(e) != null) {
                m.putIfAbsent(index.apply(e), e);
            }
        }
        return m;
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
}
