package com.biblioteca.messaging.producer;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.messaging.event.AuditoriaContext;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Publicación de prueba del microservicio administrador de RabbitMQ.
 *
 * Independiente del productor de negocio (biblioteca-api): construye el DTO
 * tipado y lo envía al exchange indicado, sin registrar la Notificacion en la
 * base de datos. El converter JSON compartido vive en biblioteca-common.
 */
@Component
public class RabbitAdminPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public RabbitAdminPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public Notificacion publicar(String exchange, String routingKey, String contenido) {
        NotificacionTipo tipo = tipo(routingKey);
        Notificacion notificacion = new Notificacion(contenido, tipo, routingKey);
        EventoMensajeria evento = new EventoMensajeria(
                notificacion.getMensajeId(), EventoTipo.valueOf(tipo.name()), null, contenido,
                OffsetDateTime.now().toString(),
                AuditoriaContext.correlationId(), AuditoriaContext.usuarioActual());

        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        log.info("Mensaje de prueba publicado exchange={} routingKey={} id={}",
                exchange, routingKey, evento.id());
        return notificacion;
    }

    /** Contenido vacío: el consumidor lo rechaza y termina en la DLQ. */
    public Notificacion publicarInvalido() {
        return publicar(props.exchanges().topic(), props.routing().notificacionPrestamoCreado(), "");
    }

    private NotificacionTipo tipo(String routingKey) {
        if (routingKey.startsWith("ticket")) {
            return NotificacionTipo.TICKET;
        }
        if (routingKey.startsWith("documento")) {
            return NotificacionTipo.DOCUMENTO;
        }
        return NotificacionTipo.NOTIFICACION;
    }
}
