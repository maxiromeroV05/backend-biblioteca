package com.biblioteca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cuerpo JSON estándar que se devuelve cuando ocurre un error.
 * Ejemplo:
 * {
 *   "mensaje": "No se encontró el libro con id 5",
 *   "estado": 404,
 *   "fecha": "2026-09-13T10:15:30"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String mensaje;
    private int estado;
    private LocalDateTime fecha;
}
