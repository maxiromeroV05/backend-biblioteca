package com.biblioteca.exception;

/**
 * Se lanza cuando se intenta crear o actualizar un registro
 * usando un valor que debe ser único y que ya existe
 * (por ejemplo un ISBN, un email o el nombre de una categoría).
 */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
