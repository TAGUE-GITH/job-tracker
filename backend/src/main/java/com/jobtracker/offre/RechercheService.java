package com.jobtracker.offre;

import com.jobtracker.ia.IaService;
import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recherche d'offres "intelligente" : la requete est d'abord comprise (intitule corrige, traduction, intitules
 * equivalents, competences du poste), puis toutes les sources du pays sont interrogees et les offres sont gardees
 * et classees selon leur pertinence reelle pour ce que le candidat cherche. Les sources ne sont jamais exposees.
 */
@Service
public class RechercheService {

    private static final Logger LOG = LoggerFactory.getLogger(RechercheService.class);
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final int TAILLE_CACHE = 500;
    /** En dessous de ce nombre d'offres pertinentes, on relance avec un intitule equivalent. */
    private static final int SEUIL_ELARGISSEMENT = 12;

    private final IaService iaService;
    private final OffreService offreService;
    /** Requetes deja comprises (l'IA n'est appelee qu'une fois par requete). */
    private final Map<String, Intention> cache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Intention> e) {
                    return size() > TAILLE_CACHE;
                }
            });

    public RechercheService(IaService iaService, OffreService offreService) {
        this.iaService = iaService;
        this.offreService = offreService;
    }

    /** Ce que le candidat cherche, tel que compris. */
    public record Intention(String intitule, String intituleEn, List<String> synonymes, List<String> competences,
            String domaine, String contrat, String lieu) {

        static Intention vide() {
            return new Intention("", "", List.of(), List.of(), "", "", "");
        }

        /** Intitule et equivalents (sans doublon), du plus proche au moins proche. */
        List<String> formulations() {
            Set<String> f = new LinkedHashSet<>();
            for (String s : concat(java.util.Arrays.asList(intitule, intituleEn), liste(synonymes))) {
                if (s != null && !s.isBlank()) {
                    f.add(s.strip());
                }
            }
            return List.copyOf(f);
        }
    }

    public record OffreTrouvee(OffreExterne offre, Integer pertinence) {
    }

    /** interpretation : la requete comprise, affichee au candidat (null si la recherche est vide). */
    public record Resultats(List<OffreTrouvee> offres, int page, boolean pageSuivante, Intention interpretation,
            List<String> suggestions) {
    }

    public Resultats rechercher(String requete, String lieu, String pays, String contrat, Catalogue.Domaine domaine,
            boolean parDate, int periodeJours, int page) {
        String q = requete == null ? "" : requete.strip();
        Intention intention = q.isEmpty() ? Intention.vide() : comprendre(q, pays);
        String contratFinal = !contrat.isBlank() ? contrat : valeurContrat(intention.contrat());
        String lieuFinal = !lieu.isBlank() ? lieu : (intention.lieu() == null ? "" : intention.lieu().strip());
        Catalogue.Domaine domaineFinal = domaine != null ? domaine
                : q.isEmpty() ? null : Catalogue.domaine(String.valueOf(intention.domaine())).orElse(null);
        String fr = q.isEmpty() ? "" : premier(intention.intitule(), q);
        String en = q.isEmpty() ? "" : premier(intention.intituleEn(), fr);

        OffresResponse brut = offreService.rechercher(OffreService.TOUTES,
                new Recherche(fr, lieuFinal, contratFinal, page, pays, domaineFinal, parDate, en));
        List<OffreExterne> offres = new ArrayList<>(brut.offres());
        boolean suite = brut.pageSuivante();

        List<OffreTrouvee> gardees = filtrer(offres, intention, q.isEmpty(), periodeJours);
        // Peu de resultats : on relance avec l'intitule equivalent le plus proche (ex. "Developpeur web")
        if (!q.isEmpty() && page == 0 && gardees.size() < SEUIL_ELARGISSEMENT) {
            List<String> autres = intention.formulations().stream()
                    .filter(f -> !normaliser(f).equals(normaliser(fr)) && !normaliser(f).equals(normaliser(en)))
                    .limit(1).toList();
            for (String autre : autres) {
                try {
                    OffresResponse plus = offreService.rechercher(OffreService.TOUTES,
                            new Recherche(autre, lieuFinal, contratFinal, 0, pays, domaineFinal, parDate, autre));
                    offres.addAll(plus.offres());
                    suite |= plus.pageSuivante();
                } catch (RuntimeException e) {
                    LOG.info("Elargissement '{}' impossible : {}", autre, e.getMessage());
                }
            }
            gardees = filtrer(dedoublonner(offres), intention, false, periodeJours);
        }
        Comparator<OffreTrouvee> ordre = parDate || q.isEmpty()
                ? Comparator.comparing((OffreTrouvee o) -> o.offre().datePublication(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                : Comparator.comparing(OffreTrouvee::pertinence, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(o -> o.offre().datePublication(), Comparator.nullsLast(Comparator.reverseOrder()));
        List<OffreTrouvee> triees = gardees.stream().sorted(ordre).toList();
        List<String> suggestions = triees.isEmpty() && !q.isEmpty()
                ? intention.formulations().stream().filter(f -> !normaliser(f).equals(normaliser(q))).limit(4).toList()
                : List.of();
        return new Resultats(triees, page, suite, q.isEmpty() ? null : intention, suggestions);
    }

    /** Comprend la requete avec l'IA (mise en cache), sinon avec un petit dictionnaire de metiers. */
    Intention comprendre(String requete, String pays) {
        String cle = pays + "|" + normaliser(requete);
        Intention connue = cache.get(cle);
        if (connue != null) {
            return connue;
        }
        Intention intention = null;
        if (iaService.disponible()) {
            try {
                String nomPays = Catalogue.pays(pays).map(Catalogue.Pays::nom).orElse(pays);
                Intention lue = JSON.readValue(iaService.comprendreRecherche(requete, nomPays).reponse(),
                        Intention.class);
                intention = new Intention(premier(lue.intitule(), requete), premier(lue.intituleEn(), ""),
                        liste(lue.synonymes()), liste(lue.competences()), lue.domaine(), lue.contrat(), lue.lieu());
            } catch (RuntimeException e) {
                LOG.info("Comprehension de '{}' par l'IA impossible : {}", requete, e.getMessage());
            }
        }
        if (intention == null) {
            intention = Dictionnaire.comprendre(requete);
        }
        cache.put(cle, intention);
        return intention;
    }

    /** Garde les offres pertinentes (et recentes si demande), avec leur note de pertinence. */
    static List<OffreTrouvee> filtrer(List<OffreExterne> offres, Intention intention, boolean sansRequete,
            int periodeJours) {
        LocalDate limite = periodeJours > 0 ? LocalDate.now().minusDays(periodeJours) : null;
        List<OffreTrouvee> resultat = new ArrayList<>();
        for (OffreExterne o : dedoublonner(offres)) {
            if (limite != null && (o.datePublication() == null || o.datePublication().isBefore(limite))) {
                continue;
            }
            if (sansRequete) {
                resultat.add(new OffreTrouvee(o, null));
                continue;
            }
            int note = pertinence(o, intention);
            if (note >= 35) {
                resultat.add(new OffreTrouvee(o, note));
            }
        }
        return resultat;
    }

    /**
     * Note de 0 a 100 : jusqu'a 75 points si le titre reprend l'intitule ou un equivalent (part des mots
     * significatifs presents), jusqu'a 25 points selon les competences du poste citees dans l'offre.
     */
    static int pertinence(OffreExterne o, Intention intention) {
        String titre = " " + normaliser(o.titre()) + " ";
        String texte = titre + normaliser(o.description()) + " ";
        double meilleurTitre = 0;
        for (String formulation : intention.formulations()) {
            List<String> mots = motsSignificatifs(formulation);
            if (mots.isEmpty()) {
                continue;
            }
            long presents = mots.stream().filter(m -> titre.contains(" " + m) || titre.contains(m + " ")).count();
            double part = (double) presents / mots.size();
            // L'intitule exact dans la description compte aussi, un peu moins
            if (part < 1 && texte.contains(" " + normaliser(formulation) + " ")) {
                part = Math.max(part, 0.7);
            }
            meilleurTitre = Math.max(meilleurTitre, part);
        }
        long competences = liste(intention.competences()).stream()
                .map(RechercheService::normaliser)
                .filter(c -> c.length() >= 2 && texte.contains(c.length() <= 3 ? " " + c + " " : c))
                .count();
        double partCompetences = Math.min(1.0, competences / 3.0);
        return (int) Math.round(75 * meilleurTitre + 25 * partCompetences);
    }

    private static List<OffreExterne> dedoublonner(List<OffreExterne> offres) {
        Set<String> vues = new HashSet<>();
        List<OffreExterne> resultat = new ArrayList<>();
        for (OffreExterne o : offres) {
            // Meme poste, meme entreprise, meme lieu publie sur deux sites : une seule fois
            String cleContenu = "c|" + normaliser(o.titre()) + "|" + normaliser(o.entreprise()) + "|" + normaliser(o.lieu());
            boolean nouveauLien = o.url() == null || vues.add("u|" + o.url());
            if (nouveauLien && vues.add(cleContenu)) {
                resultat.add(o);
            }
        }
        return resultat;
    }

    /** Mots significatifs d'un intitule ("de", "h/f", "junior"... ignores). */
    static List<String> motsSignificatifs(String intitule) {
        Set<String> vides = Set.of("de", "du", "des", "la", "le", "les", "et", "en", "a", "au", "h", "f", "x", "the",
                "of", "and", "for", "junior", "senior", "confirme", "experimente", "stagiaire", "alternant", "alternance",
                "stage", "intern", "cdi", "cdd");
        List<String> mots = new ArrayList<>();
        for (String m : normaliser(intitule).split(" ")) {
            if (m.length() > 1 && !vides.contains(m)) {
                mots.add(m);
            }
        }
        return mots;
    }

    /** Minuscules sans accents ; "full-stack" et "fullstack" deviennent "full stack". */
    static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String t = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        t = t.replace("fullstack", "full stack").replace("front-end", "frontend").replace("back-end", "backend")
                .replace("front end", "frontend").replace("back end", "backend");
        return t.replaceAll("[^a-z0-9+#]+", " ").strip();
    }

    private static String valeurContrat(String contrat) {
        String c = contrat == null ? "" : contrat.strip().toUpperCase(Locale.ROOT);
        return Set.of("ALTERNANCE", "STAGE", "CDI", "CDD").contains(c) ? c : "";
    }

    private static String premier(String valeur, String sinon) {
        return valeur != null && !valeur.isBlank() ? valeur.strip() : sinon;
    }

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }
}
