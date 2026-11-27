package com.biblioteca;

import com.biblioteca.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprueba que el microservicio administrador de RabbitMQ arranca de forma
 * aislada: solo sus beans y los de biblioteca-common, sin API ni base de datos.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class RabbitAdminApplicationTests {

    @Autowired
    private RabbitAdminService adminService;

    @Test
    void contextLoads() {
        assertNotNull(adminService);
    }
}
