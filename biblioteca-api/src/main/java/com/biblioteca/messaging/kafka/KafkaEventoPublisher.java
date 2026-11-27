package com.biblioteca.messaging.kafka;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.exception.ServicioMensajeriaException;
import com.biblioteca.messaging.event.AuditoriaContext;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Productor de Kafka: registra la Notificacion como PENDIENTE y publica el DTO
 * EventoMensajeria (JacksonJsonSerializer) en un tópico. No conoce nombres de
 * tópicos: los toma de KafkaTopicsProperties (application.yml).
 *
 * El mensaje usa como key el id de la Notificacion, así todos los mensajes
 * con el mismo id caen en la misma partición.
 */
@Component
public class KafkaEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventoPublisher.class);

    private final KafkaTemplate<String, EventoMensajeria> kafkaTemplate;
    private final NotificacionService notificacionService;
    private final KafkaTopicsProperties props;

    public KafkaEventoPublisher(KafkaTemplate<String, EventoMensajeria> kafkaTemplate,
                                NotificacionService notificacionService,
                                KafkaTopicsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificacionService = notificacionService;
        this.props = props;
    }

    /** Evento de préstamo en el tópico de préstamos, con su tipo de negocio explícito. */
    public Notificacion publicarPrestamo(EventoTipo tipo, String destinatario, String contenido) {
        if (tipo != EventoTipo.PRESTAMO_CREADO && tipo != EventoTipo.PRESTAMO_DEVUELTO) {
            throw new IllegalArgumentException("El tipo de un evento de préstamo debe ser PRESTAMO_CREADO o PRESTAMO_DEVUELTO");
        }
        return publicar(props.topics().prestamos(), tipo, destinatario, contenido);
    }

    /** Notificación en el tópico de notificaciones (flujo de negocio + admin). */
    public Notificacion publicarNotificacion(String destinatario, String contenido) {
        return publicar(props.topics().notificaciones(), EventoTipo.NOTIFICACION, destinatario, contenido);
    }

    /** Publicación genérica (la usa el endpoint /api/kafka/publish). */
    public Notificacion publicarLibre(String topic, String contenido) {
        return publicar(topic, EventoTipo.NOTIFICACION, null, contenido);
    }

    /** Mensaje de prueba con contenido vacío: el consumidor lo rechaza y termina en el DLT. */
    public Notificacion publicarMensajeInvalido() {
        return publicar(props.topics().notificaciones(), EventoTipo.NOTIFICACION, null, "");
    }

    /**
     * Núcleo: guarda la Notificacion PENDIENTE y envía el DTO esperando la
     * confirmación del broker. Si Kafka falla, la deja como FALLIDO y lanza
     * ServicioMensajeriaException (HTTP 503).
     */
    public Notificacion publicar(String topic, EventoTipo tipo, String destinatario, String contenido) {
        // En la BD los eventos de Kafka se registran como NOTIFICACION; el tipo
        // de negocio concreto viaja en el DTO (PRESTAMO_CREADO, etc.).
        String correlationId = AuditoriaContext.correlationId();
        String usuario = AuditoriaContext.usuarioActual();
        Notificacion notificacion = notificacionService.crearPendiente(
                contenido, NotificacionTipo.NOTIFICACION, topic, destinatario,
                tipo, correlationId, usuario);

        EventoMensajeria evento = new EventoMensajeria(
                notificacion.getMensajeId(),
                tipo,
                destinatario,
                contenido,
                OffsetDateTime.now().toString(),
                correlationId,
                usuario
        );

        try {
            kafkaTemplate.send(topic, evento.id(), evento).get(props.sendTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw falla(notificacion, topic, e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw falla(notificacion, topic, e);
        }

        log.info("Mensaje publicado en Kafka topic={} key={} tipo={} correlationId={} usuario={}",
                topic, evento.id(), tipo, correlationId, usuario);
        return notificacion;
    }

    private ServicioMensajeriaException falla(Notificacion notificacion, String topic, Exception causa) {
        String detalle = "No se pudo publicar en Kafka (tópico " + topic + "): " + causa.getMessage();
        try {
            notificacionService.marcarFallido(notificacion.getMensajeId(), detalle);
        } catch (Exception e) {
            log.error("No se pudo marcar la notificación {} como FALLIDO", notificacion.getMensajeId(), e);
        }
        log.error(detalle);
        return new ServicioMensajeriaException(detalle, causa);
    }
}
