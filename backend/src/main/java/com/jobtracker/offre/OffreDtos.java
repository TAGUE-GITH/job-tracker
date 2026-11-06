package com.jobtracker.offre;

import java.time.LocalDate;
import java.util.List;

public final class OffreDtos {

    private OffreDtos() {
    }

    /** Criteres de recherche. contrat : vide, ALTERNANCE, STAGE, CDI ou CDD. page commence a 0. */
    public record Recherche(String motsCles, String lieu, String contrat, int page) {
    }

    /** Offre d'emploi venant d'une API externe, au meme format quelle que soit la source. */
    public record OffreExterne(
            String id,
            String source,
            String titre,
            String entreprise,
            String lieu,
            String contrat,
            String salaire,
            String description,
            String url,
            LocalDate datePublication) {
    }

    public record ResultatSource(List<OffreExterne> offres, long total) {
    }

    public record OffresResponse(
            List<String> sources,
            String source,
            List<OffreExterne> offres,
            long total,
            int page,
            boolean pageSuivante) {
    }
}
