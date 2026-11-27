package com.biblioteca.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaTopicsConfigTest {

    private KafkaTopicsProperties props() {
        return new KafkaTopicsProperties(
                true, ".DLT", 2,
                new KafkaTopicsProperties.Topics("biblioteca.notificaciones", "biblioteca.prestamos"),
                new KafkaTopicsProperties.TopicDefaults(3, 3),
                Map.of("retention.ms", "604800000", "cleanup.policy", "delete"),
                Map.of("retention.ms", "86400000", "cleanup.policy", "compact,delete"),
                new KafkaTopicsProperties.Retry(3, 10),
                new KafkaTopicsProperties.Groups("g-not", "g-prest", "g-dlt"),
                new KafkaTopicsProperties.Listener(3));
    }

    @Test
    void losTopicosDeNegocioDeclaranParticionesReplicasRetencionYCleanup() {
        KafkaTopicsConfig config = new KafkaTopicsConfig(props());

        NewTopic notificaciones = config.notificacionesTopic();
        NewTopic prestamos = config.prestamosTopic();

        assertEquals(3, notificaciones.numPartitions());
        assertEquals(3, notificaciones.replicationFactor());
        assertEquals(Map.of("retention.ms", "604800000", "cleanup.policy", "delete"),
                notificaciones.configs());
        assertEquals(notificaciones.configs(), prestamos.configs());
    }

    @Test
    void losDltTienenSuPropiaRetencionYPolitica() {
        KafkaTopicsConfig config = new KafkaTopicsConfig(props());

        NewTopic dlt = config.notificacionesDlt();

        assertEquals("biblioteca.notificaciones.DLT", dlt.name());
        assertEquals(Map.of("retention.ms", "86400000", "cleanup.policy", "compact,delete"), dlt.configs());
    }
}
