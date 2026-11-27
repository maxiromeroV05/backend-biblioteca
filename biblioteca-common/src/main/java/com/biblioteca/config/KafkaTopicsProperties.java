package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Propiedades de Kafka leídas desde application.yml (prefijo app.kafka).
 * Es la única fuente de nombres de tópicos, grupos y política de reintentos.
 *
 * @param enabled            false = no crea tópicos, no consume y no publica
 * @param dltSuffix          sufijo del tópico de mensajes muertos (el de "x" es "x" + sufijo)
 * @param sendTimeoutSeconds espera máxima de la confirmación del broker al publicar
 * @param topics             nombres de los dos tópicos de negocio
 * @param topicDefaults      particiones y réplicas con las que se crean los tópicos
 * @param topicConfigs       configuraciones explícitas de los tópicos de negocio
 * @param dltConfigs         configuraciones explícitas de los tópicos muertos
 * @param retry              política de reintentos antes de enviar al DLT
 * @param groups             un grupo de consumidores por responsabilidad
 * @param listener           concurrencia de los listeners
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(
        boolean enabled,
        String dltSuffix,
        int sendTimeoutSeconds,
        Topics topics,
        TopicDefaults topicDefaults,
        Map<String, String> topicConfigs,
        Map<String, String> dltConfigs,
        Retry retry,
        Groups groups,
        Listener listener
) {

    public record Topics(String notificaciones, String prestamos) {
    }

    /** Valores con los que se crea un tópico cuando la petición no los indica. */
    public record TopicDefaults(int partitions, int replicas) {
    }

    public record Retry(int maxAttempts, long backoffMs) {
    }

    /** Consumer groups separados por flujo para dividir responsabilidades y balancear carga. */
    public record Groups(String notificaciones, String prestamos, String dlt) {
    }

    public record Listener(int concurrency) {
    }

    /** Nombre del tópico de mensajes muertos (Dead Letter Topic) de un tópico. */
    public String dlt(String topic) {
        return topic + dltSuffix;
    }
}
