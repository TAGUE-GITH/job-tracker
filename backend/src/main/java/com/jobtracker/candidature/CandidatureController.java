package com.jobtracker.candidature;

import com.jobtracker.candidature.CandidatureDtos.CandidatureRequest;
import com.jobtracker.candidature.CandidatureDtos.CandidatureResponse;
import com.jobtracker.candidature.CandidatureDtos.LettreRequest;
import com.jobtracker.common.CurrentUser;
import com.jobtracker.etape.EtapeDtos.EtapeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidatures")
public class CandidatureController {

    private final CandidatureService service;

    public CandidatureController(CandidatureService service) {
        this.service = service;
    }

    @GetMapping
    public List<CandidatureResponse> lister(@RequestParam(required = false) StatutCandidature statut,
            @RequestParam(required = false) String q) {
        return service.lister(CurrentUser.id(), statut, q);
    }

    @GetMapping("/{id}")
    public CandidatureResponse detail(@PathVariable Long id) {
        return service.detail(CurrentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureResponse creer(@Valid @RequestBody CandidatureRequest req) {
        return service.creer(CurrentUser.id(), req);
    }

    @PutMapping("/{id}")
    public CandidatureResponse modifier(@PathVariable Long id, @Valid @RequestBody CandidatureRequest req) {
        return service.modifier(CurrentUser.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(CurrentUser.id(), id);
    }

    @PostMapping("/{id}/lettre")
    public CandidatureResponse enregistrerLettre(@PathVariable Long id, @Valid @RequestBody LettreRequest req) {
        return service.enregistrerLettre(CurrentUser.id(), id, req);
    }

    @PostMapping("/{id}/etapes")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureResponse ajouterEtape(@PathVariable Long id, @Valid @RequestBody EtapeRequest req) {
        return service.ajouterEtape(CurrentUser.id(), id, req);
    }

    @PutMapping("/{id}/etapes/{etapeId}")
    public CandidatureResponse modifierEtape(@PathVariable Long id, @PathVariable Long etapeId,
            @Valid @RequestBody EtapeRequest req) {
        return service.modifierEtape(CurrentUser.id(), id, etapeId, req);
    }

    @DeleteMapping("/{id}/etapes/{etapeId}")
    public CandidatureResponse supprimerEtape(@PathVariable Long id, @PathVariable Long etapeId) {
        return service.supprimerEtape(CurrentUser.id(), id, etapeId);
    }
}
