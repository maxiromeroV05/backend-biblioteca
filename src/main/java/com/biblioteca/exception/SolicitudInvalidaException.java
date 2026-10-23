package com.biblioteca.exception;

/**
 * Se lanza cuando la petición es sintácticamente válida pero sus datos
 * no tienen sentido (por ejemplo, una routing key de dead letter sin
 * indicar el dead letter exchange). Se responde con HTTP 400.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
