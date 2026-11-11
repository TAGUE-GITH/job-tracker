package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Arbeitnow (https://www.arbeitnow.com) : offres sur place en Allemagne, tous domaines. API publique sans cle,
 * sans recherche : les dernieres offres sont gardees une heure en memoire et filtrees ici.
 */
@Component
@Order(4)
public class ArbeitnowSource implements SourceOffres {

    private static final Logger LOG = LoggerFactory.getLogger(ArbeitnowSource.class);
    private static final String URL_API = "https://www.arbeitnow.com/api/job-board-api";
    private static final int PAGES = 3;
    private static final Duration DUREE_CACHE = Duration.ofHours(1);
    /** Mots des types de contrat d'Arbeitnow (allemand ou anglais) pour chacun de nos contrats. */
    private static final Map<String, List<String>> CONTRATS = Map.of(
            "ALTERNANCE", List.of("ausbildung", "werkstudent", "dual", "apprentice"),
            "STAGE", List.of("praktikum", "intern"),
            "CDI", List.of("unbefristet", "permanent", "festanstellung", "full time", "vollzeit"),
            "CDD", List.of("befristet", "temporary", "fixed-term"));

    private final RestClient restClient;
    private volatile List<Offre> cache = List.of();
    private volatile Instant chargeLe = Instant.EPOCH;

    /** Sources publiques sans cle : desactivables (OFFRES_SOURCES_PUBLIQUES=false), notamment dans les tests. */
    private final boolean active;

    public ArbeitnowSource(@Value("${app.offres.sources-publiques:true}") boolean active) {
        this.active = active;
        this.restClient = RestClient.builder().defaultHeader(HttpHeaders.USER_AGENT, "Postulyn/1.0").build();
    }

    record Reponse(List<Offre> data) {
    }

    record Offre(String slug, String company_name, String title, String description, Boolean remote, String url,
            List<String> tags, List<String> job_types, String location, Long created_at) {
    }

    @Override
    public String nom() {
        return "Arbeitnow";
    }

    @Override
    public boolean configuree() {
        return active;
    }

    @Override
    public boolean couvre(String pays) {
        return "DE".equalsIgnoreCase(pays);
    }

    @Override
    public String remarque(Recherche r) {
        return "Offres en Allemagne publiées sur Arbeitnow, souvent rédigées en allemand ou en anglais.";
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        List<String> mots = Arrays.stream(r.motsCles().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(m -> m.length() > 1).toList();
        String lieu = r.lieu().toLowerCase(Locale.ROOT).strip();
        List<String> types = CONTRATS.get(r.contrat());
        List<OffreExterne> filtrees = offres().stream()
                .filter(o -> {
                    String titre = (o.title() + " " + String.join(" ", liste(o.tags()))).toLowerCase(Locale.ROOT);
                    String tout = titre + " " + String.valueOf(o.description()).toLowerCase(Locale.ROOT);
                    return mots.stream().allMatch(tout::contains)
                            && (r.domaine() == null || r.domaine().reconnu(titre))
                            && (lieu.isEmpty() || String.valueOf(o.location()).toLowerCase(Locale.ROOT).contains(lieu))
                            && (types == null || liste(o.job_types()).stream()
                                    .map(t -> t.toLowerCase(Locale.ROOT)).anyMatch(t -> types.stream().anyMatch(t::contains)));
                })
                .map(this::convertir)
                .toList();
        int debut = Math.min(r.page() * TAILLE_PAGE, filtrees.size());
        return new ResultatSource(filtrees.subList(debut, Math.min(debut + TAILLE_PAGE, filtrees.size())),
                filtrees.size());
    }

    /** Dernieres offres, rechargees au plus une fois par heure. */
    private List<Offre> offres() {
        if (Duration.between(chargeLe, Instant.now()).compareTo(DUREE_CACHE) < 0 && !cache.isEmpty()) {
            return cache;
        }
        synchronized (this) {
            if (Duration.between(chargeLe, Instant.now()).compareTo(DUREE_CACHE) < 0 && !cache.isEmpty()) {
                return cache;
            }
            List<Offre> toutes = new ArrayList<>();
            for (int page = 1; page <= PAGES; page++) {
                try {
                    int p = page;
                    Reponse res = restClient.get().uri(URL_API, b -> b.queryParam("page", p).build())
                            .retrieve().body(Reponse.class);
                    if (res == null || res.data() == null || res.data().isEmpty()) {
                        break;
                    }
                    toutes.addAll(res.data());
                } catch (RestClientException e) {
                    if (toutes.isEmpty() && cache.isEmpty()) {
                        throw e;
                    }
                    LOG.warn("Arbeitnow : page {} non chargee ({})", page, e.getMessage());
                    break;
                }
            }
            if (!toutes.isEmpty()) {
                cache = List.copyOf(toutes);
                chargeLe = Instant.now();
            }
            return cache;
        }
    }

    private OffreExterne convertir(Offre o) {
        LocalDate date = o.created_at() == null ? null
                : LocalDate.ofInstant(Instant.ofEpochSecond(o.created_at()), ZoneOffset.UTC);
        String description = SourceOffres.sansHtml(o.description());
        if (description != null && description.length() > 600) {
            description = description.substring(0, 600).strip() + "…";
        }
        String lieu = Boolean.TRUE.equals(o.remote()) ? o.location() + " · Télétravail possible" : o.location();
        return new OffreExterne(
                "arbeitnow-" + o.slug(),
                nom(),
                SourceOffres.sansHtml(o.title()),
                o.company_name(),
                lieu,
                liste(o.job_types()).isEmpty() ? null : String.join(", ", o.job_types()),
                null,
                description,
                o.url(),
                date);
    }

    private static List<String> liste(List<String> l) {
        return l == null ? List.of() : l;
    }
}
