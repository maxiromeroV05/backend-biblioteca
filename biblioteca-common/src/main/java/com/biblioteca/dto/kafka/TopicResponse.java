package com.biblioteca.dto.kafka;

import java.util.Map;

/**
 * Descripción de un tópico.
 *
 * @param name              nombre del tópico
 * @param partitions        número de particiones
 * @param replicationFactor réplicas por partición
 * @param configs           configuraciones propias del tópico (overrides del valor por
 *                          defecto del broker). En el listado viene vacío; el detalle
 *                          (GET /api/kafka/topics/{name}) las incluye.
 */
public record TopicResponse(
        String name,
        int partitions,
        int replicationFactor,
        Map<String, String> configs
) {
}
