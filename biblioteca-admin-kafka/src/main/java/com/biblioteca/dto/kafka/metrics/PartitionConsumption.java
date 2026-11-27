package com.biblioteca.dto.kafka.metrics;

/**
 * Distribución del consumo: una fila por grupo, tópico y partición.
 */
public record PartitionConsumption(
        String groupId,
        String topic,
        int partition,
        long committedOffset,
        long endOffset,
        long lag
) {
}
