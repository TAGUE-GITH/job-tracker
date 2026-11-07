package com.jobtracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.ia.api-key=",
        "app.offres.france-travail.client-id=",
        "app.offres.lba.api-key=",
        "app.offres.jooble.api-key=",
        "app.offres.adzuna.app-id="
})
@AutoConfigureMockMvc
class JobTrackerApplicationTests {

    @Autowired
    private MockMvc mvc;

    private String token;

    @BeforeEach
    void inscription() throws Exception {
        String email = "user" + System.nanoTime() + "@test.fr";
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Franck","email":"%s","password":"secret123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + extraire(body, "token");
    }

    @Test
    void refuseLesRequetesSansToken() throws Exception {
        mvc.perform(get("/api/candidatures")).andExpect(status().isUnauthorized());
    }

    @Test
    void parcoursCompletDUneCandidature() throws Exception {
        String body = mvc.perform(post("/api/candidatures").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomEntreprise":"Acme","poste":"Dev Java","dateCandidature":"2026-10-01",
                                 "texteOffre":"Spring Boot, React, Docker"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.etapes[0].type").value("CANDIDATURE_ENVOYEE"))
                .andReturn().getResponse().getContentAsString();
        String id = extraire(body, "id");

        mvc.perform(post("/api/candidatures/" + id + "/etapes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ECHANGE_RH","date":"2026-10-08","notes":"Appel de 20 min"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.etapes.length()").value(2));

        mvc.perform(get("/api/stats").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.tauxReponse").value(100.0))
                .andExpect(jsonPath("$.parMois[0].mois").value("2026-10"));

        mvc.perform(post("/api/candidatures/" + id + "/ia").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void uploadDUnCvEtRattachement() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf",
                "contenu".getBytes());
        String body = mvc.perform(multipart("/api/documents").file(fichier).param("type", "CV")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String docId = extraire(body, "id");

        mvc.perform(get("/api/documents/" + docId + "/fichier").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("contenu"));

        mvc.perform(post("/api/candidatures").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomEntreprise":"Globex","poste":"Fullstack","dateCandidature":"2026-10-02",
                                 "cvId":%s}""".formatted(docId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cv.nomFichier").value("cv.pdf"));
    }

    @Test
    void aideIvoSansCleEtSansMessage() throws Exception {
        mvc.perform(post("/api/ia/aide").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"Comment ajouter un CV ?"}]}"""))
                .andExpect(status().isServiceUnavailable());

        mvc.perform(post("/api/ia/aide").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"messages\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void offresSansApiConfiguree() throws Exception {
        mvc.perform(get("/api/offres").param("q", "java").header("Authorization", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("LBA_API_KEY")));

        mvc.perform(get("/api/offres").param("contrat", "INTERIM").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyseCvEtLettreDeMotivation() throws Exception {
        MockMultipartFile cv = new MockMultipartFile("fichier", "cv.txt", "text/plain",
                "Franck Tague - Developpeur Java Spring Boot, React, Docker, GitHub Actions, PostgreSQL".getBytes());
        String cvId = extraire(mvc.perform(multipart("/api/documents").file(cv).param("type", "CV")
                        .header("Authorization", token))
                .andReturn().getResponse().getContentAsString(), "id");

        String sansOffre = extraire(mvc.perform(post("/api/candidatures").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomEntreprise":"Initech","poste":"Dev","dateCandidature":"2026-11-01","cvId":%s}"""
                                .formatted(cvId)))
                .andReturn().getResponse().getContentAsString(), "id");
        // Sans texte d'offre, rien a comparer
        mvc.perform(post("/api/candidatures/" + sansOffre + "/ia/compatibilite").header("Authorization", token))
                .andExpect(status().isBadRequest());

        String id = extraire(mvc.perform(post("/api/candidatures").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomEntreprise":"Acme/Labs","poste":"Dev Java","dateCandidature":"2026-11-01",
                                 "texteOffre":"Alternance Java Spring Boot","cvId":%s}""".formatted(cvId)))
                .andReturn().getResponse().getContentAsString(), "id");
        // CV lu, mais pas de cle d'IA dans les tests
        mvc.perform(post("/api/candidatures/" + id + "/ia/compatibilite").header("Authorization", token))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/candidatures/" + id + "/ia/lettre").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consignes\":\"ton dynamique\"}"))
                .andExpect(status().isServiceUnavailable());

        // La lettre (relue par le candidat) est enregistree en PDF et rattachee
        String body = mvc.perform(post("/api/candidatures/" + id + "/lettre").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texte\":\"Madame, Monsieur,\\n\\nJe souhaite rejoindre Acme.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lettre.nomFichier").value("Lettre de motivation - Acme Labs.pdf"))
                .andReturn().getResponse().getContentAsString();
        String lettreId = body.replaceAll("(?s).*\"lettre\":\\{\"id\":(\\d+).*", "$1");
        mvc.perform(get("/api/documents/" + lettreId + "/fichier").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void valideLesChampsObligatoires() throws Exception {
        mvc.perform(post("/api/candidatures").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.nomEntreprise").exists());
    }

    private static String extraire(String json, String champ) {
        Matcher m = Pattern.compile("\"" + champ + "\"\\s*:\\s*\"?([^\",}]+)").matcher(json);
        if (!m.find()) {
            throw new AssertionError(champ + " absent de " + json);
        }
        return m.group(1);
    }
}
