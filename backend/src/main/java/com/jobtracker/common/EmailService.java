package com.jobtracker.common;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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

    /** Resultat d'un envoi : message comprehensible en cas d'echec (pour l'email de test de "Mon compte"). */
    public record Envoi(boolean envoye, String message) {
    }

    /** Envoie l'email ; renvoie faux en cas d'echec (l'erreur est journalisee, jamais montree a l'utilisateur). */
    public boolean envoyer(String destinataire, String sujet, String texte, String html) {
        return envoyerAvecDetail(destinataire, sujet, texte, html).envoye();
    }

    public Envoi envoyerAvecDetail(String destinataire, String sujet, String texte, String html) {
        if (cle.isBlank() || expediteur.isBlank()) {
            LOG.warn("Envoi d'email non configure (BREVO_API_KEY, MAIL_FROM). Email pour {} : {}\n{}",
                    destinataire, sujet, texte);
            return new Envoi(false, cle.isBlank()
                    ? "La variable BREVO_API_KEY n'est pas definie sur le serveur (Render > Environment)."
                    : "La variable MAIL_FROM n'est pas definie sur le serveur (Render > Environment).");
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
            return new Envoi(true, "Email envoye par Brevo depuis " + expediteur + ".");
        } catch (RestClientResponseException e) {
            String reponse = e.getResponseBodyAsString();
            LOG.error("Echec de l'envoi d'email a {} : {} {}", destinataire, e.getStatusCode().value(), reponse);
            return new Envoi(false, expliquer(e.getStatusCode().value(), reponse));
        } catch (RestClientException e) {
            LOG.error("Echec de l'envoi d'email a {} : {}", destinataire, e.getMessage());
            return new Envoi(false, "Brevo est injoignable : " + e.getMessage());
        }
    }

    /** Traduit les refus les plus courants de Brevo en consigne a suivre. */
    String expliquer(int statut, String reponse) {
        String r = reponse == null ? "" : reponse.toLowerCase(Locale.ROOT);
        if (r.matches("(?s).*\\bip\\b.*")) {
            return "Brevo bloque l'adresse IP du serveur Render. Dans Brevo : Securite > IP autorisees, "
                    + "desactive le blocage des IP inconnues (ou autorise l'IP indiquee). Reponse : " + reponse;
        }
        if (r.contains("sender")) {
            return "L'expediteur " + expediteur + " n'est pas valide dans Brevo : ajoute-le et valide-le dans "
                    + "Expediteurs, domaines et IP, et mets exactement la meme adresse dans MAIL_FROM. Reponse : " + reponse;
        }
        if (statut == 401) {
            return "Brevo refuse la cle API (" + cle.substring(0, Math.min(8, cle.length())) + "..., "
                    + cle.length() + " caracteres) : genere une cle dans SMTP & API > Cles API et recopie-la "
                    + "entierement dans BREVO_API_KEY. Reponse : " + reponse;
        }
        if (statut == 403 || r.contains("not activated") || r.contains("activat")) {
            return "Ton compte Brevo n'est pas encore autorise a envoyer des emails transactionnels : termine "
                    + "l'activation du compte dans Brevo (profil, verification). Reponse : " + reponse;
        }
        return "Brevo a refuse l'envoi (erreur " + statut + ") : " + reponse;
    }
}
