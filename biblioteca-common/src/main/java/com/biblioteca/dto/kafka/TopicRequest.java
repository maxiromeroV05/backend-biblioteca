package com.biblioteca.dto.kafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Datos para crear un tópico (POST /api/kafka/topics).
 *
 * @param name              nombre del tópico
 * @param partitions        número de particiones (si no se indica, app.kafka.topic-defaults.partitions)
 * @param replicationFactor factor de replicación (si no se indica, app.kafka.topic-defaults.replicas)
 * @param configs           configuraciones del tópico, por ejemplo {"retention.ms": "604800000"}
 */
public record TopicRequest(
        @NotBlank(message = "El nombre del tópico es obligatorio")
        @Size(max = 249, message = "El nombre del tópico no puede superar los 249 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "El nombre solo puede tener letras, números, '.', '_' o '-'")
        String name,

        @Min(value = 1, message = "Debe haber al menos 1 partición")
        @Max(value = 1000, message = "El máximo permitido es 1000 particiones")
        Integer partitions,

        @Min(value = 1, message = "El factor de replicación debe ser al menos 1")
        @Max(value = 10, message = "El máximo permitido es 10 réplicas")
        Integer replicationFactor,

        Map<String, String> configs
) {
}
