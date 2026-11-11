package com.jobtracker.user;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Qui est administrateur :
 * - les administrateurs principaux, dont l'email figure dans ADMIN_EMAILS (variable Render, separee par des
 *   virgules) : ils ne peuvent pas perdre leurs droits depuis l'application ;
 * - les comptes nommes administrateurs depuis la page Administration (colonne users.administrateur).
 */
@Component
public class Administrateurs {

    private final Set<String> principaux;

    public Administrateurs(@Value("${app.admin.emails:}") String liste) {
        this.principaux = Arrays.stream(liste.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Administrateur principal (ADMIN_EMAILS). */
    public boolean estPrincipal(String email) {
        return email != null && principaux.contains(email.strip().toLowerCase(Locale.ROOT));
    }

    public boolean estAdmin(User user) {
        return user != null && (user.isAdministrateur() || estPrincipal(user.getEmail()));
    }
}
