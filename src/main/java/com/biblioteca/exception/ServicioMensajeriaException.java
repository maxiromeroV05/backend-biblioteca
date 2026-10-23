package com.biblioteca.exception;

/**
 * Se lanza cuando no se puede contactar la API de administración de
 * RabbitMQ (plugin management). Se responde con HTTP 503.
 */
public class ServicioMensajeriaException extends RuntimeException {

    public ServicioMensajeriaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
