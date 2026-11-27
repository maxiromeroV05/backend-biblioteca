package com.biblioteca.messaging.kafka;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.exception.ServicioMensajeriaException;
import com.biblioteca.messaging.event.AuditoriaContext;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publicación de prueba del microservicio administrador de Kafka.
 *
 * Es independiente del productor de negocio (que vive en biblioteca-api y
 * registra la Notificacion en la BD): aquí solo se construye el DTO tipado y
 * se envía al tópico indicado para poder demostrar el flujo y el DLT.
 */
@Component
public class KafkaAdminPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminPublisher.class);

    private final KafkaTemplate<String, EventoMensajeria> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public KafkaAdminPublisher(KafkaTemplate<String, EventoMensajeria> kafkaTemplate,
                               KafkaTopicsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public Notificacion publicar(String topic, String contenido) {
        Notificacion notificacion = new Notificacion(contenido, NotificacionTipo.NOTIFICACION, topic);
        EventoMensajeria evento = new EventoMensajeria(
                notificacion.getMensajeId(), EventoTipo.NOTIFICACION, null, contenido,
                OffsetDateTime.now().toString(),
                AuditoriaContext.correlationId(), AuditoriaContext.usuarioActual());

        try {
            kafkaTemplate.send(topic, evento.id(), evento).get(props.sendTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw falla(topic, e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw falla(topic, e);
        }

        log.info("Mensaje de prueba publicado en Kafka topic={} key={}", topic, evento.id());
        return notificacion;
    }

    /** Publica el DTO exacto recibido por POST /api/kafka/publish-evento. */
    public Notificacion publicar(String topic, EventoMensajeria evento) {
        try {
            kafkaTemplate.send(topic, evento.id(), evento)
                    .get(props.sendTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw falla(topic, e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw falla(topic, e);
        }

        Notificacion notificacion = new Notificacion(
                evento.contenido(), NotificacionTipo.NOTIFICACION, topic);
        notificacion.setMensajeId(evento.id());
        notificacion.setDestinatario(evento.destinatario());

        log.info("Evento de prueba publicado en Kafka topic={} key={} tipo={} correlationId={}",
                topic, evento.id(), evento.tipo(), evento.correlationId());
        return notificacion;
    }

    /** Contenido vacío: el consumidor lo rechaza y termina en el DLT. */
    public Notificacion publicarInvalido() {
        return publicar(props.topics().notificaciones(), "");
    }

    private ServicioMensajeriaException falla(String topic, Exception causa) {
        String detalle = "No se pudo publicar en Kafka (tópico " + topic + "): " + causa.getMessage();
        log.error(detalle);
        return new ServicioMensajeriaException(detalle, causa);
    }
}
