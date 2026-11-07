package com.jobtracker.ia;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IaServiceTests {

    @Test
    void messagesDErreurComprehensibles() {
        assertTrue(IaService.messageErreur(429).contains("Reessaie dans une minute"));
        assertTrue(IaService.messageErreur(401).contains("AI_API_KEY"));
        assertTrue(IaService.messageErreur(404).contains("AI_MODEL"));
        assertTrue(IaService.messageErreur(503).contains("probleme de son cote"));
    }
}
