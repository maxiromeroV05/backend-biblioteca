package com.biblioteca;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.messaging.event.EventoMensajeria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Prueba mínima que verifica que el contexto de Spring
 * levanta correctamente (todos los beans se crean sin errores) y que las
 * propiedades de Kafka (grupos y configuración de tópicos) se enlazan desde
 * application.yml.
 *
 * app.kafka.enabled=false evita que el test intente conectarse a un clúster
 * Kafka (no crea tópicos ni arranca los listeners).
 */
@SpringBootTest(properties = "app.kafka.enabled=false")
class BibliotecaApplicationTests {

    @Autowired
    private KafkaTopicsProperties kafkaProps;

    @Autowired
    private KafkaTemplate<String, EventoMensajeria> kafkaTemplate;

    @Test
    void contextLoads() {
        // Si el contexto de Spring carga sin lanzar excepciones, la prueba pasa.
        assertNotNull(kafkaTemplate);
    }

    @Test
    void configuracionDeKafkaSeLigaDesdeApplicationYml() {
        assertEquals("604800000", kafkaProps.topicConfigs().get("retention.ms"));
        assertEquals("delete", kafkaProps.topicConfigs().get("cleanup.policy"));
        assertEquals("86400000", kafkaProps.dltConfigs().get("retention.ms"));
        assertEquals("biblioteca-notificaciones", kafkaProps.groups().notificaciones());
        assertEquals("biblioteca-prestamos", kafkaProps.groups().prestamos());
        assertEquals("biblioteca-dlt", kafkaProps.groups().dlt());
        assertEquals(3, kafkaProps.listener().concurrency());
    }

}
