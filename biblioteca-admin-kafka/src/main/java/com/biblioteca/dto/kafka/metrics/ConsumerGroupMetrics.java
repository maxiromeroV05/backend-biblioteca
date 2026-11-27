package com.biblioteca.dto.kafka.metrics;

import java.util.Map;

/**
 * @param groupId          identificador del grupo
 * @param state            estado del grupo (Stable, Empty, ...)
 * @param members          número de miembros activos
 * @param committedOffsets offset confirmado por "topico-particion"
 * @param lagByTopic       lag acumulado por tópico
 * @param totalLag         lag total del grupo
 */
public record ConsumerGroupMetrics(
        String groupId,
        String state,
        int members,
        Map<String, Long> committedOffsets,
        Map<String, Long> lagByTopic,
        long totalLag
) {
}
