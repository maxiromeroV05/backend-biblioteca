package com.biblioteca.messaging.consumer.documentos;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.messaging.consumer.ConsumidorBase;
import com.biblioteca.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/** Dominio DOCUMENTOS: consume documentos.queue (exchange direct). */
@Component
public class DocumentoConsumer extends ConsumidorBase {

    public DocumentoConsumer(NotificacionService notificacionService,
                             RabbitTemplate rabbitTemplate,
                             MessagingProperties props) {
        super(notificacionService, rabbitTemplate, props.retry().maxAttempts());
    }

    @RabbitListener(queues = "${app.rabbit.queues.documentos.name}")
    public void consumir(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        procesar(payload, message, channel);
    }

    @Override
    protected void ejecutar(Map<String, Object> payload) {
        log.info("Generando documento para {}: {}", payload.get("destinatario"), payload.get("contenido"));
    }
}
