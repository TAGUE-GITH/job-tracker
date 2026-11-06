package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * API Adzuna (agregateur de nombreux sites d'emploi, gratuite avec une cle sur https://developer.adzuna.com).
 */
@Component
@Order(2)
public class AdzunaSource implements SourceOffres {

    private static final String URL_API = "https://api.adzuna.com/v1/api/jobs/fr/search/";

    private final RestClient restClient = RestClient.create();
    private final String appId;
    private final String appKey;

    public AdzunaSource(
            @Value("${app.offres.adzuna.app-id:}") String appId,
            @Value("${app.offres.adzuna.app-key:}") String appKey) {
        this.appId = SourceOffres.nettoyer(appId);
        this.appKey = SourceOffres.nettoyer(appKey);
    }

    record Reponse(Long count, List<Offre> results) {
    }

    record Offre(String id, String title, String description, String created, Nom company, Nom location,
            String redirect_url, Double salary_min, Double salary_max, String contract_type,
            String contract_time) {
    }

    record Nom(String display_name) {
    }

    @Override
    public String nom() {
        return "Adzuna";
    }

    @Override
    public boolean configuree() {
        return !appId.isBlank() && !appKey.isBlank();
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        String motsCles = switch (r.contrat()) {
            case "ALTERNANCE" -> (r.motsCles() + " alternance").strip();
            case "STAGE" -> (r.motsCles() + " stage").strip();
            default -> r.motsCles();
        };
        String what = motsCles;
        Reponse res = restClient.get()
                .uri(URL_API + (r.page() + 1), b -> {
                    b.queryParam("app_id", appId)
                            .queryParam("app_key", appKey)
                            .queryParam("results_per_page", TAILLE_PAGE)
                            .queryParam("sort_by", "date");
                    if (!what.isBlank()) {
                        b.queryParam("what", what);
                    }
                    if (!r.lieu().isBlank()) {
                        b.queryParam("where", r.lieu());
                    }
                    if ("CDI".equals(r.contrat())) {
                        b.queryParam("permanent", 1);
                    } else if ("CDD".equals(r.contrat())) {
                        b.queryParam("contract", 1);
                    }
                    return b.build();
                })
                .retrieve()
                .body(Reponse.class);
        if (res == null || res.results() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<OffreExterne> offres = res.results().stream().map(this::convertir).toList();
        return new ResultatSource(offres, res.count() != null ? res.count() : offres.size());
    }

    private OffreExterne convertir(Offre o) {
        LocalDate date = o.created() != null && o.created().length() >= 10
                ? LocalDate.parse(o.created().substring(0, 10)) : null;
        return new OffreExterne(
                "adz-" + o.id(),
                nom(),
                SourceOffres.sansHtml(o.title()),
                o.company() != null ? o.company().display_name() : null,
                o.location() != null ? o.location().display_name() : null,
                contrat(o),
                salaire(o),
                SourceOffres.sansHtml(o.description()),
                o.redirect_url(),
                date);
    }

    private static String contrat(Offre o) {
        if ("permanent".equals(o.contract_type())) {
            return "CDI";
        }
        if ("contract".equals(o.contract_type())) {
            return "CDD";
        }
        return "part_time".equals(o.contract_time()) ? "Temps partiel" : null;
    }

    private static String salaire(Offre o) {
        if (o.salary_min() == null) {
            return null;
        }
        long min = Math.round(o.salary_min());
        long max = o.salary_max() != null ? Math.round(o.salary_max()) : min;
        return min == max ? min + " € / an" : min + " - " + max + " € / an";
    }
}
