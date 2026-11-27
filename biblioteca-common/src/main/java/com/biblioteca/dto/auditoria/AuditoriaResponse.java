package com.biblioteca.dto.auditoria;

/**
 * Fila del historial de eventos publicados/consumidos.
 *
 * @param id                 id interno de la notificación
 * @param mensajeId          clave del mensaje en Kafka/RabbitMQ
 * @param eventoTipo         PRESTAMO_CREADO, PRESTAMO_DEVUELTO, NOTIFICACION, ...
 * @param estado             PENDIENTE, PROCESADO o FALLIDO
 * @param usuario            usuario que originó el evento
 * @param correlationId      correlación de extremo a extremo
 * @param destinatario       email del destinatario
 * @param contenido          texto del mensaje
 * @param canal              tópico de Kafka o routing key de RabbitMQ
 * @param fechaCreacion      ISO-8601 local
 * @param fechaProcesamiento ISO-8601 local (puede ser null)
 * @param error              motivo del fallo (puede ser null)
 */
public record AuditoriaResponse(
        Long id,
        String mensajeId,
        String eventoTipo,
        String estado,
        String usuario,
        String correlationId,
        String destinatario,
        String contenido,
        String canal,
        String fechaCreacion,
        String fechaProcesamiento,
        String error
) {
}
