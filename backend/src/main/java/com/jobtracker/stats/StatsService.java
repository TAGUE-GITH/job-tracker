package com.jobtracker.stats;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.candidature.StatutCandidature;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.stats.StatsResponse.EtapeCount;
import com.jobtracker.stats.StatsResponse.MoisCount;
import java.time.YearMonth;
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

        long avecReponse = compter(candidatures, t -> t != TypeEtape.CANDIDATURE_ENVOYEE);
        long avecRh = compter(candidatures, t -> t == TypeEtape.ECHANGE_RH);
        long avecEntretien = compter(candidatures, TypeEtape::estEntretien);
        long avecOffre = compter(candidatures, t -> t == TypeEtape.OFFRE);

        List<EtapeCount> entonnoir = List.of(
                new EtapeCount("Candidatures", total),
                new EtapeCount("Reponses", avecReponse),
                new EtapeCount("Echange RH", avecRh),
                new EtapeCount("Entretiens", avecEntretien),
                new EtapeCount("Offres", avecOffre));

        return new StatsResponse(total, parStatut.get(StatutCandidature.EN_COURS.name()),
                pourcentage(avecReponse, total), pourcentage(avecEntretien, total), pourcentage(avecOffre, total),
                parStatut, parMois, entonnoir);
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
