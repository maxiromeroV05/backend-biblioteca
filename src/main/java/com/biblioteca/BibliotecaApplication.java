package com.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del sistema de gestión de biblioteca.
 * Al ejecutar el método main() se levanta el servidor embebido
 * (Tomcat) y queda disponible la API REST en http://localhost:8080/api
 *
 * @EnableScheduling habilita la tarea programada que marca los
 * préstamos vencidos como ATRASADO (ver PrestamoServiceImpl).
 */
@SpringBootApplication
@EnableScheduling
public class BibliotecaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliotecaApplication.class, args);
    }

}
