package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Careerjet (Optioncarriere en France) : moteur d'offres de nombreux pays, tous contrats dont les stages.
 * Cle gratuite depuis un compte editeur (https://www.optioncarriere.com/partners/api), a mettre dans
 * CAREERJET_API_KEY. L'API demande l'adresse IP et le navigateur de l'utilisateur qui fait la recherche.
 */
@Component
@Order(2)
public class CareerjetSource implements SourceOffres {

    private static final String URL_API = "https://search.api.careerjet.net/v4/query";
    /** Pays de Postulyn -> site Careerjet (code de langue et de pays). */
    static final Map<String, String> LOCALES = Map.ofEntries(
            Map.entry("FR", "fr_FR"), Map.entry("BE", "fr_BE"), Map.entry("CH", "fr_CH"), Map.entry("LU", "fr_LU"),
            Map.entry("CA", "fr_CA"), Map.entry("DE", "de_DE"), Map.entry("GB", "en_GB"), Map.entry("IE", "en_IE"),
            Map.entry("ES", "es_ES"), Map.entry("PT", "pt_PT"), Map.entry("IT", "it_IT"), Map.entry("NL", "nl_NL"),
            Map.entry("US", "en_US"), Map.entry("MA", "fr_MA"), Map.entry("TN", "fr_TN"), Map.entry("SN", "fr_SN"),
            Map.entry("CI", "fr_CI"), Map.entry("CM", "fr_CM"));
    private static final Map<String, String> CONTRATS = Map.of("CDI", "p", "CDD", "t", "STAGE", "i");

    private final RestClient restClient = RestClient.create();
    private final String cle;

    public CareerjetSource(@Value("${app.offres.careerjet.api-key:}") String cle) {
        this.cle = SourceOffres.nettoyer(cle);
    }

    record Reponse(String type, Long hits, String message, List<Offre> jobs) {
    }

    record Offre(String title, String company, String date, String description, String locations, String salary,
            String url) {
    }

    @Override
    public String nom() {
        return "Careerjet";
    }

    @Override
    public boolean configuree() {
        return !cle.isBlank();
    }

    @Override
    public boolean couvre(String pays) {
        return LOCALES.containsKey(pays.toUpperCase(Locale.ROOT));
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        String locale = LOCALES.get(r.pays().toUpperCase(Locale.ROOT));
        boolean francophone = locale.startsWith("fr_");
        String motsCles = francophone ? r.requeteFr() : r.requeteEn();
        if ("ALTERNANCE".equals(r.contrat())) {
            motsCles = (motsCles + " alternance").strip();
        }
        String[] client = client();
        String mots = motsCles;
        String identifiants = Base64.getEncoder().encodeToString((cle + ":").getBytes(StandardCharsets.UTF_8));
        Reponse res;
        try {
            res = restClient.get()
                    .uri(URL_API, b -> {
                        b.queryParam("locale_code", locale)
                                .queryParam("page", Math.min(r.page() + 1, 10))
                                .queryParam("page_size", TAILLE_PAGE)
                                .queryParam("fragment_size", 500)
                                .queryParam("sort", r.parDate() ? "date" : "relevance")
                                .queryParam("user_ip", client[0])
                                .queryParam("user_agent", client[1]);
                        if (!mots.isBlank()) {
                            b.queryParam("keywords", mots);
                        }
                        if (!r.lieu().isBlank()) {
                            b.queryParam("location", r.lieu());
                        }
                        if (CONTRATS.containsKey(r.contrat())) {
                            b.queryParam("contract_type", CONTRATS.get(r.contrat()));
                        }
                        return b.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + identifiants)
                    .retrieve()
                    .body(Reponse.class);
        } catch (RestClientResponseException e) {
            int statut = e.getStatusCode().value();
            if (statut == 400 && e.getResponseBodyAsString().toLowerCase(Locale.ROOT).contains("locale")) {
                // Pays non pris en charge par Careerjet : pas d'offre, sans erreur
                return new ResultatSource(List.of(), 0);
            }
            if (statut == 401 || statut == 403) {
                throw new SourceBloqueeException("Careerjet refuse la cle (CAREERJET_API_KEY) : verifie-la dans ton "
                        + "compte editeur Optioncarriere.", java.time.Duration.ofHours(1));
            }
            throw e;
        }
        // "LOCATIONS" : lieu inconnu ou ambigu, aucune recherche n'a eu lieu
        if (res == null || !"JOBS".equalsIgnoreCase(res.type()) || res.jobs() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<OffreExterne> offres = res.jobs().stream().map(o -> convertir(o, r.pays())).toList();
        return new ResultatSource(offres, res.hits() != null ? res.hits() : offres.size());
    }

    /** Adresse IP et navigateur de l'utilisateur qui fait la recherche (exiges par l'API). */
    private static String[] client() {
        String ip = "127.0.0.1";
        String agent = "Mozilla/5.0 (Postulyn)";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            HttpServletRequest requete = attributs.getRequest();
            String transmise = requete.getHeader("X-Forwarded-For");
            ip = transmise != null && !transmise.isBlank() ? transmise.split(",")[0].strip() : requete.getRemoteAddr();
            String navigateur = requete.getHeader(HttpHeaders.USER_AGENT);
            if (navigateur != null && !navigateur.isBlank()) {
                agent = navigateur;
            }
        }
        return new String[] {ip, agent};
    }

    static OffreExterne convertir(Offre o, String pays) {
        LocalDate date = null;
        if (o.date() != null && !o.date().isBlank()) {
            try {
                // "Wed,15 Nov 2025 19:13:43 GMT" : le nom du jour est ignore (il peut ne pas correspondre)
                String texte = o.date().strip().replaceFirst("^[A-Za-z]+,\\s*", "");
                date = ZonedDateTime.parse(texte, DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss zzz", Locale.ENGLISH))
                        .toLocalDate();
            } catch (RuntimeException e) {
                date = null;
            }
        }
        String lieu = o.locations() == null || o.locations().isBlank()
                ? Catalogue.pays(pays).map(Catalogue.Pays::nom).orElse(null) : o.locations();
        return new OffreExterne(
                "cj-" + Integer.toHexString(String.valueOf(o.url()).hashCode()),
                "Careerjet",
                SourceOffres.sansHtml(o.title()),
                o.company() == null || o.company().isBlank() ? null : o.company(),
                lieu,
                null,
                o.salary() == null || o.salary().isBlank() ? null : o.salary(),
                SourceOffres.sansHtml(o.description()),
                o.url(),
                date);
    }
}
