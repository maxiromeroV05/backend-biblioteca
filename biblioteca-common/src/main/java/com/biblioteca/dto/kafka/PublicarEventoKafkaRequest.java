package com.biblioteca.dto.kafka;

import com.biblioteca.messaging.event.EventoMensajeria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Publicación de prueba con el DTO exacto que viaja por Kafka.
 *
 * @param topic  tópico destino (el DTO no lleva el tópico, va en el metadata)
 * @param evento cuerpo completo: id, tipo, destinatario, contenido, fechaEnvio,
 *               correlationId y usuario
 */
public record PublicarEventoKafkaRequest(
        @NotBlank(message = "El tópico es obligatorio")
        @Size(max = 249, message = "El nombre del tópico no puede superar los 249 caracteres")
        String topic,

        @Valid
        @NotNull(message = "El evento es obligatorio")
        EventoMensajeria evento
) {
}
