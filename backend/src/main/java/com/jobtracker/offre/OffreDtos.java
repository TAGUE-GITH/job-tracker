package com.jobtracker.offre;

import java.time.LocalDate;
import java.util.List;

public final class OffreDtos {

    private OffreDtos() {
    }

    /**
     * Criteres de recherche. contrat : vide, ALTERNANCE, STAGE, CDI ou CDD. page commence a 0.
     * pays : code ISO (FR par defaut). domaine : null = tous les domaines. parDate : plus recentes d'abord.
     */
    public record Recherche(String motsCles, String lieu, String contrat, int page, String pays,
            Catalogue.Domaine domaine, boolean parDate, String motsClesEn) {

        public Recherche(String motsCles, String lieu, String contrat, int page) {
            this(motsCles, lieu, contrat, page, "FR", null, false, null);
        }

        public Recherche(String motsCles, String lieu, String contrat, int page, String pays,
                Catalogue.Domaine domaine) {
            this(motsCles, lieu, contrat, page, pays, domaine, false, null);
        }

        public Recherche(String motsCles, String lieu, String contrat, int page, String pays,
                Catalogue.Domaine domaine, boolean parDate) {
            this(motsCles, lieu, contrat, page, pays, domaine, parDate, null);
        }

        /** Mots-cles a envoyer a une API francophone : ceux de l'utilisateur, sinon le mot du domaine. */
        public String requeteFr() {
            return !motsCles.isBlank() || domaine == null ? motsCles : domaine.motFr();
        }

        /** Idem pour une API anglophone (traduction fournie par la comprehension de la recherche). */
        public String requeteEn() {
            if (motsClesEn != null && !motsClesEn.isBlank()) {
                return motsClesEn;
            }
            return !motsCles.isBlank() || domaine == null ? motsCles : domaine.motEn();
        }
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

    /** remarque : precision sur la source affichee (ex. offres en teletravail), ou null. */
    public record OffresResponse(
            List<String> sources,
            String source,
            List<OffreExterne> offres,
            long total,
            int page,
            boolean pageSuivante,
            String remarque) {
    }

    public record Criteres(List<Catalogue.Pays> pays, List<DomaineOption> domaines) {
    }

    public record DomaineOption(String code, String libelle) {
    }
}
