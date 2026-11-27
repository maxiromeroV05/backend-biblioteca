package com.biblioteca.dto;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Prestamo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de Prestamo. Solo se pide libroId y usuarioId para crear el
 * préstamo; el resto de los campos de solo lectura los calcula el
 * backend (fechaPrestamo, estado, etc).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoDTO {

    private Long id;

    @NotNull(message = "Debe indicarse el id del libro")
    private Long libroId;

    // Solo lectura.
    private String libroTitulo;

    @NotNull(message = "Debe indicarse el id del usuario")
    private Long usuarioId;

    // Solo lectura.
    private String usuarioNombre;

    // Solo lectura: se asigna al crear el préstamo (día de hoy).
    private LocalDate fechaPrestamo;

    // Fecha límite para devolver el libro. Si no se manda, el
    // servicio calcula una por defecto (ej: 14 días desde hoy).
    private LocalDate fechaDevolucionEsperada;

    // Solo lectura: se completa cuando se registra la devolución.
    private LocalDate fechaDevolucionReal;

    // Solo lectura: lo controla el backend (ACTIVO, DEVUELTO, ATRASADO).
    private EstadoPrestamo estado;

    @Size(max = 500)
    private String observaciones;

    public static PrestamoDTO desdeEntidad(Prestamo prestamo) {
        PrestamoDTO dto = new PrestamoDTO();
        dto.setId(prestamo.getId());
        dto.setFechaPrestamo(prestamo.getFechaPrestamo());
        dto.setFechaDevolucionEsperada(prestamo.getFechaDevolucionEsperada());
        dto.setFechaDevolucionReal(prestamo.getFechaDevolucionReal());
        dto.setEstado(prestamo.getEstado());
        dto.setObservaciones(prestamo.getObservaciones());
        if (prestamo.getLibro() != null) {
            dto.setLibroId(prestamo.getLibro().getId());
            dto.setLibroTitulo(prestamo.getLibro().getTitulo());
        }
        if (prestamo.getUsuario() != null) {
            dto.setUsuarioId(prestamo.getUsuario().getId());
            dto.setUsuarioNombre(prestamo.getUsuario().getNombre() + " " + prestamo.getUsuario().getApellido());
        }
        return dto;
    }
}
