package com.biblioteca.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

/**
 * Declara los tópicos de Kafka de la aplicación y sus tópicos de mensajes
 * muertos (DLT).
 *
 * Esta clase SOLO configura; no contiene lógica de negocio. Los nombres vienen
 * de application.yml (ver KafkaTopicsProperties). El KafkaAdmin que crea Spring
 * Boot crea estos tópicos en el clúster al arrancar (si todavía no existen).
 *
 *   biblioteca.notificaciones  --(falla)--> biblioteca.notificaciones.DLT
 *   biblioteca.prestamos       --(falla)--> biblioteca.prestamos.DLT
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaTopicsConfig {

    private final KafkaTopicsProperties props;

    public KafkaTopicsConfig(KafkaTopicsProperties props) {
        this.props = props;
    }

    @Bean
    public NewTopic notificacionesTopic() {
        return crear(props.topics().notificaciones(), props.topicConfigs());
    }

    @Bean
    public NewTopic notificacionesDlt() {
        return crear(props.dlt(props.topics().notificaciones()), props.dltConfigs());
    }

    @Bean
    public NewTopic prestamosTopic() {
        return crear(props.topics().prestamos(), props.topicConfigs());
    }

    @Bean
    public NewTopic prestamosDlt() {
        return crear(props.dlt(props.topics().prestamos()), props.dltConfigs());
    }

    private NewTopic crear(String nombre, Map<String, String> configs) {
        return TopicBuilder.name(nombre)
                .partitions(props.topicDefaults().partitions())
                .replicas(props.topicDefaults().replicas())
                .configs(configs)
                .build();
    }
}
