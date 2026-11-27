package com.biblioteca.dto.kafka;

import java.util.List;

/**
 * Miembro de un consumer group, con las particiones que tiene asignadas.
 *
 * @param memberId           identificador del miembro
 * @param clientId           identificador de cliente del consumidor
 * @param host               host desde el que se conecta
 * @param assignedPartitions particiones asignadas ("topico-0", "topico-1", ...)
 */
public record ConsumerGroupMemberResponse(
        String memberId,
        String clientId,
        String host,
        List<String> assignedPartitions
) {
}
