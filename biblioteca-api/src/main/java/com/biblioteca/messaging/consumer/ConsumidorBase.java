package com.biblioteca.messaging.consumer;

import com.biblioteca.exception.MensajeInvalidoException;
import com.biblioteca.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.Map;

/**
 * Lógica común de confirmación (ACK/NACK) de los consumidores.
 *
 * Decisión ante cada mensaje:
 *
 *  1. Procesa bien                      -> ACK.
 *  2. MensajeInvalidoException          -> error NO recuperable: NACK sin requeue -> DLQ.
 *  3. Cualquier otra excepción          -> error recuperable:
 *        - si quedan reintentos: se republica en la misma cola con el header
 *          x-retry-count + 1 y se hace ACK del original;
 *        - si se agotaron: NACK sin requeue -> DLQ.
 *
 * Cada subclase solo implementa ejecutar(), que es la tarea de su dominio.
 */
public abstract class ConsumidorBase {

    private static final String HEADER_REINTENTOS = "x-retry-count";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final NotificacionService notificacionService;
    private final RabbitTemplate rabbitTemplate;
    private final int maxIntentos;

    protected ConsumidorBase(NotificacionService notificacionService,
                             RabbitTemplate rabbitTemplate,
                             int maxIntentos) {
        this.notificacionService = notificacionService;
        this.rabbitTemplate = rabbitTemplate;
        this.maxIntentos = maxIntentos;
    }

    /** Tarea específica del dominio (enviar notificación, emitir ticket, generar documento). */
    protected abstract void ejecutar(Map<String, Object> payload);

    protected final void procesar(Map<String, Object> payload, Message message, Channel channel)
            throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String mensajeId = String.valueOf(payload.get("id"));

        try {
            validar(payload);
            ejecutar(payload);
            notificacionService.marcarProcesado(mensajeId);
            channel.basicAck(deliveryTag, false);
            log.info("Mensaje procesado id={} cola={}", mensajeId, message.getMessageProperties().getConsumerQueue());

        } catch (MensajeInvalidoException e) {
            log.error("Mensaje inválido (no recuperable), se envía a DLQ. cola={} id={} motivo={} payload={}",
                    message.getMessageProperties().getConsumerQueue(), mensajeId, e.getMessage(), payload);
            marcarFallido(mensajeId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            int intentos = intentosPrevios(message);

            if (intentos < maxIntentos) {
                log.warn("Error recuperable id={} (intento {}/{}): {}. Se reintenta.",
                        mensajeId, intentos + 1, maxIntentos, e.getMessage());
                try {
                    reencolar(message, intentos + 1);
                    channel.basicAck(deliveryTag, false);
                    return;
                } catch (Exception errorReencolado) {
                    log.error("No se pudo reencolar el mensaje id={}", mensajeId, errorReencolado);
                }
            }

            log.error("Reintentos agotados, se envía a DLQ. cola={} id={} error={} payload={}",
                    message.getMessageProperties().getConsumerQueue(), mensajeId, e.getMessage(), payload);
            marcarFallido(mensajeId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validar(Map<String, Object> payload) {
        Object id = payload.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new MensajeInvalidoException("El mensaje no trae identificador");
        }
        Object contenido = payload.get("contenido");
        if (contenido == null || contenido.toString().isBlank()) {
            throw new MensajeInvalidoException("El contenido del mensaje está vacío");
        }
    }

    private int intentosPrevios(Message message) {
        Object valor = message.getMessageProperties().getHeader(HEADER_REINTENTOS);
        return valor instanceof Number n ? n.intValue() : 0;
    }

    /** Republica el mensaje SOLO en esta cola (exchange por defecto + nombre de la cola). */
    private void reencolar(Message message, int intento) {
        Message copia = MessageBuilder.fromMessage(message)
                .setHeader(HEADER_REINTENTOS, intento)
                .build();
        rabbitTemplate.send("", message.getMessageProperties().getConsumerQueue(), copia);
    }

    private void marcarFallido(String mensajeId, String error) {
        try {
            notificacionService.marcarFallido(mensajeId, error);
        } catch (Exception e) {
            log.error("No se pudo marcar la notificación {} como FALLIDO", mensajeId, e);
        }
    }
}
