package com.jobtracker.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class IaServiceTests {

    @Test
    void messagesDErreurComprehensibles() {
        assertTrue(IaService.messageErreur(429).contains("Reessaie dans une minute"));
        assertTrue(IaService.messageErreur(401).contains("AI_API_KEY"));
        assertTrue(IaService.messageErreur(404).contains("AI_MODEL"));
        assertTrue(IaService.messageErreur(503).contains("probleme de son cote"));
    }

    @Test
    void messagesSelonLeDetailDuFournisseur() {
        assertTrue(IaService.messageErreur(413,
                "{\"error\":{\"message\":\"Request too large for model on tokens per minute (TPM): Limit 8000\"}}")
                .contains("Raccourcis"));
        assertTrue(IaService.messageErreur(429, "Rate limit reached on tokens per day (TPD)")
                .contains("limite quotidienne"));
        assertTrue(IaService.messageErreur(400, "{\"error\":{\"code\":\"json_validate_failed\"}}")
                .contains("mal formee"));
        assertTrue(IaService.messageErreur(429, "").contains("Reessaie dans une minute"));
    }

    @Test
    void onReessaieSeulementQuandCelaPeutReussir() {
        // Limite par minute : on attend le delai indique par le fournisseur
        assertEquals(Duration.ofSeconds(8), IaService.attenteAvantNouvelEssai(429, "", "7.2"));
        assertEquals(Duration.ofSeconds(6), IaService.attenteAvantNouvelEssai(429, "", null));
        // Limite journaliere (attente trop longue) : inutile de reessayer
        assertNull(IaService.attenteAvantNouvelEssai(429, "", "3600"));
        // JSON invalide genere par le modele : on relance tout de suite
        assertEquals(Duration.ZERO, IaService.attenteAvantNouvelEssai(400, "json_validate_failed", null));
        // Panne passagere
        assertEquals(Duration.ofSeconds(2), IaService.attenteAvantNouvelEssai(503, "", null));
        // Erreurs definitives
        assertNull(IaService.attenteAvantNouvelEssai(401, "", null));
        assertNull(IaService.attenteAvantNouvelEssai(400, "bad request", null));
        assertNull(IaService.attenteAvantNouvelEssai(413, "tokens per minute", null));
    }
}
