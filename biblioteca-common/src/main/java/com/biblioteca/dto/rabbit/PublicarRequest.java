package com.biblioteca.dto.rabbit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mensaje de prueba que se publica desde el panel de mensajería (POST /api/rabbit/publish). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicarRequest {

    @NotBlank(message = "El exchange es obligatorio")
    @Size(max = 255, message = "El nombre del exchange no puede superar los 255 caracteres")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    @Size(max = 255, message = "La routing key no puede superar los 255 caracteres")
    private String routingKey;

    @NotBlank(message = "El mensaje (payload) es obligatorio")
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String payload;
}
