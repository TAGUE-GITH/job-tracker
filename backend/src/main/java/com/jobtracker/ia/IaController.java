package com.jobtracker.ia;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.ia.IaDtos.AideRequest;
import com.jobtracker.ia.IaDtos.AnalyseRequest;
import com.jobtracker.ia.IaDtos.LettreIaRequest;
import com.jobtracker.ia.IaDtos.IaRequest;
import com.jobtracker.ia.IaDtos.IaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IaController {

    private final IaService iaService;

    public IaController(IaService iaService) {
        this.iaService = iaService;
    }

    @PostMapping("/api/candidatures/{id}/ia")
    public IaResponse assister(@PathVariable Long id, @Valid @RequestBody IaRequest req) {
        return iaService.assister(CurrentUser.id(), id, req);
    }

    @PostMapping("/api/candidatures/{id}/ia/compatibilite")
    public IaResponse compatibilite(@PathVariable Long id, @RequestBody(required = false) AnalyseRequest req) {
        return iaService.compatibilite(CurrentUser.id(), id, req == null ? null : req.cvId());
    }

    @PostMapping("/api/candidatures/{id}/ia/relance")
    public IaResponse relance(@PathVariable Long id) {
        return iaService.relance(CurrentUser.id(), id);
    }

    @PostMapping("/api/candidatures/{id}/ia/cv")
    public IaResponse cvAdapte(@PathVariable Long id, @RequestBody(required = false) AnalyseRequest req) {
        return iaService.cvAdapte(CurrentUser.id(), id, req == null ? null : req.cvId());
    }

    @PostMapping("/api/candidatures/{id}/ia/lettre")
    public IaResponse lettre(@PathVariable Long id, @Valid @RequestBody(required = false) LettreIaRequest req) {
        return iaService.lettre(CurrentUser.id(), id, req == null ? null : req.cvId(),
                req == null ? null : req.consignes());
    }

    /** TAGIA repond aux questions sur l'application et sur les candidatures de l'utilisateur. */
    @PostMapping("/api/ia/aide")
    public IaResponse aide(@Valid @RequestBody AideRequest req) {
        return iaService.aide(CurrentUser.id(), req.messages());
    }
}
