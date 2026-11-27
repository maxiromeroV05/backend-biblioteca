package com.biblioteca.dto.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para publicar un mensaje de prueba (POST /api/kafka/publish).
 */
public record PublicarKafkaRequest(
        @NotBlank(message = "El tópico es obligatorio")
        // 100 = largo de la columna "cola" de la tabla notificaciones, donde se guarda el tópico.
        @Size(max = 100, message = "El nombre del tópico no puede superar los 100 caracteres")
        String topic,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 1000, message = "El contenido no puede superar los 1000 caracteres")
        String contenido
) {
}
