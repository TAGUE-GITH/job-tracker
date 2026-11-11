package com.jobtracker;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.common.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:support;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.admin.emails=patron@postulyn.test"
})
@AutoConfigureMockMvc
class SupportTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailService emailService;

    private String inscrire(String email) throws Exception {
        String corps = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"email\":\"%s\",\"password\":\"secret123\",\"accepteConditions\":true}"
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + corps.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    /** Jeton de l'administrateur principal (le compte est cree au premier appel). */
    private String patron() throws Exception {
        String reponse = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"patron@postulyn.test\",\"password\":\"secret123\"}"))
                .andReturn().getResponse().getContentAsString();
        return reponse.contains("token")
                ? "Bearer " + reponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1")
                : inscrire("patron@postulyn.test");
    }

    private static String message(String email, String sujet) {
        return """
                {"nom":"Camille","email":"%s","categorie":"PROBLEME","sujet":"%s",
                 "message":"Je n'arrive pas a importer mon CV en PDF."}""".formatted(email, sujet);
    }

    @Test
    void lesConditionsDoiventEtreAcceptees() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"email\":\"refus@test.fr\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.accepteConditions").exists());
    }

    @Test
    void unVisiteurContacteLeSupportEtLAdministrateurTraiteLeMessage() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(message("visiteur@test.fr", "Import de CV")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").doesNotExist());
        // L'equipe est prevenue et le visiteur recoit un accuse de reception
        verify(emailService).envoyer(eq("patron@postulyn.test"), anyString(), anyString(), anyString());
        verify(emailService).envoyer(eq("visiteur@test.fr"), anyString(), anyString(), anyString());

        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"email\":\"pas-un-email\",\"categorie\":\"PROBLEME\",\"sujet\":\"x\",\"message\":\"court\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.email").exists())
                .andExpect(jsonPath("$.erreurs.message").exists());

        String membre = inscrire("membre-support@test.fr");
        mvc.perform(get("/api/admin/messages").header("Authorization", membre)).andExpect(status().isForbidden());
        // Un utilisateur connecte : le message est rattache a son compte
        mvc.perform(post("/api/contact").header("Authorization", membre).contentType(MediaType.APPLICATION_JSON)
                        .content(message("membre-support@test.fr", "Question")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber());

        String patron = patron();
        String liste = mvc.perform(get("/api/admin/messages").header("Authorization", patron))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/admin/statistiques").header("Authorization", patron))
                .andExpect(jsonPath("$.messagesNonTraites").value(2));
        String id = liste.replaceAll("(?s)^\\[\\{\"id\":(\\d+).*", "$1");
        mvc.perform(put("/api/admin/messages/" + id).header("Authorization", patron)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"traite\":true}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/statistiques").header("Authorization", patron))
                .andExpect(jsonPath("$.messagesNonTraites").value(1));
        mvc.perform(delete("/api/admin/messages/" + id).header("Authorization", patron))
                .andExpect(status().isNoContent());
    }

    @Test
    void lesMessagesRepetesSontLimites() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                    .content(message("insistant@test.fr", "Message " + i))).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content(message("insistant@test.fr", "Encore")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void lesAvisRemontentDansLAdministration() throws Exception {
        mvc.perform(post("/api/avis").contentType(MediaType.APPLICATION_JSON).content("{\"note\":5}"))
                .andExpect(status().isUnauthorized());
        String client = inscrire("client-avis@test.fr");
        mvc.perform(post("/api/avis").header("Authorization", client).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":6}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/avis").header("Authorization", client).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":4,\"commentaire\":\"Tres pratique, il manque un export Excel.\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/avis").header("Authorization", client).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":2}"))
                .andExpect(status().isNoContent());

        String patron = patron();
        mvc.perform(get("/api/admin/avis").header("Authorization", patron))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(2))
                .andExpect(jsonPath("$.moyenne").value(3.0))
                .andExpect(jsonPath("$.parNote.4").value(1))
                .andExpect(jsonPath("$.avis[?(@.note == 4)].email").value("client-avis@test.fr"));
    }
}
