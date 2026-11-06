package com.jobtracker.offre;

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
            @RequestParam(defaultValue = "") String source) {
        if (!CONTRATS.contains(contrat)) {
            throw new IllegalArgumentException("Type de contrat inconnu : " + contrat);
        }
        if (page < 0 || page > 50) {
            throw new IllegalArgumentException("Page invalide");
        }
        return offreService.rechercher(source, new Recherche(q.strip(), lieu.strip(), contrat, page));
    }
}
