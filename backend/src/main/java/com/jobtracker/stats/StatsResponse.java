package com.jobtracker.stats;

import java.util.List;
import java.util.Map;

public record StatsResponse(
        long total,
        long enCours,
        double tauxReponse,
        double tauxEntretien,
        double tauxOffre,
        Map<String, Long> parStatut,
        List<MoisCount> parMois,
        List<EtapeCount> entonnoir) {

    public record MoisCount(String mois, long count) {
    }

    public record EtapeCount(String etape, long count) {
    }
}
