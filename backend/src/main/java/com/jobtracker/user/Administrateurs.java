package com.jobtracker.user;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Comptes administrateurs : les emails listes dans ADMIN_EMAILS (separes par des virgules).
 * Rien n'est stocke en base : pour nommer ou retirer un administrateur, on modifie la variable sur Render.
 */
@Component
public class Administrateurs {

    private final Set<String> emails;

    public Administrateurs(@Value("${app.admin.emails:}") String liste) {
        this.emails = Arrays.stream(liste.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean estAdmin(String email) {
        return email != null && emails.contains(email.strip().toLowerCase(Locale.ROOT));
    }
}
