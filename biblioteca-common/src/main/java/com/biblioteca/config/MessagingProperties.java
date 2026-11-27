package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de mensajería leídas desde application.yml (prefijo app.rabbit).
 * Es la única fuente de nombres de colas, exchanges y routing keys.
 */
@ConfigurationProperties(prefix = "app.rabbit")
public record MessagingProperties(
        Exchanges exchanges,
        Queues queues,
        Routing routing,
        Retry retry,
        Management management
) {

    public record Exchanges(String direct, String topic, String dlx) {
    }

    /** Una cola de negocio con su binding y su cola de mensajes muertos (DLQ). */
    public record QueueDef(String name, String routingKey, String dlq, String dlqRoutingKey) {
    }

    public record Queues(QueueDef notificaciones, QueueDef tickets, QueueDef documentos) {
    }

    public record Routing(String notificacionPrestamoCreado, String notificacionPrestamoDevuelto) {
    }

    public record Retry(int maxAttempts) {
    }

    public record Management(String url) {
    }
}
