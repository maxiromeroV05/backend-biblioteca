package com.biblioteca.dto.kafka;

import java.util.List;

/**
 * Estado de un consumer group de Kafka.
 *
 * @param groupId    identificador del grupo
 * @param state      estado del grupo (Stable, Empty, Dead, ...)
 * @param topics     tópicos con particiones asignadas o offsets confirmados
 * @param members    miembros del grupo y sus particiones
 */
public record ConsumerGroupResponse(
        String groupId,
        String state,
        List<String> topics,
        List<ConsumerGroupMemberResponse> members
) {
}
