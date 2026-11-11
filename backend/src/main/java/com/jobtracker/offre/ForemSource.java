package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le Forem (service public de l'emploi en Wallonie) : toutes les offres publiees en Belgique, en donnees ouvertes
 * sur https://www.odwb.be (API Opendatasoft, sans cle). Recherche sur le titre et le metier, tri par date.
 */
@Component
@Order(1)
public class ForemSource implements SourceOffres {

    private static final String URL_API =
            "https://www.odwb.be/api/explore/v2.1/catalog/datasets/offres-d-emploi-forem/records";
    private static final Set<String> CONTRATS = Set.of("", "CDI", "CDD");

    private final RestClient restClient;
    private final boolean active;

    public ForemSource(@Value("${app.offres.sources-publiques:true}") boolean active) {
        this.active = active;
        this.restClient = RestClient.builder().defaultHeader(HttpHeaders.USER_AGENT, "Postulyn/1.0").build();
    }

    record Reponse(Long total_count, List<Offre> results) {
    }

    record Offre(String numerooffreforem, String titreoffre, List<String> lieuxtravaillocalite,
            String typecontrat, String nomemployeur, String regimetravail, List<String> secteurs, String url,
            String datedebutdiffusion, String metier, List<String> niveauxetudes, String experiencerequise) {
    }

    @Override
    public String nom() {
        return "Le Forem";
    }

    @Override
    public boolean configuree() {
        return active;
    }

    @Override
    public boolean couvre(String pays) {
        return "BE".equalsIgnoreCase(pays);
    }

    @Override
    public boolean supporte(String contrat) {
        return CONTRATS.contains(contrat);
    }

    @Override
    public String remarque(Recherche r) {
        return "Offres publiées en Belgique, via le Forem (service public de l'emploi en Wallonie).";
    }

    /** Filtre ODSQL : mots-cles sur le titre ou le metier, lieu, contrat. */
    static String filtre(Recherche r) {
        List<String> conditions = new ArrayList<>();
        for (String mot : texte(r.requeteFr()).split("\\s+")) {
            if (mot.length() > 1) {
                conditions.add("(search(titreoffre, \"" + mot + "\") or search(metier, \"" + mot + "\"))");
            }
        }
        String lieu = texte(r.lieu());
        if (!lieu.isBlank()) {
            conditions.add("(search(lieuxtravaillocalite, \"" + lieu + "\") or search(lieuxtravailregion, \"" + lieu
                    + "\") or lieuxtravailcodepostal like \"" + lieu + "%\")");
        }
        if ("CDI".equals(r.contrat())) {
            conditions.add("typecontrat like \"%indéterminée%\"");
        } else if ("CDD".equals(r.contrat())) {
            conditions.add("(typecontrat like \"Durée déterminée\" or typecontrat = \"Remplacement\")");
        }
        return String.join(" and ", conditions);
    }

    /** Retire les caracteres qui casseraient la requete. */
    private static String texte(String valeur) {
        return valeur.replaceAll("[\"\\\\%()]", " ").strip();
    }

    @Override
    public ResultatSource rechercher(Recherche r) {
        String where = filtre(r);
        Reponse res = restClient.get()
                .uri(URL_API, b -> {
                    b.queryParam("limit", TAILLE_PAGE)
                            .queryParam("offset", r.page() * TAILLE_PAGE)
                            .queryParam("order_by", "datedebutdiffusion desc");
                    if (!where.isBlank()) {
                        b.queryParam("where", "{where}");
                    }
                    return b.build(where);
                })
                .retrieve()
                .body(Reponse.class);
        if (res == null || res.results() == null) {
            return new ResultatSource(List.of(), 0);
        }
        return new ResultatSource(res.results().stream().map(this::convertir).toList(),
                res.total_count() != null ? res.total_count() : res.results().size());
    }

    private OffreExterne convertir(Offre o) {
        LocalDate date = o.datedebutdiffusion() != null && o.datedebutdiffusion().length() >= 10
                ? LocalDate.parse(o.datedebutdiffusion().substring(0, 10)) : null;
        List<String> details = new ArrayList<>();
        if (o.metier() != null) {
            details.add("Métier : " + o.metier());
        }
        if (o.secteurs() != null && !o.secteurs().isEmpty()) {
            details.add("Secteur : " + String.join(", ", o.secteurs()));
        }
        if (o.regimetravail() != null) {
            details.add("Régime : " + o.regimetravail());
        }
        if (o.niveauxetudes() != null && !o.niveauxetudes().isEmpty()) {
            details.add("Études : " + String.join(", ", o.niveauxetudes()));
        }
        if (o.experiencerequise() != null) {
            details.add("Expérience : " + o.experiencerequise());
        }
        String lieu = o.lieuxtravaillocalite() == null || o.lieuxtravaillocalite().isEmpty() ? "Belgique"
                : capitaliser(String.join(", ", o.lieuxtravaillocalite())) + ", Belgique";
        return new OffreExterne(
                "forem-" + o.numerooffreforem(),
                nom(),
                o.titreoffre(),
                o.nomemployeur() == null ? null : capitaliser(o.nomemployeur()),
                lieu,
                o.typecontrat(),
                null,
                String.join(" · ", details),
                o.url(),
                date);
    }

    /** "ADECCO PERSONNEL SERVICES" -> "Adecco Personnel Services" */
    static String capitaliser(String texte) {
        StringBuilder sb = new StringBuilder();
        boolean debutMot = true;
        for (char c : texte.toLowerCase().toCharArray()) {
            sb.append(debutMot ? Character.toUpperCase(c) : c);
            debutMot = !Character.isLetterOrDigit(c);
        }
        return sb.toString();
    }
}
