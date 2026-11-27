package com.biblioteca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SOLO PARA PRUEBAS LOCALES. Se activa únicamente con el perfil "dev":
 *
 *     .\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
 *
 * Deja pasar todas las peticiones sin JWT, para poder probar la API y la
 * mensajería sin una cuenta de Entra ID. Sin ese perfil (el valor por defecto)
 * esta clase NO se carga y rige SecurityConfig, que exige JWT en todo /api/**.
 */
@Configuration
@Profile("dev")
@EnableWebSecurity
public class DevSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSecurityConfig.class);

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("*** PERFIL DEV ACTIVO: la API NO exige autenticación. Usar solo en local. ***");

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())   // usa CorsConfig (orígenes de app.security.cors-allowed-origins)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .build();
    }
}
