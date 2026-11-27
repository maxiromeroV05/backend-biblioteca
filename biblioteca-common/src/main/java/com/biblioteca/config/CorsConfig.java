package com.biblioteca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend Angular (que corre en otro
 * puerto, típicamente http://localhost:4200) pueda consumir esta API
 * sin que el navegador bloquee las peticiones.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Orígenes permitidos: app.security.cors-allowed-origins (application.yml).
    @Value("${app.security.cors-allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
