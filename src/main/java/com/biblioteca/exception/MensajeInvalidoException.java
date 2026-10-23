package com.biblioteca.exception;

/**
 * Error NO recuperable al procesar un mensaje de RabbitMQ (contenido vacío,
 * campos obligatorios ausentes, etc.). Reintentar no sirve de nada, por lo
 * que el consumidor lo rechaza (NACK sin requeue) y va directo a la DLQ.
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
