package com.biblioteca.dto.kafka.metrics;

/**
 * @param name              nombre del tópico
 * @param partitions        particiones declaradas
 * @param replicationFactor réplicas por partición
 * @param totalMessages     suma de offsets finales (mensajes publicados)
 * @param eventsPerMinute   variación de mensajes entre dos sondeos
 * @param failures          mensajes acumulados en su DLT
 * @param failuresPerMinute variación de mensajes del DLT entre dos sondeos
 */
public record TopicMetrics(
        String name,
        int partitions,
        int replicationFactor,
        long totalMessages,
        double eventsPerMinute,
        long failures,
        double failuresPerMinute
) {
}
