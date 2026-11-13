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

    /** Nombre de questions autorisees par adresse IP et par heure pour ORVYNA sans compte. */
    private static final int QUESTIONS_PAR_HEURE = 30;
    private final java.util.Map<String, java.util.Deque<java.time.Instant>> questionsParIp =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** ORVYNA sur la page d'accueil, sans compte : aide sur Postulyn et conseils de recherche d'emploi. */
    @PostMapping("/api/public/orvyna")
    public IaResponse aidePublique(@Valid @RequestBody AideRequest req,
            jakarta.servlet.http.HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        String ip = transmise != null && !transmise.isBlank() ? transmise.split(",")[0].strip() : requete.getRemoteAddr();
        java.time.Instant maintenant = java.time.Instant.now();
        java.util.Deque<java.time.Instant> recentes = questionsParIp.computeIfAbsent(ip,
                k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        while (!recentes.isEmpty() && recentes.peekFirst().isBefore(maintenant.minus(java.time.Duration.ofHours(1)))) {
            recentes.pollFirst();
        }
        if (recentes.size() >= QUESTIONS_PAR_HEURE) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Vous avez pose beaucoup de questions : reessayez dans un moment, ou creez un compte gratuit.");
        }
        recentes.addLast(maintenant);
        if (questionsParIp.size() > 10_000) {
            questionsParIp.clear();
        }
        return iaService.aidePublique(req.messages());
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

    /** ORVYNA repond aux questions sur l'application et sur les candidatures de l'utilisateur. */
    @PostMapping("/api/candidatures/{id}/ia/message")
    public IaResponse messageRecruteur(@PathVariable Long id,
            @Valid @RequestBody(required = false) LettreIaRequest req) {
        return iaService.messageRecruteur(CurrentUser.id(), id, req == null ? null : req.cvId(),
                req == null ? null : req.consignes());
    }

    @PostMapping("/api/ia/aide")
    public IaResponse aide(@Valid @RequestBody AideRequest req) {
        return iaService.aide(CurrentUser.id(), req.messages());
    }
}
