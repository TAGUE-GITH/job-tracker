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

    public OffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    @GetMapping("/api/offres")
    public OffresResponse rechercher(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String lieu,
            @RequestParam(defaultValue = "") String contrat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "FR") String pays,
            @RequestParam(defaultValue = "") String domaine) {
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
        return offreService.rechercher(source, new Recherche(q.strip(), lieu.strip(), contrat, page, p.code(), d));
    }

    /** Listes des pays et domaines pour les filtres de la page Offres. */
    @GetMapping("/api/offres/criteres")
    public Criteres criteres() {
        return new Criteres(Catalogue.PAYS, java.util.Arrays.stream(Catalogue.Domaine.values())
                .map(d -> new DomaineOption(d.name(), d.libelle())).toList());
    }
}
