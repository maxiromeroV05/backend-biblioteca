package com.biblioteca.messaging.producer;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.config.MessagingProperties;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.messaging.kafka.KafkaEventoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Eventos de mensajería del dominio de préstamos.
 *
 * Cada evento se publica en RabbitMQ (notificación + ticket/documento) y en
 * Kafka (tópico de préstamos). Son dos publicaciones independientes: si una
 * falla, la otra igual se intenta.
 *
 * IMPORTANTE: nunca lanza excepciones hacia afuera. Si RabbitMQ o Kafka no
 * están disponibles, el préstamo igual se crea o devuelve: la mensajería es
 * asíncrona y desacoplada, no puede romper la lógica existente.
 */
@Component
public class PrestamoEventos {

    private static final Logger log = LoggerFactory.getLogger(PrestamoEventos.class);

    private final EventoPublisher publisher;
    private final MessagingProperties props;
    private final KafkaEventoPublisher kafkaPublisher;
    private final KafkaTopicsProperties kafkaProps;

    public PrestamoEventos(EventoPublisher publisher,
                           MessagingProperties props,
                           KafkaEventoPublisher kafkaPublisher,
                           KafkaTopicsProperties kafkaProps) {
        this.publisher = publisher;
        this.props = props;
        this.kafkaPublisher = kafkaPublisher;
        this.kafkaProps = kafkaProps;
    }

    /** Al crear un préstamo: notificación al usuario + ticket del préstamo. */
    public void prestamoCreado(Prestamo prestamo) {
        String destinatario = destinatario(prestamo);
        String libro = titulo(prestamo);

        try {
            publisher.publicarNotificacion(
                    props.routing().notificacionPrestamoCreado(),
                    destinatario,
                    "Préstamo registrado: '" + libro + "'. Devolución esperada: "
                            + prestamo.getFechaDevolucionEsperada());

            publisher.publicarTicket(
                    destinatario,
                    "Ticket de préstamo #" + prestamo.getId() + " - '" + libro + "'");
        } catch (Exception e) {
            log.warn("No se pudieron publicar los eventos del préstamo {}: {}", prestamo.getId(), e.getMessage());
        }

        publicarEnKafka(prestamo, EventoTipo.PRESTAMO_CREADO, destinatario,
                "Préstamo #" + prestamo.getId() + " creado: '" + libro + "'. Devolución esperada: "
                        + prestamo.getFechaDevolucionEsperada());
    }

    /** Al devolver un préstamo: notificación al usuario + comprobante de devolución. */
    public void prestamoDevuelto(Prestamo prestamo) {
        String destinatario = destinatario(prestamo);
        String libro = titulo(prestamo);

        try {
            publisher.publicarNotificacion(
                    props.routing().notificacionPrestamoDevuelto(),
                    destinatario,
                    "Devolución registrada: '" + libro + "'. Gracias por devolverlo.");

            publisher.publicarDocumento(
                    destinatario,
                    "Comprobante de devolución del préstamo #" + prestamo.getId() + " - '" + libro + "'");
        } catch (Exception e) {
            log.warn("No se pudieron publicar los eventos de devolución del préstamo {}: {}",
                    prestamo.getId(), e.getMessage());
        }

        publicarEnKafka(prestamo, EventoTipo.PRESTAMO_DEVUELTO, destinatario,
                "Préstamo #" + prestamo.getId() + " devuelto: '" + libro + "'");
    }

    /** Publica el evento en el tópico de préstamos de Kafka (si Kafka está habilitado). */
    private void publicarEnKafka(Prestamo prestamo, EventoTipo tipo, String destinatario, String contenido) {
        if (!kafkaProps.enabled()) {
            return;
        }
        try {
            kafkaPublisher.publicarPrestamo(tipo, destinatario, contenido);
        } catch (Exception e) {
            log.warn("No se pudo publicar en Kafka el evento del préstamo {}: {}", prestamo.getId(), e.getMessage());
        }
    }

    // Los dos métodos siguientes no pueden lanzar excepciones (por ejemplo, por una
    // relación lazy sin sesión): un fallo aquí no debe romper el préstamo.
    private String destinatario(Prestamo prestamo) {
        try {
            return prestamo.getUsuario() != null ? prestamo.getUsuario().getEmail() : null;
        } catch (Exception e) {
            log.warn("No se pudo leer el destinatario del préstamo {}: {}", prestamo.getId(), e.getMessage());
            return null;
        }
    }

    private String titulo(Prestamo prestamo) {
        try {
            return prestamo.getLibro() != null ? prestamo.getLibro().getTitulo() : "(sin título)";
        } catch (Exception e) {
            log.warn("No se pudo leer el título del préstamo {}: {}", prestamo.getId(), e.getMessage());
            return "(sin título)";
        }
    }
}
