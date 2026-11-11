package com.jobtracker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.common.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securite;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.frontend-url=https://postilyn.test"
})
@AutoConfigureMockMvc
class SecuriteTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailService emailService;

    private void inscrire(String email, String motDePasse) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Franck\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, motDePasse)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions connecter(String email, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, motDePasse)));
    }

    @Test
    void motDePasseTropFaibleRefuseALInscription() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"A\",\"email\":\"faible@test.fr\",\"password\":\"motdepasse\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.password").exists());
    }

    @Test
    void compteBloqueApresCinqEchecs() throws Exception {
        inscrire("bloque@test.fr", "secret123");
        for (int i = 0; i < 5; i++) {
            connecter("bloque@test.fr", "mauvais1").andExpect(status().isUnauthorized());
        }
        // Meme le bon mot de passe est refuse pendant le blocage
        connecter("bloque@test.fr", "secret123")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Reessaie dans")));
    }

    @Test
    void motDePasseOublieEtReinitialisation() throws Exception {
        inscrire("oubli@test.fr", "secret123");
        String ancienJeton = "Bearer " + connecter("oubli@test.fr", "secret123").andReturn().getResponse()
                .getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // Email inconnu : meme reponse, aucun email envoye
        mvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@test.fr\"}"))
                .andExpect(status().isNoContent());
        verify(emailService, never()).envoyer(eq("inconnu@test.fr"), anyString(), anyString(), anyString());

        mvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Oubli@Test.fr\"}"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<String> texte = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyer(eq("oubli@test.fr"), anyString(), texte.capture(), anyString());
        assertTrue(texte.getValue().contains("https://postilyn.test/reinitialiser?jeton="), texte.getValue());
        String jeton = texte.getValue().replaceAll("(?s).*reinitialiser\\?jeton=([A-Za-z0-9_-]+).*", "$1");

        String corps = "{\"jeton\":\"%s\",\"nouveau\":\"nouveau123\"}".formatted(jeton);
        mvc.perform(post("/api/auth/reinitialiser").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isNoContent());
        // Lien a usage unique
        mvc.perform(post("/api/auth/reinitialiser").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());

        connecter("oubli@test.fr", "secret123").andExpect(status().isUnauthorized());
        connecter("oubli@test.fr", "nouveau123").andExpect(status().isOk());
        // Les sessions ouvertes avant la reinitialisation sont deconnectees
        mvc.perform(get("/api/candidatures").header("Authorization", ancienJeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void deconnexionDeTousLesAppareils() throws Exception {
        inscrire("partout@test.fr", "secret123");
        String jeton = "Bearer " + connecter("partout@test.fr", "secret123").andReturn().getResponse()
                .getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        String nouveau = "Bearer " + mvc.perform(post("/api/compte/deconnexion-partout").header("Authorization", jeton))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/candidatures").header("Authorization", jeton)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/candidatures").header("Authorization", nouveau)).andExpect(status().isOk());
    }
}
