package com.biblioteca.messaging.kafka;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba que el endpoint publish-evento envía el DTO sin transformarlo. */
class KafkaAdminPublisherTest {

    @Test
    void publicaElDtoRecibidoComoValorYComoKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventoMensajeria> template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, EventoMensajeria>> futuro =
                CompletableFuture.completedFuture(null);
        when(template.send(org.mockito.ArgumentMatchers.eq("biblioteca.prestamos"),
                org.mockito.ArgumentMatchers.eq("msg-1"),
                org.mockito.ArgumentMatchers.any(EventoMensajeria.class)))
                .thenReturn(futuro);

        KafkaTopicsProperties props = new KafkaTopicsProperties(
                true, ".DLT", 2,
                new KafkaTopicsProperties.Topics("biblioteca.notificaciones", "biblioteca.prestamos"),
                new KafkaTopicsProperties.TopicDefaults(3, 3),
                Map.of(), Map.of(),
                new KafkaTopicsProperties.Retry(3, 10),
                new KafkaTopicsProperties.Groups("g-not", "g-prest", "g-dlt"),
                new KafkaTopicsProperties.Listener(3));
        KafkaAdminPublisher publisher = new KafkaAdminPublisher(template, props);

        EventoMensajeria evento = new EventoMensajeria(
                "msg-1", EventoTipo.PRESTAMO_DEVUELTO, "ana@correo.cl",
                "Préstamo devuelto", "2026-11-27T10:00:00Z", "corr-123", "ana@correo.cl");

        Notificacion resultado = publisher.publicar("biblioteca.prestamos", evento);

        assertEquals("msg-1", resultado.getMensajeId());
        ArgumentCaptor<EventoMensajeria> captor = ArgumentCaptor.forClass(EventoMensajeria.class);
        verify(template).send(org.mockito.ArgumentMatchers.eq("biblioteca.prestamos"),
                org.mockito.ArgumentMatchers.eq("msg-1"), captor.capture());
        assertEquals(evento, captor.getValue());
    }
}
