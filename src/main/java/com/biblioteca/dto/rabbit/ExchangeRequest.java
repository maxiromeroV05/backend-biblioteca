package com.biblioteca.dto.rabbit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos para crear un exchange; también se usa como respuesta de los listados. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = 255, message = "El nombre del exchange no puede superar los 255 caracteres")
    @Pattern(regexp = "^(?!amq\\.)[A-Za-z0-9._:-]+$",
            message = "El nombre solo puede tener letras, números, '.', '_', '-' o ':' y no puede empezar con 'amq.'")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$",
            message = "El tipo debe ser direct, topic, fanout o headers")
    private String type;

    // Si no se indica, el exchange es durable.
    private Boolean durable = true;

    // Si no se indica, no se elimina automáticamente.
    private Boolean autoDelete = false;
}
