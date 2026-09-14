package com.biblioteca.exception;

/**
 * Se lanza cuando se busca un registro por id (o por otra clave)
 * y este no existe en la base de datos.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
