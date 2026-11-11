package com.jobtracker.offre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.RecommandationService.ProfilCv;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommandationTests {

    private static final ProfilCv PROFIL = RecommandationService.lire("""
            {"titre":"Développeur Java","resume":"…","metiers":["Développeur Java","Développeur backend"],
             "competences":["Java","Spring Boot","PostgreSQL","Docker","API REST","Git"],
             "motsCles":["CI/CD","JUnit"],"domaine":"INFORMATIQUE","niveau":"Junior","contrats":["ALTERNANCE"],
             "champInconnu":true}""", 4L, "cv.pdf");

    private static OffreExterne offre(String titre, String description) {
        return new OffreExterne("x", "s", titre, null, null, null, null, description, "https://o/" + titre, null);
    }

    @Test
    void leProfilEstLuMemeAvecDesChampsInattendus() {
        assertEquals("INFORMATIQUE", PROFIL.domaine());
        assertEquals(4L, PROFIL.cvId());
        assertEquals(2, PROFIL.metiersOuTitre().size());
    }

    @Test
    void lesOffresProchesDuCvSontMieuxNotees() {
        var proche = RecommandationService.noter(offre("Développeur Java H/F",
                "Vous développerez des API REST en Java / Spring Boot avec PostgreSQL et Docker. Git, JUnit."), PROFIL);
        var lointaine = RecommandationService.noter(offre("Comptable fournisseurs",
                "Saisie des factures, rapprochements bancaires, Excel."), PROFIL);
        assertTrue(proche.score() >= 80, "score " + proche.score());
        assertTrue(lointaine.score() < 20, "score " + lointaine.score());
        assertTrue(proche.motsTrouves().containsAll(List.of("Java", "Spring Boot", "PostgreSQL")), proche.motsTrouves().toString());
        // "Git" ne doit pas etre trouve dans "digital" (mot court : recherche du mot entier)
        var digital = RecommandationService.noter(offre("Chef de projet digital", "Strategie digitale"), PROFIL);
        assertTrue(!digital.motsTrouves().contains("Git"), digital.motsTrouves().toString());
    }
}
