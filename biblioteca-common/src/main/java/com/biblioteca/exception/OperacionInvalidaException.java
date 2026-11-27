package com.biblioteca.exception;

/**
 * Se lanza cuando se intenta realizar una operación que viola
 * una regla de negocio, por ejemplo:
 * - Eliminar una categoría/autor que todavía tiene libros asociados.
 * - Eliminar un usuario con préstamos activos.
 * - Crear un préstamo de un libro sin ejemplares disponibles.
 */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
