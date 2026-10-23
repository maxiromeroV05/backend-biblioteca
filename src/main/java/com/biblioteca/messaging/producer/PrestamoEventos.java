package com.biblioteca.messaging.producer;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.entity.Prestamo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Eventos de mensajería del dominio de préstamos.
 *
 * IMPORTANTE: nunca lanza excepciones hacia afuera. Si RabbitMQ no está
 * disponible, el préstamo igual se crea o devuelve: la mensajería es
 * asíncrona y desacoplada, no puede romper la lógica existente.
 */
@Component
public class PrestamoEventos {

    private static final Logger log = LoggerFactory.getLogger(PrestamoEventos.class);

    private final EventoPublisher publisher;
    private final MessagingProperties props;

    public PrestamoEventos(EventoPublisher publisher, MessagingProperties props) {
        this.publisher = publisher;
        this.props = props;
    }

    /** Al crear un préstamo: notificación al usuario + ticket del préstamo. */
    public void prestamoCreado(Prestamo prestamo) {
        try {
            String destinatario = destinatario(prestamo);
            String libro = titulo(prestamo);

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
    }

    /** Al devolver un préstamo: notificación al usuario + comprobante de devolución. */
    public void prestamoDevuelto(Prestamo prestamo) {
        try {
            String destinatario = destinatario(prestamo);
            String libro = titulo(prestamo);

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
    }

    private String destinatario(Prestamo prestamo) {
        return prestamo.getUsuario() != null ? prestamo.getUsuario().getEmail() : null;
    }

    private String titulo(Prestamo prestamo) {
        return prestamo.getLibro() != null ? prestamo.getLibro().getTitulo() : "(sin título)";
    }
}
