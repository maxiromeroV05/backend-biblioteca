package com.biblioteca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la seguridad JWT (SecurityConfig): el filtro del resource server
 * debe dejar pasar solo las peticiones con un JWT válido.
 *
 *  - /api/public/**   -> abierto (200 sin token)
 *  - /api/**          -> 401 sin token o con un token inválido
 *  - /api/** con JWT  -> 200 (jwt() simula un token ya validado por el IDaaS)
 */
@SpringBootTest(properties = "app.kafka.enabled=false")
class SeguridadApiTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/libros"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMalFormadoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/libros").header("Authorization", "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiConJwtValidoResponde200() throws Exception {
        mockMvc.perform(get("/api/libros").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void administracionDeMensajeriaExigeJwt() throws Exception {
        mockMvc.perform(get("/api/kafka/topics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/rabbit/queues")).andExpect(status().isUnauthorized());
    }
}
