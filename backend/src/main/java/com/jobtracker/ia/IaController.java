package com.jobtracker.ia;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.ia.IaDtos.AideRequest;
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

    /** TAGIA repond aux questions sur l'application et sur les candidatures de l'utilisateur. */
    @PostMapping("/api/ia/aide")
    public IaResponse aide(@Valid @RequestBody AideRequest req) {
        return iaService.aide(CurrentUser.id(), req.messages());
    }
}
