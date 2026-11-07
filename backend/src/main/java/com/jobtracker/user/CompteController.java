package com.jobtracker.user;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.user.AuthDtos.AuthResponse;
import com.jobtracker.user.CompteService.Export;
import com.jobtracker.user.CompteService.Profil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
            @NotBlank @Pattern(regexp = MotDePasse.REGLE, message = MotDePasse.MESSAGE) String nouveau) {
    }

    public record SuppressionRequest(@NotBlank String motDePasse) {
    }

    public record ProfilRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
            @Size(max = 30) String telephone,
            @Size(max = 300) String adresse,
            @Size(max = 100) String ville) {
    }

    @GetMapping("/profil")
    public Profil profil() {
        return compteService.profil(CurrentUser.id());
    }

    @PutMapping("/profil")
    public Profil modifierProfil(@Valid @RequestBody ProfilRequest req) {
        return compteService.modifierProfil(CurrentUser.id(), req.nom(), req.telephone(), req.adresse(), req.ville());
    }

    @PutMapping("/mot-de-passe")
    public AuthResponse changerMotDePasse(@Valid @RequestBody MotDePasseRequest req) {
        return compteService.changerMotDePasse(CurrentUser.id(), req.actuel(), req.nouveau());
    }

    @PostMapping("/deconnexion-partout")
    public AuthResponse deconnecterPartout() {
        return compteService.deconnecterPartout(CurrentUser.id());
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
