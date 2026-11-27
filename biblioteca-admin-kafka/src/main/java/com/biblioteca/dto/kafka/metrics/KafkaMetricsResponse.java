package com.biblioteca.dto.kafka.metrics;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Instantánea de métricas del clúster. El front debe hacer polling cada
 * {@code pollingIntervalSeconds} segundos.
 *
 * @param generatedAt           fecha UTC de la instantánea
 * @param pollingIntervalSeconds intervalo recomendado de polling
 * @param topics                métricas por tópico de negocio
 * @param consumerGroups        offsets confirmados y lag por grupo
 * @param distribution          consumo por grupo/tópico/partición
 * @param summary               totales
 */
public record KafkaMetricsResponse(
        OffsetDateTime generatedAt,
        int pollingIntervalSeconds,
        List<TopicMetrics> topics,
        List<ConsumerGroupMetrics> consumerGroups,
        List<PartitionConsumption> distribution,
        MetricsSummary summary
) {
}
