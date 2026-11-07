package com.jobtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.demo.DemoService;
import com.jobtracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
class DemoTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DemoService demoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void unCompteDeDemoEstRempliPuisSupprimeAutomatiquement() throws Exception {
        String body = mvc.perform(post("/api/auth/demo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.nom").value("Alex Martin"))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/candidatures").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(8));
        mvc.perform(get("/api/stats").header("Authorization", token))
                .andExpect(jsonPath("$.parStatut.OFFRE_RECUE").value(1))
                .andExpect(jsonPath("$.parStatut.ABANDONNEE").value(1));
        mvc.perform(get("/api/documents").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(2));

        // Un compte cree il y a plus de 6 heures est supprime avec toutes ses donnees
        long avant = userRepository.count();
        jdbc.update("update users set date_creation = dateadd('HOUR', -7, current_timestamp) where email like '%@demo.taguora'");
        demoService.nettoyer();
        assertEquals(avant - 1, userRepository.count());
        assertEquals(0, jdbc.queryForObject("select count(*) from candidature", Integer.class));
    }
}
