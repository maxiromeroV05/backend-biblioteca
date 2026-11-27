package com.biblioteca;

import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprueba que el microservicio administrador de Kafka arranca de forma
 * aislada: solo sus beans y los de biblioteca-common, sin API ni base de datos.
 */
@SpringBootTest(properties = {
        "app.kafka.enabled=false",
        "spring.kafka.admin.auto-create=false"
})
class KafkaAdminApplicationTests {

    @Autowired
    private KafkaAdminService adminService;

    @Autowired
    private KafkaTemplate<String, EventoMensajeria> kafkaTemplate;

    @Test
    void contextLoads() {
        assertNotNull(adminService);
        assertNotNull(kafkaTemplate);
    }
}
