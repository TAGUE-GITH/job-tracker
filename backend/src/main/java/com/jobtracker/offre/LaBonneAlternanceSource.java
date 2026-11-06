package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * API "Recherche d'opportunites d'emploi en alternance" de La bonne alternance
 * (toutes les offres d'alternance en France, dont celles de France Travail).
 * Cle gratuite sur https://api.apprentissage.beta.gouv.fr. La recherche se fait par codes metier ROME :
 * les mots-cles sont traduits en codes ROME, puis servent a classer les offres.
 */
@Component
@Order(1)
public class LaBonneAlternanceSource implements SourceOffres {

    private static final String URL_API = "https://api.apprentissage.beta.gouv.fr/api/job/v1/search";
    private static final String URL_GEOCODAGE = "https://data.geopf.fr/geocodage/search";
    private static final String URL_SITE = "https://labonnealternance.apprentissage.beta.gouv.fr";
    private static final Pattern DEPARTEMENT = Pattern.compile("^(\\d{2,3}|2[AaBb])$");
    private static final Pattern CODE_ROME = Pattern.compile("^[A-Na-n]\\d{4}$");
    private static final int RAYON_KM = 30;

    /** Codes ROME par mot-cle (le premier mot-cle reconnu l'emporte). Informatique par defaut. */
    private static final List<Map.Entry<String, List<String>>> ROMES = List.of(
            Map.entry("data", List.of("M1403", "M1805", "M1806")),
            Map.entry("donnee", List.of("M1403", "M1805", "M1806")),
            Map.entry("reseau", List.of("M1801", "M1810", "I1401")),
            Map.entry("cyber", List.of("M1801", "M1802", "M1810")),
            Map.entry("securite", List.of("M1801", "M1802", "M1810")),
            Map.entry("support", List.of("M1801", "M1810", "M1802")),
            Map.entry("systeme", List.of("M1801", "M1810", "M1802")),
            Map.entry("design", List.of("E1205", "M1805")),
            Map.entry("graphiste", List.of("E1205")),
            Map.entry("marketing", List.of("M1705", "E1103")),
            Map.entry("communication", List.of("E1103", "M1705")),
            Map.entry("commercial", List.of("D1402", "D1406", "D1407")),
            Map.entry("vente", List.of("D1402", "D1406", "D1407")),
            Map.entry("comptab", List.of("M1203", "M1202")),
            Map.entry("ressources humaines", List.of("M1503", "M1501")),
            Map.entry("rh", List.of("M1503", "M1501")),
            Map.entry("gestion", List.of("M1205", "M1203", "M1402")),
            Map.entry("projet", List.of("M1806", "M1805", "M1402")));
    private static final List<String> ROMES_INFORMATIQUE = List.of("M1805", "M1806", "M1801", "M1810");

    private final RestClient restClient = RestClient.create();
    private final String cle;

    public LaBonneAlternanceSource(@Value("${app.offres.lba.api-key:}") String cle) {
        this.cle = SourceOffres.nettoyer(cle);
    }

    record Reponse(List<Offre> jobs) {
    }

    record Offre(Identifiant identifier, Contrat contract, Annonce offer, Lieu workplace, Candidature apply) {
    }

    record Identifiant(String id, String partner_job_id, String partner_label) {
    }

    record Contrat(List<String> type) {
    }

    record Annonce(String title, String description, String status, Publication publication) {
    }

    record Publication(String creation) {
    }

    record Lieu(String name, String brand, String legal_name, String website, Adresse location) {
    }

    record Adresse(String address) {
    }

    record Candidature(String url) {
    }

    record Geocodage(List<Commune> features) {
    }

    record Commune(Geometrie geometry) {
    }

    record Geometrie(List<Double> coordinates) {
    }

    @Override
    public String nom() {
        return "La bonne alternance";
    }

    @Override
    public boolean configuree() {
        return !cle.isBlank();
    }

    @Override
    public boolean supporte(String contrat) {
        return contrat.isEmpty() || "ALTERNANCE".equals(contrat);
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        List<String> romes = romes(r.motsCles());
        double[] coordonnees = DEPARTEMENT.matcher(r.lieu()).matches() || r.lieu().isBlank()
                ? null : geocoder(r.lieu());
        Reponse res = restClient.get()
                .uri(URL_API, b -> {
                    b.queryParam("romes", String.join(",", romes));
                    if (DEPARTEMENT.matcher(r.lieu()).matches()) {
                        b.queryParam("departements", r.lieu().toUpperCase(Locale.ROOT));
                    } else if (coordonnees != null) {
                        b.queryParam("latitude", coordonnees[1])
                                .queryParam("longitude", coordonnees[0])
                                .queryParam("radius", RAYON_KM);
                    }
                    return b.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cle)
                .retrieve()
                .body(Reponse.class);
        if (res == null || res.jobs() == null) {
            return new ResultatSource(List.of(), 0);
        }

        // Pas de pagination dans l'API : on classe par pertinence puis on decoupe en pages
        Set<String> mots = mots(r.motsCles());
        List<OffreExterne> offres = res.jobs().stream()
                .filter(o -> o.offer() == null || o.offer().status() == null
                        || "Active".equalsIgnoreCase(o.offer().status()))
                .map(this::convertir)
                .sorted(Comparator.comparingInt((OffreExterne o) -> -score(o, mots)))
                .toList();
        int debut = Math.min(r.page() * TAILLE_PAGE, offres.size());
        int fin = Math.min(debut + TAILLE_PAGE, offres.size());
        return new ResultatSource(offres.subList(debut, fin), offres.size());
    }

    static List<String> romes(String motsCles) {
        Set<String> codes = new LinkedHashSet<>();
        for (String mot : motsCles.split("[\\s,;]+")) {
            if (CODE_ROME.matcher(mot).matches()) {
                codes.add(mot.toUpperCase(Locale.ROOT));
            }
        }
        if (!codes.isEmpty()) {
            return List.copyOf(codes);
        }
        String texte = " " + sansAccents(motsCles) + " ";
        return ROMES.stream()
                .filter(e -> texte.contains(e.getKey().length() <= 3 ? " " + e.getKey() + " " : e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(ROMES_INFORMATIQUE);
    }

    private double[] geocoder(String lieu) {
        try {
            Geocodage g = restClient.get()
                    .uri(URL_GEOCODAGE, b -> b.queryParam("q", lieu).queryParam("type", "municipality")
                            .queryParam("limit", 1).build())
                    .retrieve()
                    .body(Geocodage.class);
            if (g != null && g.features() != null && !g.features().isEmpty()
                    && g.features().getFirst().geometry() != null
                    && g.features().getFirst().geometry().coordinates().size() == 2) {
                List<Double> c = g.features().getFirst().geometry().coordinates();
                return new double[] {c.get(0), c.get(1)};
            }
        } catch (RestClientException e) {
            // ville introuvable ou service indisponible : recherche dans toute la France
        }
        return null;
    }

    private OffreExterne convertir(Offre o) {
        Identifiant id = o.identifier();
        String identifiant = id == null ? null : id.id() != null ? id.id() : id.partner_job_id();
        Annonce a = o.offer();
        Lieu l = o.workplace();
        String entreprise = l == null ? null : l.name() != null ? l.name() : l.brand() != null ? l.brand() : l.legal_name();
        String date = a != null && a.publication() != null ? a.publication().creation() : null;
        return new OffreExterne(
                "lba-" + (identifiant != null ? identifiant : Integer.toHexString(o.hashCode())),
                nom(),
                a != null ? a.title() : null,
                entreprise,
                l != null && l.location() != null ? l.location().address() : null,
                o.contract() != null && o.contract().type() != null && !o.contract().type().isEmpty()
                        ? String.join(", ", o.contract().type()) : "Alternance",
                null,
                a != null ? SourceOffres.sansHtml(a.description()) : null,
                lien(o, identifiant),
                date != null && date.length() >= 10 ? LocalDate.parse(date.substring(0, 10)) : null);
    }

    private static String lien(Offre o, String identifiant) {
        if (o.apply() != null && o.apply().url() != null && !o.apply().url().isBlank()) {
            return o.apply().url();
        }
        if (identifiant != null && o.identifier().partner_label() != null) {
            return URL_SITE + "/emploi/" + o.identifier().partner_label() + "/" + identifiant + "/offre";
        }
        return URL_SITE;
    }

    private static Set<String> mots(String motsCles) {
        return Arrays.stream(sansAccents(motsCles).split("[^a-z0-9+#]+"))
                .filter(m -> m.length() > 1)
                .collect(Collectors.toSet());
    }

    private static int score(OffreExterne o, Set<String> mots) {
        String titre = sansAccents(o.titre() == null ? "" : o.titre());
        String description = sansAccents(o.description() == null ? "" : o.description());
        int score = 0;
        for (String mot : mots) {
            if (titre.contains(mot)) {
                score += 3;
            } else if (description.contains(mot)) {
                score += 1;
            }
        }
        return score;
    }

    private static String sansAccents(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
