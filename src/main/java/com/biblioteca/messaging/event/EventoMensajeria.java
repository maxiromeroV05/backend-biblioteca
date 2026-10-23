package com.biblioteca.messaging.event;

/**
 * Cuerpo (JSON) de los mensajes que viajan por RabbitMQ.
 *
 * @param id           identificador del mensaje (mensajeId de la Notificacion)
 * @param tipo         NOTIFICACION, TICKET o DOCUMENTO
 * @param destinatario a quién va dirigido (email del usuario)
 * @param contenido    texto del mensaje
 * @param fechaEnvio   fecha ISO-8601 en que se publicó
 */
public record EventoMensajeria(
        String id,
        String tipo,
        String destinatario,
        String contenido,
        String fechaEnvio
) {
}
