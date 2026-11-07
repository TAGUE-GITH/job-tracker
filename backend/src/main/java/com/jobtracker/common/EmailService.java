package com.jobtracker.common;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envoi d'emails transactionnels via l'API HTTP de Brevo (gratuite jusqu'a 300 emails par jour).
 * HTTP plutot que SMTP : les hebergeurs gratuits bloquent souvent les ports SMTP.
 * Sans cle (developpement), l'email est simplement ecrit dans les logs.
 */
@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient = RestClient.create();
    private final String cle;
    private final String expediteur;

    public EmailService(@Value("${app.mail.brevo-api-key:}") String cle,
            @Value("${app.mail.expediteur:}") String expediteur) {
        this.cle = cle.strip();
        this.expediteur = expediteur.strip();
    }

    public boolean configure() {
        return !cle.isBlank() && !expediteur.isBlank();
    }

    /** Envoie l'email ; renvoie faux en cas d'echec (l'erreur est journalisee, jamais montree a l'utilisateur). */
    public boolean envoyer(String destinataire, String sujet, String texte, String html) {
        if (!configure()) {
            LOG.warn("Envoi d'email non configure (BREVO_API_KEY, MAIL_FROM). Email pour {} : {}\n{}",
                    destinataire, sujet, texte);
            return false;
        }
        try {
            restClient.post()
                    .uri(URL_BREVO)
                    .header("api-key", cle)
                    .body(Map.of(
                            "sender", Map.of("name", "Taguora", "email", expediteur),
                            "to", List.of(Map.of("email", destinataire)),
                            "subject", sujet,
                            "textContent", texte,
                            "htmlContent", html))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            LOG.error("Echec de l'envoi d'email a {} : {}", destinataire, e.getMessage());
            return false;
        }
    }
}
