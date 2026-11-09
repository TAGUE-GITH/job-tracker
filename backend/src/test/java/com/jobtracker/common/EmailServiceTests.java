package com.jobtracker.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailServiceTests {

    private final EmailService service = new EmailService("xkeysib-cle-de-test", "moi@exemple.fr");

    @Test
    void sansConfigurationLeDiagnosticNommeLaVariableManquante() {
        EmailService.Envoi sansCle = new EmailService("", "moi@exemple.fr")
                .envoyerAvecDetail("a@b.fr", "Sujet", "Texte", "<p>Texte</p>");
        assertFalse(sansCle.envoye());
        assertTrue(sansCle.message().contains("BREVO_API_KEY"));

        EmailService.Envoi sansExpediteur = new EmailService("xkeysib-abc", " ")
                .envoyerAvecDetail("a@b.fr", "Sujet", "Texte", "<p>Texte</p>");
        assertTrue(sansExpediteur.message().contains("MAIL_FROM"));
    }

    @Test
    void lesRefusDeBrevoSontExpliques() {
        assertTrue(service.expliquer(401, "{\"code\":\"unauthorized\",\"message\":\"We have detected you are using "
                + "an unrecognised IP address 1.2.3.4\"}").contains("IP autorisees"));
        assertTrue(service.expliquer(400, "{\"message\":\"sender not valid\"}").contains("moi@exemple.fr"));
        assertTrue(service.expliquer(401, "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}")
                .contains("BREVO_API_KEY"));
        assertTrue(service.expliquer(403, "{\"message\":\"account not activated\"}").contains("activation"));
    }
}
