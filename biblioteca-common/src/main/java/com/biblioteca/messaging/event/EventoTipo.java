package com.biblioteca.messaging.event;

/**
 * Tipos de evento que viajan en el cuerpo de los mensajes ({@link EventoMensajeria}).
 *
 * <ul>
 *   <li>{@code NOTIFICACION}, {@code TICKET}, {@code DOCUMENTO}: usados por RabbitMQ.</li>
 *   <li>{@code PRESTAMO_CREADO}, {@code PRESTAMO_DEVUELTO}: usados por el tópico
 *       {@code biblioteca.prestamos} de Kafka, para distinguir cada flujo de negocio.</li>
 * </ul>
 *
 * Al ser un enum, Jackson lo serializa como su nombre y el consumidor recibe un
 * DTO tipado (no un String suelto).
 */
public enum EventoTipo {
    NOTIFICACION,
    TICKET,
    DOCUMENTO,
    PRESTAMO_CREADO,
    PRESTAMO_DEVUELTO
}
