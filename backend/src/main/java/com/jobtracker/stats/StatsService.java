package com.jobtracker.stats;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.candidature.StatutCandidature;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.stats.StatsResponse.EtapeCount;
import com.jobtracker.stats.StatsResponse.GroupeCount;
import com.jobtracker.stats.StatsResponse.MoisCount;
import java.net.URI;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final CandidatureService candidatureService;

    public StatsService(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    @Transactional(readOnly = true)
    public StatsResponse calculer(Long userId) {
        List<Candidature> candidatures = candidatureService.toutes(userId);
        long total = candidatures.size();

        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (StatutCandidature s : StatutCandidature.values()) {
            parStatut.put(s.name(), candidatures.stream().filter(c -> c.getStatut() == s).count());
        }

        Map<YearMonth, Long> mois = new TreeMap<>();
        candidatures.forEach(c -> mois.merge(YearMonth.from(c.getDateCandidature()), 1L, Long::sum));
        List<MoisCount> parMois = new ArrayList<>();
        mois.forEach((m, n) -> parMois.add(new MoisCount(m.toString(), n)));

        long avecReponse = compter(candidatures, StatsService::estReponse);
        long avecRh = compter(candidatures, t -> t == TypeEtape.ECHANGE_RH);
        long avecEntretien = compter(candidatures, TypeEtape::estEntretien);
        long avecOffre = compter(candidatures, t -> t == TypeEtape.OFFRE);

        List<EtapeCount> entonnoir = List.of(
                new EtapeCount("Candidatures", total),
                new EtapeCount("Reponses", avecReponse),
                new EtapeCount("Echange RH", avecRh),
                new EtapeCount("Entretiens", avecEntretien),
                new EtapeCount("Offres", avecOffre));

        List<GroupeCount> parCv = grouper(candidatures,
                c -> c.getCv() != null ? c.getCv().getNomFichier() : "Sans CV");
        List<GroupeCount> parSource = grouper(candidatures, c -> source(c.getLienOffre()));
        Double delai = candidatures.stream()
                .map(c -> c.getEtapes().stream()
                        .filter(e -> estReponse(e.getType()))
                        .map(e -> ChronoUnit.DAYS.between(c.getDateCandidature(), e.getDate()))
                        .filter(j -> j >= 0)
                        .min(Long::compare)
                        .orElse(null))
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream().boxed().map(d -> Math.round(d * 10) / 10.0)
                .findFirst().orElse(null);

        return new StatsResponse(total, parStatut.get(StatutCandidature.EN_COURS.name()),
                pourcentage(avecReponse, total), pourcentage(avecEntretien, total), pourcentage(avecOffre, total),
                parStatut, parMois, entonnoir, parCv, parSource, delai);
    }

    private static boolean estReponse(TypeEtape t) {
        return t != TypeEtape.CANDIDATURE_ENVOYEE && t != TypeEtape.RELANCE;
    }

    /** Regroupe les candidatures (par CV, par site...) : les 6 groupes les plus utilises. */
    private static List<GroupeCount> grouper(List<Candidature> candidatures, Function<Candidature, String> cle) {
        Map<String, List<Candidature>> groupes = candidatures.stream()
                .collect(Collectors.groupingBy(cle, LinkedHashMap::new, Collectors.toList()));
        return groupes.entrySet().stream()
                .map(e -> {
                    long n = e.getValue().size();
                    long reponses = compter(e.getValue(), StatsService::estReponse);
                    return new GroupeCount(e.getKey(), n, reponses, pourcentage(reponses, n));
                })
                .sorted(Comparator.comparingLong(GroupeCount::total).reversed())
                .limit(6)
                .toList();
    }

    private static final Map<String, String> SITES = Map.of(
            "welcometothejungle", "Welcome to the Jungle", "linkedin", "LinkedIn", "indeed", "Indeed",
            "hellowork", "HelloWork", "apec", "Apec", "labonnealternance", "La bonne alternance",
            "jooble", "Jooble", "francetravail", "France Travail", "adzuna", "Adzuna");

    /** Nom lisible du site d'ou vient l'offre, d'apres son lien. */
    static String source(String lien) {
        if (lien == null || lien.isBlank()) {
            return "Sans lien";
        }
        String hote;
        try {
            hote = URI.create(lien.strip()).getHost();
        } catch (IllegalArgumentException e) {
            hote = null;
        }
        if (hote == null) {
            return "Autre";
        }
        String h = hote.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        return SITES.entrySet().stream()
                .filter(s -> h.contains(s.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(h);
    }

    private static long compter(List<Candidature> candidatures, Predicate<TypeEtape> filtre) {
        return candidatures.stream()
                .filter(c -> c.getEtapes().stream().map(e -> e.getType()).anyMatch(filtre))
                .count();
    }

    private static double pourcentage(long n, long total) {
        return total == 0 ? 0 : Math.round(n * 1000.0 / total) / 10.0;
    }
}
