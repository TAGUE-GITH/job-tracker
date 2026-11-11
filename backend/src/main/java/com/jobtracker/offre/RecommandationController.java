package com.jobtracker.offre;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.offre.RecommandationService.ProfilCv;
import com.jobtracker.offre.RecommandationService.Recommandations;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Profil tire du CV et offres recommandees ("Offres pour moi"). */
@RestController
public class RecommandationController {

    private static final Set<String> CONTRATS = Set.of("", "ALTERNANCE", "STAGE", "CDI", "CDD");

    private final RecommandationService service;

    public RecommandationController(RecommandationService service) {
        this.service = service;
    }

    public record AnalyseCvRequest(@NotNull(message = "Choisis un CV") Long cvId) {
    }

    @PostMapping("/api/profil-cv")
    public ProfilCv analyser(@jakarta.validation.Valid @RequestBody AnalyseCvRequest req) {
        return service.analyser(CurrentUser.id(), req.cvId());
    }

    @GetMapping("/api/profil-cv")
    public ResponseEntity<ProfilCv> profil() {
        ProfilCv p = service.profil(CurrentUser.id());
        return p == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(p);
    }

    @DeleteMapping("/api/profil-cv")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void oublier() {
        service.oublier(CurrentUser.id());
    }

    @GetMapping("/api/offres/pour-moi")
    public Recommandations pourMoi(
            @RequestParam(defaultValue = "FR") String pays,
            @RequestParam(defaultValue = "") String contrat,
            @RequestParam(defaultValue = "") String lieu,
            @RequestParam(defaultValue = "pertinence") String tri) {
        if (!CONTRATS.contains(contrat)) {
            throw new IllegalArgumentException("Type de contrat inconnu : " + contrat);
        }
        Catalogue.Pays p = Catalogue.pays(pays)
                .orElseThrow(() -> new IllegalArgumentException("Pays non pris en charge : " + pays));
        return service.recommander(CurrentUser.id(), p.code(), contrat, lieu.strip(), "date".equalsIgnoreCase(tri));
    }
}
