package com.biblioteca.dto.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición para publicar una notificación de negocio en el tópico
 * biblioteca.notificaciones de Kafka (POST /api/notificaciones).
 *
 * @param destinatario email del usuario (opcional)
 * @param contenido    texto de la notificación
 */
public record NotificacionKafkaRequest(
        @Size(max = 150, message = "El destinatario no puede superar los 150 caracteres")
        String destinatario,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 1000, message = "El contenido no puede superar los 1000 caracteres")
        String contenido
) {
}
