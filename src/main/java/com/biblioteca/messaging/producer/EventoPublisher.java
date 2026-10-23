package com.biblioteca.messaging.producer;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Productor de mensajes: registra la Notificacion como PENDIENTE y publica
 * el evento en RabbitMQ. No conoce nombres de colas ni exchanges: los toma
 * de MessagingProperties (application.yml).
 */
@Component
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final NotificacionService notificacionService;
    private final MessagingProperties props;

    public EventoPublisher(RabbitTemplate rabbitTemplate,
                           NotificacionService notificacionService,
                           MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.notificacionService = notificacionService;
        this.props = props;
    }

    /** Notificación por el exchange topic (el patrón del binding decide la cola). */
    public Notificacion publicarNotificacion(String routingKey, String destinatario, String contenido) {
        return publicar(props.exchanges().topic(), routingKey, props.queues().notificaciones().name(),
                NotificacionTipo.NOTIFICACION, destinatario, contenido);
    }

    /** Ticket por el exchange direct. */
    public Notificacion publicarTicket(String destinatario, String contenido) {
        return publicar(props.exchanges().direct(), props.queues().tickets().routingKey(),
                props.queues().tickets().name(), NotificacionTipo.TICKET, destinatario, contenido);
    }

    /** Documento por el exchange direct. */
    public Notificacion publicarDocumento(String destinatario, String contenido) {
        return publicar(props.exchanges().direct(), props.queues().documentos().routingKey(),
                props.queues().documentos().name(), NotificacionTipo.DOCUMENTO, destinatario, contenido);
    }

    /** Mensaje de prueba con contenido vacío: el consumidor lo rechaza y termina en la DLQ. */
    public Notificacion publicarMensajeInvalido() {
        return publicarNotificacion(props.routing().notificacionPrestamoCreado(), null, "");
    }

    /**
     * Publicación genérica (la usa el endpoint /api/rabbit/publish). El tipo se
     * deduce de la routing key: ticket.* -> TICKET, documento.* -> DOCUMENTO.
     */
    public Notificacion publicarLibre(String exchange, String routingKey, String contenido) {
        NotificacionTipo tipo = NotificacionTipo.NOTIFICACION;
        if (routingKey.startsWith("ticket")) {
            tipo = NotificacionTipo.TICKET;
        } else if (routingKey.startsWith("documento")) {
            tipo = NotificacionTipo.DOCUMENTO;
        }
        return publicar(exchange, routingKey, routingKey, tipo, null, contenido);
    }

    /**
     * Núcleo: guarda la Notificacion PENDIENTE y envía el mensaje. Si RabbitMQ
     * falla, la deja como FALLIDO y propaga la excepción.
     */
    public Notificacion publicar(String exchange, String routingKey, String cola,
                                 NotificacionTipo tipo, String destinatario, String contenido) {
        Notificacion notificacion = notificacionService.crearPendiente(contenido, tipo, cola, destinatario);

        EventoMensajeria evento = new EventoMensajeria(
                notificacion.getMensajeId(),
                tipo.name(),
                destinatario,
                contenido,
                OffsetDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        } catch (AmqpException e) {
            notificacionService.marcarFallido(notificacion.getMensajeId(),
                    "No se pudo publicar en RabbitMQ: " + e.getMessage());
            throw e;
        }

        log.info("Mensaje publicado exchange={} routingKey={} id={} tipo={}",
                exchange, routingKey, evento.id(), tipo);
        return notificacion;
    }
}
