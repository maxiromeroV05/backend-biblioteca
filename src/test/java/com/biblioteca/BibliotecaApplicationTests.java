package com.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba mínima que verifica que el contexto de Spring
 * levanta correctamente (todos los beans se crean sin errores).
 */
@SpringBootTest
class BibliotecaApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto de Spring carga sin lanzar excepciones, la prueba pasa.
    }

}
