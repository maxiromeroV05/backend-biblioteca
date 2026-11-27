package com.biblioteca.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo (JSON) de los mensajes que viajan por RabbitMQ y Kafka.
 *
 * @param id            identificador del mensaje (mensajeId de la Notificacion)
 * @param tipo          tipo tipado del evento: NOTIFICACION/TICKET/DOCUMENTO en
 *                      RabbitMQ y PRESTAMO_CREADO/PRESTAMO_DEVUELTO en Kafka
 * @param destinatario  a quién va dirigido (email del usuario)
 * @param contenido     texto del mensaje
 * @param fechaEnvio    fecha ISO-8601 en que se publicó
 * @param correlationId identificador de correlación para trazar el evento
 * @param usuario       usuario autenticado que originó el evento (puede ser null)
 */
public record EventoMensajeria(
        @NotBlank(message = "El id del evento es obligatorio")
        @Size(max = 100, message = "El id no puede superar los 100 caracteres")
        String id,

        @NotNull(message = "El tipo de evento es obligatorio")
        EventoTipo tipo,

        @Size(max = 150, message = "El destinatario no puede superar los 150 caracteres")
        String destinatario,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 1000, message = "El contenido no puede superar los 1000 caracteres")
        String contenido,

        @NotBlank(message = "La fecha de envío es obligatoria")
        String fechaEnvio,

        @Size(max = 100, message = "El correlationId no puede superar los 100 caracteres")
        String correlationId,

        @Size(max = 150, message = "El usuario no puede superar los 150 caracteres")
        String usuario
) {

    /** Constructor canónico usado también por Jackson al deserializar el mensaje. */
    @JsonCreator
    public EventoMensajeria {
    }

    /** Constructor de compatibilidad para mensajes sin datos de auditoría. */
    public EventoMensajeria(String id, EventoTipo tipo, String destinatario,
                            String contenido, String fechaEnvio) {
        this(id, tipo, destinatario, contenido, fechaEnvio, null, null);
    }
}
