package com.jobtracker.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobtracker.document.CvPdf;
import java.util.List;
import org.junit.jupiter.api.Test;

class CvAdaptationTests {

    private static final String EXTRACTION = """
            {"titre": "Développeur", "profil": "Profil d'origine",
             "competencesTechniques": ["Java", "Spring Boot"], "competencesTransverses": ["Autonomie"],
             "experiences": [
               {"poste": "Développeur (stage)", "entreprise": "DITS Group", "lieu": "Livry-sur-Seine",
                "periode": "Juin 2026 - Sept. 2026", "missions": ["API REST", "Tests", "Documentation"]},
               {"poste": "Vendeur", "entreprise": "Décathlon", "lieu": "Limoges", "periode": "2023",
                "missions": ["Conseil client"]},
               {"poste": "Développeur", "entreprise": "Yamo Group", "lieu": "Yaoundé", "periode": "2025",
                "missions": ["API Flask"]}],
             "projets": [{"nom": "Postulyn", "description": "Suivi de candidatures", "technologies": "Java",
                          "points": ["API", "Front"]}],
             "formations": [
               {"diplome": "Cycle ingénieur", "etablissement": "3IL", "lieu": "Limoges", "periode": "2025 - 2028", "details": ""},
               {"diplome": "Licence", "etablissement": "Université de Douala", "lieu": "Douala", "periode": "2022", "details": "Mention bien"},
               {"diplome": "Baccalauréat C", "etablissement": "Lycée de Bafang", "lieu": "Bafang", "periode": "2019", "details": ""}],
             "langues": ["Français : natif"], "atouts": ["Rigueur"], "centresInteret": ["Football"], "liens": [],
             "autresRubriques": [{"titre": "Certifications", "elements": ["AWS Cloud Practitioner"]}]}
            """;

    @Test
    void rienDuCvImporteNEstPerduNiChange() {
        // L'IA "oublie" une experience, deux formations, raccourcit une liste de missions et change un poste
        String ameliorations = """
                {"titre": "Développeur Java / Spring Boot - Alternance", "profil": "Nouveau profil",
                 "experiences": [
                   {"index": 0, "poste": "Lead developer", "missions": ["Développé une API REST sécurisée en Spring Boot",
                      "Écrit des tests JUnit", "Rédigé la documentation technique", "Participé aux rituels Scrum"]},
                   {"index": 2, "missions": []}],
                 "projets": [{"index": 0, "description": "Application de suivi de candidatures", "points": ["API"]}],
                 "formations": [{"index": 1, "details": "Mention bien, spécialité génie logiciel"}],
                 "autresRubriques": [{"index": 0, "elements": ["AWS Certified Cloud Practitioner (2025)"]}],
                 "competencesTechniquesAjoutees": ["Spring Data JPA", "java"],
                 "competencesTransversesAjoutees": ["Travail en équipe"],
                 "atoutsAjoutes": ["Curiosité", "rigueur"], "motsClesAjoutes": ["Spring Data JPA"]}
                """;
        CvAdaptation.CvAdapte cv = CvAdaptation.fusionner(CvAdaptation.lireExtraction(EXTRACTION),
                CvAdaptation.lireAmeliorations(ameliorations));

        assertEquals(3, cv.experiences().size());
        assertEquals("Développeur (stage)", cv.experiences().get(0).poste());
        assertEquals("DITS Group", cv.experiences().get(0).entreprise());
        assertEquals(4, cv.experiences().get(0).missions().size());
        assertEquals(List.of("Conseil client"), cv.experiences().get(1).missions());
        assertEquals(List.of("API Flask"), cv.experiences().get(2).missions());

        assertEquals(3, cv.formations().size());
        assertEquals("Mention bien, spécialité génie logiciel", cv.formations().get(1).details());
        assertEquals("Baccalauréat C", cv.formations().get(2).diplome());

        // Moins de points que l'original : on garde ceux du CV
        assertEquals(List.of("API", "Front"), cv.projets().getFirst().points());
        assertEquals("Application de suivi de candidatures", cv.projets().getFirst().description());

        assertEquals(List.of("Java", "Spring Boot", "Spring Data JPA"), cv.competencesTechniques());
        assertEquals(List.of("Autonomie", "Travail en équipe"), cv.competencesTransverses());
        assertEquals(List.of("Spring Data JPA", "Travail en équipe"), cv.competencesAjoutees());
        assertEquals(List.of("Rigueur", "Curiosité"), cv.atouts());
        assertEquals("Certifications", cv.autresRubriques().getFirst().titre());
        assertEquals("Développeur Java / Spring Boot - Alternance", cv.titre());
    }

    @Test
    void ameliorationsVidesRendentLeCvImporte() {
        CvAdaptation.CvAdapte cv = CvAdaptation.fusionner(CvAdaptation.lireExtraction(EXTRACTION),
                new CvAdaptation.Ameliorations(null, null, null, null, null, null, null, null, null, null));
        assertEquals("Profil d'origine", cv.profil());
        assertEquals(3, cv.experiences().size());
        assertTrue(CvAdaptation.versJson(cv).contains("\"autresRubriques\""));
    }

    @Test
    void lesRubriquesLibresApparaissentDansChaqueModele() {
        CvPdf.Cv base = CvAdaptation.lireExtraction(EXTRACTION);
        for (String modele : CvPdf.MODELES) {
            CvPdf.Cv cv = new CvPdf.Cv(base.titre(), base.profil(), base.competencesTechniques(),
                    base.competencesTransverses(), base.experiences(), base.projets(), base.formations(),
                    base.langues(), base.atouts(), base.centresInteret(), base.liens(), modele, false,
                    base.autresRubriques());
            String texte = texte(CvPdf.generer(new CvPdf.Contact("Franck Tague", "f@x.fr", null, "Limoges"), cv));
            assertTrue(texte.contains("CERTIFICATIONS") && texte.contains("AWS Cloud Practitioner"), modele + texte);
            assertTrue(texte.contains("Baccalauréat C") && texte.toUpperCase().contains("DÉCATHLON"), modele + texte);
        }
    }

    private static String texte(byte[] pdf) {
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
