package com.biblioteca.messaging.kafka;

import com.biblioteca.exception.MensajeInvalidoException;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.service.NotificacionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Lógica de dominio de los eventos de Kafka. Los @KafkaListener solo reciben el
 * registro y delegan aquí: validación, logging contextual y actualización de la
 * Notificacion correspondiente.
 */
@Service
public class KafkaEventoProcesadorService {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventoProcesadorService.class);

    private final NotificacionService notificacionService;

    public KafkaEventoProcesadorService(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Valida el DTO recibido y marca la notificación como PROCESADO. */
    public void procesar(ConsumerRecord<String, EventoMensajeria> registro) {
        EventoMensajeria evento = registro.value();
        validar(evento);

        log.info("Evento Kafka recibido topic={} partition={} offset={} tipo={} destinatario={}: {}",
                registro.topic(), registro.partition(), registro.offset(),
                evento.tipo(), evento.destinatario(), evento.contenido());

        try {
            notificacionService.marcarProcesado(evento.id());
        } catch (IllegalArgumentException e) {
            // Evento publicado por otro productor: no hay Notificacion que actualizar.
            log.warn("Evento {} sin registro de notificación asociado: {}", evento.id(), e.getMessage());
        }
    }

    /** Registra el mensaje muerto (DLT) con su motivo y marca la notificación como FALLIDO. */
    public void registrarMensajeMuerto(ConsumerRecord<String, String> registro) {
        String motivo = cabecera(registro, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String origen = cabecera(registro, KafkaHeaders.DLT_ORIGINAL_TOPIC);

        log.error("Mensaje muerto (DLT) topic={} origen={} key={} motivo={} payload={}",
                registro.topic(), origen, registro.key(), motivo, registro.value());

        if (registro.key() != null && !registro.key().isBlank()) {
            try {
                notificacionService.marcarFallido(registro.key(), motivo);
            } catch (Exception e) {
                log.debug("No se pudo marcar la notificación {} como FALLIDO: {}", registro.key(), e.getMessage());
            }
        }
    }

    private void validar(EventoMensajeria evento) {
        if (evento == null) {
            throw new MensajeInvalidoException("El mensaje no tiene contenido JSON");
        }
        if (evento.id() == null || evento.id().isBlank()) {
            throw new MensajeInvalidoException("El mensaje no trae identificador");
        }
        if (evento.tipo() == null) {
            throw new MensajeInvalidoException("El mensaje no trae tipo de evento");
        }
        if (evento.contenido() == null || evento.contenido().isBlank()) {
            throw new MensajeInvalidoException("El contenido del mensaje está vacío");
        }
    }

    private String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header header = registro.headers().lastHeader(nombre);
        return header == null || header.value() == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
    }
}
