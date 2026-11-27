package com.biblioteca.messaging.consumer.notificaciones;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas del consumidor de RabbitMQ y de su política de ACK/NACK (ConsumidorBase),
 * con Channel y RabbitTemplate simulados: no necesitan un RabbitMQ levantado.
 *
 *  - Mensaje válido                 -> ACK
 *  - Mensaje inválido               -> NACK sin requeue (va a la DLQ)
 *  - Error recuperable con intentos -> se republica en la cola y se hace ACK del original
 *  - Error recuperable sin intentos -> NACK sin requeue (va a la DLQ)
 */
class NotificacionConsumerTest {

    private static final String COLA = "notificaciones.queue";
    private static final long TAG = 7L;

    private NotificacionService notificacionService;
    private RabbitTemplate rabbitTemplate;
    private Channel channel;
    private NotificacionConsumer consumer;

    @BeforeEach
    void setUp() {
        notificacionService = mock(NotificacionService.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        channel = mock(Channel.class);

        // Solo se usa Retry: hasta 3 reintentos antes de mandar el mensaje a la DLQ.
        MessagingProperties props = new MessagingProperties(null, null, null, new MessagingProperties.Retry(3), null);
        consumer = new NotificacionConsumer(notificacionService, rabbitTemplate, props);
    }

    private Message mensaje(Integer reintentosPrevios) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(TAG);
        propiedades.setConsumerQueue(COLA);
        if (reintentosPrevios != null) {
            propiedades.setHeader("x-retry-count", reintentosPrevios);
        }
        return new Message(new byte[0], propiedades);
    }

    @Test
    void mensajeValidoSeProcesaYSeConfirmaConAck() throws IOException {
        Map<String, Object> payload = Map.of(
                "id", "m-1", "tipo", "NOTIFICACION", "destinatario", "ana@correo.cl", "contenido", "Hola");

        consumer.consumir(payload, mensaje(null), channel);

        verify(notificacionService).marcarProcesado("m-1");
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void contenidoVacioSeRechazaSinReencolarYVaALaDlq() throws IOException {
        Map<String, Object> payload = Map.of("id", "m-2", "contenido", "");

        consumer.consumir(payload, mensaje(null), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(notificacionService).marcarFallido("m-2", "El contenido del mensaje está vacío");
        verify(notificacionService, never()).marcarProcesado("m-2");
    }

    @Test
    void mensajeSinIdSeRechazaYVaALaDlq() throws IOException {
        Map<String, Object> payload = Map.of("contenido", "Hola");

        consumer.consumir(payload, mensaje(null), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorRecuperableSeReintentaRepublicandoElMensaje() throws IOException {
        doThrow(new IllegalStateException("BD caída")).when(notificacionService).marcarProcesado("m-3");
        Map<String, Object> payload = Map.of("id", "m-3", "contenido", "Hola");

        consumer.consumir(payload, mensaje(0), channel);

        verify(rabbitTemplate).send(eq(""), eq(COLA), any(Message.class));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void sinReintentosDisponiblesElMensajeVaALaDlq() throws IOException {
        doThrow(new IllegalStateException("BD caída")).when(notificacionService).marcarProcesado("m-4");
        Map<String, Object> payload = Map.of("id", "m-4", "contenido", "Hola");

        consumer.consumir(payload, mensaje(3), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(notificacionService).marcarFallido("m-4", "BD caída");
    }
}
