package com.jobtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// URL copiee depuis le navigateur, avec un chemin : elle doit quand meme autoriser le site
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cors;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.cors.origins= https://taguora.vercel.app/login , mon-front.onrender.com"
})
@AutoConfigureMockMvc
class CorsTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void normaliseLesOrigines() {
        assertEquals("https://taguora.vercel.app", SecurityConfig.origine(" https://taguora.vercel.app/login "));
        assertEquals("https://taguora.vercel.app", SecurityConfig.origine("https://taguora.vercel.app/"));
        assertEquals("https://api.onrender.com", SecurityConfig.origine("api.onrender.com"));
        assertEquals("http://localhost:5173", SecurityConfig.origine("http://localhost:5173"));
    }

    @Test
    void autoriseLeFrontendConfigure() throws Exception {
        for (String origine : new String[] {"https://taguora.vercel.app", "https://mon-front.onrender.com"}) {
            mvc.perform(options("/api/offres")
                            .header("Origin", origine)
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origine));
        }
    }

    @Test
    void refuseUneAutreOrigine() throws Exception {
        mvc.perform(options("/api/offres")
                        .header("Origin", "https://pirate.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
