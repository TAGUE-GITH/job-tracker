package com.jobtracker.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Protection contre les attaques par essais repetes : apres 5 echecs de connexion sur un meme email,
 * le compte est bloque 15 minutes. Les compteurs sont en memoire (une seule instance du serveur).
 */
@Component
public class TentativesConnexion {

    static final int MAX_ECHECS = 5;
    static final Duration BLOCAGE = Duration.ofMinutes(15);

    private record Etat(int echecs, Instant bloqueJusqua) {
    }

    private final Map<String, Etat> etats = new ConcurrentHashMap<>();

    /** Leve TropDeTentativesException si l'email est bloque. */
    public void verifier(String email) {
        Etat etat = etats.get(email);
        if (etat != null && etat.bloqueJusqua() != null) {
            if (Instant.now().isBefore(etat.bloqueJusqua())) {
                long minutes = Math.max(1, Duration.between(Instant.now(), etat.bloqueJusqua()).toMinutes() + 1);
                throw new TropDeTentativesException("Trop de tentatives de connexion. Reessaie dans "
                        + minutes + " minute" + (minutes > 1 ? "s" : "") + " ou reinitialise ton mot de passe.");
            }
            etats.remove(email);
        }
    }

    public void echec(String email) {
        etats.compute(email, (cle, etat) -> {
            int echecs = (etat == null || etat.bloqueJusqua() != null ? 0 : etat.echecs()) + 1;
            return new Etat(echecs, echecs >= MAX_ECHECS ? Instant.now().plus(BLOCAGE) : null);
        });
    }

    public void succes(String email) {
        etats.remove(email);
    }

    public static class TropDeTentativesException extends RuntimeException {

        public TropDeTentativesException(String message) {
            super(message);
        }
    }
}
