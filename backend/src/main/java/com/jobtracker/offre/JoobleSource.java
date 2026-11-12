package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
 * API REST de Jooble (moteur de recherche d'emploi, tous contrats et tous domaines). Chaque site national de
 * Jooble a ses propres cles, gratuites (page "API" du site du pays, ex. https://fr.jooble.org/api/about) :
 * - JOOBLE_API_KEY + JOOBLE_HOST : la cle principale (fr.jooble.org pour la France) ;
 * - JOOBLE_CLES_PAYS (facultatif) : d'autres pays, au format "be=CLE,ca=CLE,ch=CLE" (code pays = sous-domaine).
 */
@Component
@Order(2)
public class JoobleSource implements SourceOffres {

    private static final Pattern DEPARTEMENT = Pattern.compile("^(\\d{2,3}|2[AaBb])$");
    private static final Map<String, String> MOTS_CONTRAT = Map.of(
            "ALTERNANCE", "alternance", "STAGE", "stage", "CDI", "CDI", "CDD", "CDD");

    /** Pays (code ISO) -> site Jooble a interroger et sa cle. */
    record Acces(String hote, String cle) {
    }

    private final RestClient restClient = RestClient.create();
    private final Map<String, Acces> acces = new LinkedHashMap<>();
    private final String hotePrincipal;

    public JoobleSource(
            @Value("${app.offres.jooble.api-key:}") String cle,
            @Value("${app.offres.jooble.host:jooble.org}") String hote,
            @Value("${app.offres.jooble.cles-pays:}") String clesPays) {
        String c = SourceOffres.nettoyer(cle);
        String h = SourceOffres.nettoyer(hote);
        this.hotePrincipal = h.isBlank() ? "jooble.org" : h.replaceFirst("^https?://", "").replaceAll("/+$", "");
        if (!c.isBlank()) {
            acces.put(paysDuHote(hotePrincipal), new Acces(hotePrincipal, c));
        }
        for (String paire : clesPays.split("[,;]")) {
            String[] morceaux = paire.split("=", 2);
            if (morceaux.length == 2 && !morceaux[0].isBlank() && !SourceOffres.nettoyer(morceaux[1]).isBlank()) {
                String pays = morceaux[0].strip().toLowerCase(Locale.ROOT);
                // Le site britannique de Jooble est uk.jooble.org (code ISO : GB)
                String sousDomaine = "gb".equals(pays) ? "uk" : pays;
                acces.put(("uk".equals(pays) ? "GB" : pays.toUpperCase(Locale.ROOT)),
                        new Acces(sousDomaine + ".jooble.org", SourceOffres.nettoyer(morceaux[1])));
            }
        }
    }

    /** fr.jooble.org -> FR ; uk.jooble.org -> GB ; jooble.org (site international) -> US. */
    static String paysDuHote(String hote) {
        String prefixe = hote.contains(".jooble.") ? hote.substring(0, hote.indexOf('.')) : "";
        if (prefixe.isEmpty() || "www".equals(prefixe)) {
            return "US";
        }
        return "uk".equals(prefixe) ? "GB" : prefixe.toUpperCase(Locale.ROOT);
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
        return !acces.isEmpty();
    }

    @Override
    public boolean couvre(String pays) {
        return acces.containsKey(pays.toUpperCase(Locale.ROOT));
    }

    @Override
    public String aideErreur(int statut) {
        if (statut != 403) {
            return "";
        }
        return ". Jooble ne reconnait pas la cle : chaque cle ne marche que sur le site du pays ou elle a ete "
                + "demandee (ex. une cle de fr.jooble.org demande JOOBLE_HOST=fr.jooble.org sur Render).";
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        Acces a = acces.get(r.pays().toUpperCase(Locale.ROOT));
        if (a == null) {
            return new ResultatSource(List.of(), 0);
        }
        String motsCles = r.requeteFr();
        String motContrat = MOTS_CONTRAT.get(r.contrat());
        if (motContrat != null && !motsCles.toLowerCase(Locale.ROOT).contains(motContrat.toLowerCase(Locale.ROOT))) {
            motsCles = (motsCles + " " + motContrat).strip();
        }
        Reponse res;
        try {
            res = restClient.post()
                    .uri("https://" + a.hote() + "/api/" + a.cle())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Requete(motsCles, lieu(r.lieu(), r.pays(), a.hote()), String.valueOf(r.page() + 1),
                            String.valueOf(TAILLE_PAGE)))
                    .retrieve()
                    .body(Reponse.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            if (e.getStatusCode().value() == 403) {
                throw new SourceBloqueeException(estProtectionAntiRobots(e.getResponseBodyAsString())
                        ? "Jooble bloque actuellement les recherches automatiques (protection anti-robots Cloudflare) : "
                                + "ta cle n'est pas en cause. Postulyn reessaiera automatiquement plus tard."
                        : "Jooble refuse la cle" + aideErreur(403), java.time.Duration.ofHours(1));
            }
            throw e;
        }
        if (res == null || res.jobs() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<OffreExterne> offres = res.jobs().stream().map(this::convertir).toList();
        return new ResultatSource(offres, res.totalCount() != null ? res.totalCount() : offres.size());
    }

    /** Page "Just a moment..." de Cloudflare au lieu d'une reponse JSON de l'API. */
    static boolean estProtectionAntiRobots(String corps) {
        String c = corps == null ? "" : corps.toLowerCase(Locale.ROOT);
        return c.contains("just a moment") || c.contains("cloudflare") || c.contains("cf-chl");
    }

    /**
     * Sur le site international, "Paris" designerait aussi Paris (Texas) : on precise le pays. Un numero de
     * departement francais ou un lieu vide donne tout le pays.
     */
    private static String lieu(String lieu, String pays, String hote) {
        String nomPays = Catalogue.pays(pays).map(Catalogue.Pays::nom).orElse("");
        if (lieu.isBlank() || "FR".equals(pays) && DEPARTEMENT.matcher(lieu).matches()) {
            return nomPays;
        }
        boolean siteNational = !hote.equals("jooble.org") && !hote.startsWith("www.");
        return siteNational || lieu.toLowerCase(Locale.ROOT).contains(nomPays.toLowerCase(Locale.ROOT))
                ? lieu : lieu + ", " + nomPays;
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
