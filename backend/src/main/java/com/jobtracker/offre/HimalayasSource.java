package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Himalayas (https://himalayas.app) : offres en teletravail du monde entier, tous domaines, filtrees sur les
 * candidats acceptes depuis le pays choisi. API publique sans cle ; l'offre renvoie vers la page d'origine.
 */
@Component
@Order(3)
public class HimalayasSource implements SourceOffres {

    private static final String URL_API = "https://himalayas.app/jobs/api/search";
    /** Type de contrat de Himalayas correspondant a nos contrats. */
    private static final Map<String, List<String>> CONTRATS = Map.of(
            "STAGE", List.of("intern"),
            "CDI", List.of("full time"),
            "CDD", List.of("contractor", "temporary", "part time"));

    private final RestClient restClient;

    /** Sources publiques sans cle : desactivables (OFFRES_SOURCES_PUBLIQUES=false), notamment dans les tests. */
    private final boolean active;

    public HimalayasSource(@Value("${app.offres.sources-publiques:true}") boolean active) {
        this.active = active;
        this.restClient = RestClient.builder().defaultHeader(HttpHeaders.USER_AGENT, "Postulyn/1.0").build();
    }

    record Reponse(Long totalCount, List<Offre> jobs) {
    }

    record Offre(String title, String excerpt, String companyName, String employmentType, Long minSalary,
            Long maxSalary, String currency, String salaryPeriod, Long pubDate, String applicationLink,
            String guid, List<String> locationRestrictions) {
    }

    @Override
    public String nom() {
        return "Himalayas";
    }

    @Override
    public boolean configuree() {
        return active;
    }

    @Override
    public boolean couvre(String pays) {
        return Catalogue.pays(pays).isPresent();
    }

    @Override
    public boolean supporte(String contrat) {
        return !"ALTERNANCE".equals(contrat);
    }

    @Override
    public String remarque(Recherche r) {
        String dansPays = Catalogue.pays(r.pays()).map(Catalogue.Pays::dans).orElse(r.pays());
        return "Offres en télétravail ouvertes aux candidats basés " + dansPays
                + " (souvent rédigées en anglais), via Himalayas.";
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        Reponse res = restClient.get()
                .uri(URL_API, b -> {
                    // Les stages sont rares : on les cherche explicitement ("intern")
                    String q = ("STAGE".equals(r.contrat()) ? r.requeteEn() + " intern" : r.requeteEn()).strip();
                    if (!q.isBlank()) {
                        b.queryParam("q", q);
                    }
                    return b.queryParam("country", r.pays().toUpperCase(Locale.ROOT))
                            .queryParam("page", r.page() + 1)
                            .build();
                })
                .retrieve()
                .body(Reponse.class);
        if (res == null || res.jobs() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<String> types = CONTRATS.get(r.contrat());
        List<OffreExterne> offres = res.jobs().stream()
                .filter(o -> types == null || o.employmentType() != null
                        && types.contains(o.employmentType().toLowerCase(Locale.ROOT)))
                .map(o -> convertir(o, r.pays()))
                .toList();
        long total = res.totalCount() != null ? res.totalCount() : offres.size();
        if (types != null) {
            // Filtre de contrat applique ici : on ne compte que les offres retenues jusqu'a cette page
            total = (long) r.page() * TAILLE_PAGE + offres.size();
        }
        return new ResultatSource(offres, total);
    }

    private OffreExterne convertir(Offre o, String pays) {
        LocalDate date = o.pubDate() == null ? null
                : LocalDate.ofInstant(Instant.ofEpochSecond(o.pubDate()), ZoneOffset.UTC);
        String url = o.applicationLink() != null ? o.applicationLink() : o.guid();
        return new OffreExterne(
                "himalayas-" + Integer.toHexString(String.valueOf(url).hashCode()),
                nom(),
                SourceOffres.sansHtml(o.title()),
                o.companyName(),
                "Télétravail" + Catalogue.pays(pays).map(p -> " · " + p.nom()).orElse(""),
                o.employmentType(),
                salaire(o),
                SourceOffres.sansHtml(o.excerpt()),
                url,
                date);
    }

    private static String salaire(Offre o) {
        if (o.minSalary() == null && o.maxSalary() == null) {
            return null;
        }
        String devise = o.currency() == null ? "" : " " + o.currency();
        String periode = o.salaryPeriod() == null ? "" : switch (o.salaryPeriod().toLowerCase(Locale.ROOT)) {
            case "annual", "yearly" -> " / an";
            case "monthly" -> " / mois";
            case "weekly" -> " / semaine";
            case "daily" -> " / jour";
            case "hourly" -> " / heure";
            default -> " / " + o.salaryPeriod();
        };
        String montant = o.minSalary() != null && o.maxSalary() != null && !o.minSalary().equals(o.maxSalary())
                ? milliers(o.minSalary()) + " – " + milliers(o.maxSalary())
                : milliers(o.minSalary() != null ? o.minSalary() : o.maxSalary());
        return montant + devise + periode;
    }

    private static String milliers(long n) {
        return String.format(Locale.FRANCE, "%,d", n).replace(' ', ' ').replace(' ', ' ');
    }
}
