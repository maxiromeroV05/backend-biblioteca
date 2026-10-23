package com.biblioteca.messaging.consumer.dlq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Escucha las 3 DLQ y registra en logs cada mensaje no entregado, indicando
 * la cola de origen y el motivo (header x-death de RabbitMQ).
 *
 * Solo se activa con app.rabbit.dlq-listener.enabled=true, porque al
 * consumirlos los mensajes desaparecen de la DLQ. Con el valor por defecto
 * (false) quedan en la DLQ para poder verlos en el panel de RabbitMQ.
 */
@Component
@ConditionalOnProperty(prefix = "app.rabbit.dlq-listener", name = "enabled", havingValue = "true")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = {
            "${app.rabbit.queues.notificaciones.dlq}",
            "${app.rabbit.queues.tickets.dlq}",
            "${app.rabbit.queues.documentos.dlq}"
    })
    public void registrar(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        MessageProperties propiedades = message.getMessageProperties();

        String colaOrigen = "desconocida";
        String motivo = "desconocido";
        List<Map<String, ?>> xDeath = propiedades.getXDeathHeader();
        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, ?> primera = xDeath.get(0);
            colaOrigen = String.valueOf(primera.get("queue"));
            motivo = String.valueOf(primera.get("reason"));
        }

        log.error("[DLQ] mensaje no entregado. dlq={} colaOrigen={} motivo={} payload={}",
                propiedades.getConsumerQueue(), colaOrigen, motivo, payload);

        channel.basicAck(propiedades.getDeliveryTag(), false);
    }
}
