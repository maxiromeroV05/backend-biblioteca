package com.biblioteca.dto;

import com.biblioteca.entity.Notificacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notificación tal como la espera el frontend (modelo Notificacion de Angular):
 * id, tipo, destinatario, mensaje, estado, cola y fecha.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {

    private Long id;
    private String tipo;
    private String destinatario;
    private String mensaje;
    private String estado;
    private String cola;
    private String fecha;
    private String error;

    public static NotificacionResponse desdeEntidad(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getTipo() != null ? n.getTipo().name() : null,
                n.getDestinatario(),
                n.getContenido(),
                n.getEstado() != null ? n.getEstado().name() : null,
                n.getCola(),
                n.getFechaCreacion() != null ? n.getFechaCreacion().toString() : null,
                n.getError()
        );
    }
}
