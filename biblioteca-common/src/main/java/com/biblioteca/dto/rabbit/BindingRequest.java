package com.biblioteca.dto.rabbit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Binding exchange -> cola con su routing key. Se usa para crear (cuerpo JSON),
 * eliminar (parámetros de la URL) y como elemento de los listados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingRequest {

    @NotBlank(message = "El exchange es obligatorio")
    @Size(max = 255, message = "El nombre del exchange no puede superar los 255 caracteres")
    private String exchange;

    @NotBlank(message = "La cola es obligatoria")
    @Size(max = 255, message = "El nombre de la cola no puede superar los 255 caracteres")
    private String queue;

    // Puede ir vacía (por ejemplo en exchanges fanout), pero debe venir.
    @NotNull(message = "La routing key es obligatoria (puede ser vacía)")
    @Size(max = 255, message = "La routing key no puede superar los 255 caracteres")
    private String routingKey;
}
