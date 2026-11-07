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
        List<EtapeCount> entonnoir,
        List<GroupeCount> parCv,
        List<GroupeCount> parSource,
        Double delaiMoyenReponse) {

    public record MoisCount(String mois, long count) {
    }

    public record EtapeCount(String etape, long count) {
    }

    /** Candidatures d'un groupe (un CV, un site d'offres) et part ayant obtenu une reponse. */
    public record GroupeCount(String nom, long total, long reponses, double tauxReponse) {
    }
}
