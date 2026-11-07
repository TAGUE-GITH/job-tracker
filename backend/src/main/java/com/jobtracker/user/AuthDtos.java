package com.jobtracker.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Le nom est obligatoire") String nom,
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
            @NotBlank @Size(min = 6, message = "6 caracteres minimum") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    /** demo : compte de demonstration, supprime automatiquement apres quelques heures. */
    public record AuthResponse(String token, Long id, String nom, String email, boolean demo) {
    }
}
