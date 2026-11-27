package com.biblioteca.dto.kafka.metrics;

/**
 * @param eventsPerMinute suma de eventos/minuto de los tópicos de negocio
 * @param totalFailures   suma de mensajes en los DLT
 * @param totalLag        suma del lag de todos los consumer groups
 */
public record MetricsSummary(
        double eventsPerMinute,
        long totalFailures,
        long totalLag
) {
}
