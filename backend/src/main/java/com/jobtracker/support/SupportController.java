package com.jobtracker.support;

import com.jobtracker.common.CurrentUser;
import com.jobtracker.support.MessageContact.Categorie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Page Contact (ouverte a tous) et avis des utilisateurs connectes. */
@RestController
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    public record ContactRequest(
            @NotBlank(message = "Ton nom est obligatoire") @Size(max = 100) String nom,
            @NotBlank(message = "Ton email est obligatoire") @Email(message = "Email invalide") @Size(max = 255) String email,
            @NotNull(message = "Choisis le type de demande") Categorie categorie,
            @NotBlank(message = "Le sujet est obligatoire") @Size(max = 150) String sujet,
            @NotBlank(message = "Le message est vide") @Size(min = 10, max = 5000,
                    message = "Le message doit faire entre 10 et 5000 caracteres") String message) {
    }

    public record AvisRequest(
            @Min(value = 1, message = "Choisis une note de 1 a 5") @Max(value = 5, message = "Choisis une note de 1 a 5") int note,
            @Size(max = 2000) String commentaire) {
    }

    @PostMapping("/api/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportService.MessageVue contacter(@Valid @RequestBody ContactRequest req) {
        return supportService.envoyerMessage(utilisateurConnecte(), req.nom(), req.email(), req.categorie(),
                req.sujet(), req.message());
    }

    @PostMapping("/api/avis")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void donnerAvis(@Valid @RequestBody AvisRequest req) {
        supportService.donnerAvis(CurrentUser.id(), req.note(), req.commentaire());
    }

    /** La page Contact est publique : on rattache le message au compte seulement si la personne est connectee. */
    private static Long utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt && jwt.getClaim("uid") instanceof Number uid) {
            return uid.longValue();
        }
        return null;
    }
}
