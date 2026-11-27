package com.biblioteca.messaging.consumer.dlq;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifica que un mensaje que llega a la DLQ queda REGISTRADO EN LOS LOGS
 * (nivel ERROR, con la cola y el contenido) y que se confirma para sacarlo de la DLQ.
 */
class DeadLetterConsumerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(DeadLetterConsumer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void registraElMensajeNoEntregadoEnLosLogsYLoConfirma() throws IOException {
        Channel channel = mock(Channel.class);
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(5L);
        propiedades.setConsumerQueue("notificaciones.dlq");
        Message mensaje = new Message(new byte[0], propiedades);

        new DeadLetterConsumer().registrar(Map.of("id", "m-9", "contenido", ""), mensaje, channel);

        assertEquals(1, appender.list.size());
        ILoggingEvent evento = appender.list.get(0);
        assertEquals(Level.ERROR, evento.getLevel());
        assertTrue(evento.getFormattedMessage().contains("notificaciones.dlq"), "el log debe indicar la DLQ");
        assertTrue(evento.getFormattedMessage().contains("m-9"), "el log debe incluir el mensaje");

        verify(channel).basicAck(5L, false);
    }
}
