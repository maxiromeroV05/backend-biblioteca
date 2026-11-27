package com.biblioteca.dto.rabbit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos para crear una cola (POST /api/rabbit/queues).
 * Los campos de solo lectura del frontend (messages, consumers) se ignoran.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColaRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = 255, message = "El nombre de la cola no puede superar los 255 caracteres")
    @Pattern(regexp = "^(?!amq\\.)[A-Za-z0-9._:-]+$",
            message = "El nombre solo puede tener letras, números, '.', '_', '-' o ':' y no puede empezar con 'amq.'")
    private String name;

    // Si no se indica, la cola es durable.
    private Boolean durable = true;

    @Size(max = 255, message = "El dead letter exchange no puede superar los 255 caracteres")
    private String deadLetterExchange;

    @Size(max = 255, message = "La dead letter routing key no puede superar los 255 caracteres")
    private String deadLetterRoutingKey;

    @Positive(message = "El TTL debe ser mayor que 0 (milisegundos)")
    @Max(value = 2147483647, message = "El TTL es demasiado grande")
    private Long messageTtl;
}
