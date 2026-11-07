package com.jobtracker.user;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.user.CompteService.Export;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compte")
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    public record MotDePasseRequest(
            @NotBlank String actuel,
            @NotBlank @Size(min = 6, message = "6 caracteres minimum") String nouveau) {
    }

    public record SuppressionRequest(@NotBlank String motDePasse) {
    }

    @PutMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(@Valid @RequestBody MotDePasseRequest req) {
        compteService.changerMotDePasse(CurrentUser.id(), req.actuel(), req.nouveau());
    }

    @GetMapping("/export")
    public Export exporter() {
        return compteService.exporter(CurrentUser.id());
    }

    // POST plutot que DELETE : le mot de passe est envoye dans le corps de la requete
    @PostMapping("/suppression")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@Valid @RequestBody SuppressionRequest req) {
        compteService.supprimer(CurrentUser.id(), req.motDePasse());
    }

    // 400 et non 401 : le frontend deconnecte l'utilisateur sur une 401
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail motDePasseIncorrect(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
