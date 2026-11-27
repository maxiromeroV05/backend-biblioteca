package com.biblioteca;

import com.biblioteca.config.MessagingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio administrador de RabbitMQ.
 *
 * Expone /api/rabbit/** para crear y eliminar colas, exchanges y bindings,
 * listarlos desde la API HTTP de management y publicar mensajes de prueba.
 * No declara la topología de negocio ni accede a la base de datos; escucha en
 * el puerto 8082 por defecto.
 */
@SpringBootApplication
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
