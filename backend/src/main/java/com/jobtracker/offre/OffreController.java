package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.Criteres;
import com.jobtracker.offre.OffreDtos.DomaineOption;
import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OffreController {

    private static final Set<String> CONTRATS = Set.of("", "ALTERNANCE", "STAGE", "CDI", "CDD");

    private final OffreService offreService;
    private final RechercheService rechercheService;

    public OffreController(OffreService offreService, RechercheService rechercheService) {
        this.offreService = offreService;
        this.rechercheService = rechercheService;
    }

    private static final Set<Integer> PERIODES = Set.of(0, 1, 3, 7, 30);

    /**
     * Recherche unifiee : la requete est comprise (sens, synonymes, traduction), toutes les sources du pays sont
     * interrogees et seules les offres pertinentes sont renvoyees. periode : offres publiees depuis N jours (0 = toutes).
     */
    @GetMapping("/api/offres/recherche")
    public RechercheService.Resultats recherche(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String lieu,
            @RequestParam(defaultValue = "FR") String pays,
            @RequestParam(defaultValue = "") String contrat,
            @RequestParam(defaultValue = "") String domaine,
            @RequestParam(defaultValue = "pertinence") String tri,
            @RequestParam(defaultValue = "0") int periode,
            @RequestParam(defaultValue = "0") int page) {
        if (!CONTRATS.contains(contrat)) {
            throw new IllegalArgumentException("Type de contrat inconnu : " + contrat);
        }
        if (!PERIODES.contains(periode) || page < 0 || page > 50 || q.length() > 200) {
            throw new IllegalArgumentException("Recherche invalide");
        }
        Catalogue.Pays p = Catalogue.pays(pays)
                .orElseThrow(() -> new IllegalArgumentException("Pays non pris en charge : " + pays));
        Catalogue.Domaine d = domaine.isBlank() ? null : Catalogue.domaine(domaine)
                .orElseThrow(() -> new IllegalArgumentException("Domaine inconnu : " + domaine));
        return rechercheService.rechercher(q, lieu.strip(), p.code(), contrat, d, "date".equalsIgnoreCase(tri),
                periode, page);
    }

    @GetMapping("/api/offres")
    public OffresResponse rechercher(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String lieu,
            @RequestParam(defaultValue = "") String contrat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "FR") String pays,
            @RequestParam(defaultValue = "") String domaine,
            @RequestParam(defaultValue = "pertinence") String tri) {
        if (!CONTRATS.contains(contrat)) {
            throw new IllegalArgumentException("Type de contrat inconnu : " + contrat);
        }
        if (page < 0 || page > 50) {
            throw new IllegalArgumentException("Page invalide");
        }
        Catalogue.Pays p = Catalogue.pays(pays)
                .orElseThrow(() -> new IllegalArgumentException("Pays non pris en charge : " + pays));
        Catalogue.Domaine d = domaine.isBlank() ? null : Catalogue.domaine(domaine)
                .orElseThrow(() -> new IllegalArgumentException("Domaine inconnu : " + domaine));
        return offreService.rechercher(source, new Recherche(q.strip(), lieu.strip(), contrat, page, p.code(), d,
                "date".equalsIgnoreCase(tri)));
    }

    /** Listes des pays et domaines pour les filtres de la page Offres. */
    @GetMapping("/api/offres/criteres")
    public Criteres criteres() {
        return new Criteres(Catalogue.PAYS, java.util.Arrays.stream(Catalogue.Domaine.values())
                .map(d -> new DomaineOption(d.name(), d.libelle())).toList());
    }
}
