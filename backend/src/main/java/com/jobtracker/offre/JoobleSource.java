package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * API REST de Jooble (moteur de recherche d'emploi, tous contrats). Cle gratuite sur
 * https://fr.jooble.org/api/about : une cle demandee sur le site francais donne les offres francaises
 * (JOOBLE_HOST=fr.jooble.org), une cle du site international passe par jooble.org.
 */
@Component
@Order(2)
public class JoobleSource implements SourceOffres {

    private static final Pattern DEPARTEMENT = Pattern.compile("^(\\d{2,3}|2[AaBb])$");
    private static final Map<String, String> MOTS_CONTRAT = Map.of(
            "ALTERNANCE", "alternance", "STAGE", "stage", "CDI", "CDI", "CDD", "CDD");

    private final RestClient restClient = RestClient.create();
    private final String cle;
    private final String hote;

    public JoobleSource(
            @Value("${app.offres.jooble.api-key:}") String cle,
            @Value("${app.offres.jooble.host:jooble.org}") String hote) {
        this.cle = SourceOffres.nettoyer(cle);
        String h = SourceOffres.nettoyer(hote);
        this.hote = h.isBlank() ? "jooble.org" : h.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    record Requete(String keywords, String location, String page, String ResultOnPage) {
    }

    record Reponse(Long totalCount, List<Offre> jobs) {
    }

    record Offre(Long id, String title, String location, String snippet, String salary, String source,
            String type, String link, String company, String updated) {
    }

    @Override
    public String nom() {
        return "Jooble";
    }

    @Override
    public boolean configuree() {
        return !cle.isBlank();
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        String motsCles = r.motsCles();
        String motContrat = MOTS_CONTRAT.get(r.contrat());
        if (motContrat != null && !motsCles.toLowerCase(Locale.ROOT).contains(motContrat.toLowerCase(Locale.ROOT))) {
            motsCles = (motsCles + " " + motContrat).strip();
        }
        Reponse res = restClient.post()
                .uri("https://" + hote + "/api/" + cle)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Requete(motsCles, lieu(r.lieu()), String.valueOf(r.page() + 1),
                        String.valueOf(TAILLE_PAGE)))
                .retrieve()
                .body(Reponse.class);
        if (res == null || res.jobs() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<OffreExterne> offres = res.jobs().stream().map(this::convertir).toList();
        return new ResultatSource(offres, res.totalCount() != null ? res.totalCount() : offres.size());
    }

    /** Sur jooble.org, "Paris" designerait aussi Paris (Texas) : on precise le pays. */
    private String lieu(String lieu) {
        if (lieu.isBlank() || DEPARTEMENT.matcher(lieu).matches()) {
            return "France";
        }
        return hote.startsWith("fr.") || lieu.toLowerCase(Locale.ROOT).contains("france") ? lieu : lieu + ", France";
    }

    private OffreExterne convertir(Offre o) {
        LocalDate date = o.updated() != null && o.updated().length() >= 10
                ? LocalDate.parse(o.updated().substring(0, 10)) : null;
        return new OffreExterne(
                "jooble-" + o.id(),
                nom(),
                SourceOffres.sansHtml(o.title()),
                blancEnNull(o.company()),
                blancEnNull(o.location()),
                blancEnNull(o.type()),
                blancEnNull(o.salary()),
                SourceOffres.sansHtml(o.snippet()),
                o.link(),
                date);
    }

    private static String blancEnNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }
}
