package com.biblioteca;

import com.biblioteca.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio administrador de Kafka.
 *
 * Expone /api/kafka/** para crear, listar, modificar y eliminar tópicos y sus
 * configuraciones, inspeccionar consumer groups y publicar mensajes de prueba.
 * No contiene lógica de negocio de la biblioteca ni accede a la base de datos;
 * escucha en el puerto 8081 por defecto.
 */
@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaAdminApplication.class, args);
    }
}
