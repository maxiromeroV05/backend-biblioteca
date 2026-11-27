package com.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio API de la biblioteca: libros, autores, categorías, usuarios,
 * préstamos y notificaciones, además de los productores/consumidores de
 * RabbitMQ y Kafka.
 *
 * Los tipos compartidos (entidades, DTOs, excepciones, seguridad) viven en
 * biblioteca-common; este módulo añade JPA, la lógica de negocio y la
 * mensajería. Escucha en el puerto 8080 por defecto.
 *
 * @EnableScheduling habilita la tarea que marca los préstamos vencidos como
 * ATRASADO (ver PrestamoServiceImpl).
 */
@SpringBootApplication
@EnableScheduling
public class BibliotecaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliotecaApiApplication.class, args);
    }
}
