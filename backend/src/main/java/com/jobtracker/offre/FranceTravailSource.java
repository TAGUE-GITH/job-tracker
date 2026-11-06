package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

/**
 * API "Offres d'emploi v2" de France Travail (gratuite, inscription sur https://francetravail.io).
 * Authentification OAuth2 client_credentials, jeton mis en cache jusqu'a son expiration.
 */
@Component
@Order(3)
public class FranceTravailSource implements SourceOffres {

    private static final String URL_JETON =
            "https://entreprise.francetravail.fr/connexion/oauth2/access_token?realm=/partenaire";
    private static final String URL_API = "https://api.francetravail.io/partenaire/offresdemploi/v2";
    private static final String URL_DETAIL = "https://candidat.francetravail.fr/offres/recherche/detail/";
    private static final Pattern DEPARTEMENT = Pattern.compile("^(\\d{2,3}|2[AaBb])$");
    private static final Pattern TOTAL = Pattern.compile("/(\\d+)$");

    private static final Logger LOG = LoggerFactory.getLogger(FranceTravailSource.class);

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;

    private String jeton;
    private Instant expiration = Instant.EPOCH;

    public FranceTravailSource(
            @Value("${app.offres.france-travail.client-id:}") String clientId,
            @Value("${app.offres.france-travail.client-secret:}") String clientSecret) {
        this.clientId = SourceOffres.nettoyer(clientId);
        this.clientSecret = SourceOffres.nettoyer(clientSecret);
        if (configuree()) {
            // Aide au diagnostic sans exposer le secret : debut de l'identifiant et longueurs
            LOG.info("France Travail : identifiant {}... ({} caracteres), cle secrete de {} caracteres",
                    this.clientId.substring(0, Math.min(8, this.clientId.length())),
                    this.clientId.length(), this.clientSecret.length());
        }
    }

    record Jeton(String access_token, Long expires_in) {
    }

    record Reponse(List<Offre> resultats) {
    }

    record Offre(String id, String intitule, String description, String dateCreation, Lieu lieuTravail,
            Entreprise entreprise, String typeContratLibelle, Salaire salaire, Origine origineOffre,
            Contact contact) {
    }

    record Lieu(String libelle) {
    }

    record Entreprise(String nom) {
    }

    record Salaire(String libelle) {
    }

    record Origine(String urlOrigine) {
    }

    record Contact(String urlPostulation) {
    }

    @Override
    public String nom() {
        return "France Travail";
    }

    @Override
    public boolean configuree() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        int debut = r.page() * TAILLE_PAGE;
        ResponseEntity<Reponse> res = restClient.get()
                .uri(URL_API + "/offres/search", b -> parametres(b, r, debut))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Reponse.class);

        // 204 = aucune offre
        if (res.getBody() == null || res.getBody().resultats() == null) {
            return new ResultatSource(List.of(), 0);
        }
        List<OffreExterne> offres = res.getBody().resultats().stream().map(this::convertir).toList();
        long total = debut + offres.size();
        String plage = res.getHeaders().getFirst("Content-Range");
        if (plage != null) {
            Matcher m = TOTAL.matcher(plage.strip());
            if (m.find()) {
                total = Long.parseLong(m.group(1));
            }
        }
        return new ResultatSource(offres, total);
    }

    private URI parametres(UriBuilder b, Recherche r, int debut) {
        String motsCles = r.motsCles();
        switch (r.contrat()) {
            case "ALTERNANCE" -> b.queryParam("natureContrat", "E2,FS");
            case "CDI", "CDD" -> b.queryParam("typeContrat", r.contrat());
            // France Travail n'a pas de filtre "stage" : on l'ajoute aux mots-cles
            case "STAGE" -> motsCles = motsCles.isBlank() ? "stage" : motsCles + " stage";
            default -> { }
        }
        if (!motsCles.isBlank()) {
            b.queryParam("motsCles", motsCles);
        }
        if (DEPARTEMENT.matcher(r.lieu()).matches()) {
            b.queryParam("departement", r.lieu().toUpperCase());
        }
        b.queryParam("sort", 1);
        b.queryParam("range", debut + "-" + (debut + TAILLE_PAGE - 1));
        return b.build();
    }

    private OffreExterne convertir(Offre o) {
        String url = o.contact() != null && o.contact().urlPostulation() != null ? o.contact().urlPostulation()
                : o.origineOffre() != null && o.origineOffre().urlOrigine() != null ? o.origineOffre().urlOrigine()
                : URL_DETAIL + o.id();
        LocalDate date = o.dateCreation() != null && o.dateCreation().length() >= 10
                ? LocalDate.parse(o.dateCreation().substring(0, 10)) : null;
        return new OffreExterne(
                "ft-" + o.id(),
                nom(),
                o.intitule(),
                o.entreprise() != null ? o.entreprise().nom() : null,
                o.lieuTravail() != null ? o.lieuTravail().libelle() : null,
                o.typeContratLibelle(),
                o.salaire() != null ? o.salaire().libelle() : null,
                o.description(),
                url,
                date);
    }

    private synchronized String jeton() {
        if (jeton != null && Instant.now().isBefore(expiration)) {
            return jeton;
        }
        // Selon l'anciennete de l'application, France Travail attend l'un ou l'autre de ces scopes
        List<String> scopes = List.of(
                "api_offresdemploiv2 o2dsoffre",
                "api_offresdemploiv2",
                "application_" + clientId + " api_offresdemploiv2 o2dsoffre");
        Jeton j = null;
        for (String scope : scopes) {
            try {
                j = demanderJeton(scope);
                break;
            } catch (RestClientResponseException e) {
                String reponse = e.getResponseBodyAsString();
                if (reponse.contains("invalid_client")) {
                    throw new OffresIndisponiblesException("France Travail refuse les identifiants (invalid_client) : "
                            + "verifie FT_CLIENT_ID (identifiant client, " + clientId.length() + " caracteres recus) et "
                            + "FT_CLIENT_SECRET (cle secrete, " + clientSecret.length() + " caracteres recus) sur Render.");
                }
                if (!reponse.contains("invalid_scope")) {
                    throw e;
                }
                LOG.info("France Travail : scope \"{}\" refuse, essai suivant", scope);
            }
        }
        if (j == null) {
            throw new OffresIndisponiblesException("Identifiants France Travail valides, mais l'API "
                    + "\"Offres d'emploi v2\" n'est pas ajoutee a ton application sur francetravail.io "
                    + "(ou l'abonnement n'est pas encore actif).");
        }
        if (j.access_token() == null) {
            throw new OffresIndisponiblesException("France Travail n'a pas renvoye de jeton d'acces");
        }
        jeton = j.access_token();
        long duree = j.expires_in() != null ? j.expires_in() : 1500;
        expiration = Instant.now().plusSeconds(Math.max(60, duree - 60));
        return jeton;
    }

    private Jeton demanderJeton(String scope) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        return restClient.post()
                .uri(URL_JETON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Jeton.class);
    }
}
