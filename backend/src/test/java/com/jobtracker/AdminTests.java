package com.jobtracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.admin.emails=Chef@Postilyn.test, autre@postilyn.test"
})
@AutoConfigureMockMvc
class AdminTests {

    @Autowired
    private MockMvc mvc;

    /** Inscrit le compte et renvoie l'en-tete Authorization ; le corps contient l'indicateur admin. */
    private String inscrire(String email, boolean adminAttendu) throws Exception {
        String corps = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admin").value(adminAttendu))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + corps.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private long id(String jeton) throws Exception {
        String profil = mvc.perform(get("/api/admin/utilisateurs").header("Authorization", jeton))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(profil.replaceAll("(?s).*\"id\":(\\d+),\"nom\":\"Test\",\"email\":\"membre@test.fr\".*", "$1"));
    }

    @Test
    void seulUnAdministrateurAccedeALAdministration() throws Exception {
        String membre = inscrire("membre@test.fr", false);
        String chef = inscrire("chef@postilyn.test", true);

        mvc.perform(get("/api/admin/statistiques")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/statistiques").header("Authorization", membre)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", membre)).andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/statistiques").header("Authorization", chef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateurs").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.inscrits7Jours").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.activite.length()").value(8))
                .andExpect(jsonPath("$.candidaturesParStatut.EN_ATTENTE").value(0));
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", chef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'chef@postilyn.test')].admin").value(true))
                .andExpect(jsonPath("$[?(@.email == 'membre@test.fr')].admin").value(false));

        // Les comptes de demonstration ne gonflent pas les statistiques
        String avant = mvc.perform(get("/api/admin/statistiques").header("Authorization", chef))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/auth/demo")).andExpect(status().isCreated());
        String apres = mvc.perform(get("/api/admin/statistiques").header("Authorization", chef))
                .andReturn().getResponse().getContentAsString();
        String chiffres = "(?s).*?\"utilisateurs\":(\\d+),\"comptesDemo\":(\\d+).*?\"candidatures\":(\\d+),.*";
        org.junit.jupiter.api.Assertions.assertEquals(avant.replaceAll(chiffres, "$1/$3"), apres.replaceAll(chiffres, "$1/$3"));
        // ... mais le compte de demonstration est bien compte a part
        org.junit.jupiter.api.Assertions.assertEquals(
                Integer.parseInt(avant.replaceAll(chiffres, "$2")) + 1, Integer.parseInt(apres.replaceAll(chiffres, "$2")));

        long idMembre = id(chef);
        // Deconnexion forcee : l'ancien jeton du membre ne marche plus
        mvc.perform(post("/api/admin/utilisateurs/" + idMembre + "/deconnexion").header("Authorization", chef))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/candidatures").header("Authorization", membre)).andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/admin/utilisateurs/" + idMembre).header("Authorization", chef))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", chef))
                .andExpect(jsonPath("$[?(@.email == 'membre@test.fr')]").isEmpty());
    }

    @Test
    void unAdministrateurNePeutPasEtreSupprime() throws Exception {
        String chef = inscrire("autre@postilyn.test", true);
        String corps = mvc.perform(get("/api/admin/utilisateurs").header("Authorization", chef))
                .andReturn().getResponse().getContentAsString();
        String idChef = corps.replaceAll("(?s).*\"id\":(\\d+),\"nom\":\"Test\",\"email\":\"autre@postilyn.test\".*", "$1");
        mvc.perform(delete("/api/admin/utilisateurs/" + idChef).header("Authorization", chef))
                .andExpect(status().isBadRequest());
    }
}
