package com.biblioteca.messaging.kafka;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.exception.ServicioMensajeriaException;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaEventoPublisherTest {

    private static final String TOPIC_NOTIFICACIONES = "biblioteca.notificaciones";
    private static final String TOPIC_PRESTAMOS = "biblioteca.prestamos";

    private KafkaTemplate<String, EventoMensajeria> kafkaTemplate;
    private NotificacionService notificacionService;
    private KafkaEventoPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        notificacionService = mock(NotificacionService.class);

        KafkaTopicsProperties props = new KafkaTopicsProperties(
                true, ".DLT", 2,
                new KafkaTopicsProperties.Topics(TOPIC_NOTIFICACIONES, TOPIC_PRESTAMOS),
                new KafkaTopicsProperties.TopicDefaults(3, 3),
                Map.of("retention.ms", "604800000", "cleanup.policy", "delete"),
                Map.of("retention.ms", "86400000", "cleanup.policy", "delete"),
                new KafkaTopicsProperties.Retry(3, 10),
                new KafkaTopicsProperties.Groups("g-not", "g-prest", "g-dlt"),
                new KafkaTopicsProperties.Listener(3));

        publisher = new KafkaEventoPublisher(kafkaTemplate, notificacionService, props);
    }

    @Test
    void publicaElDtoConElIdComoKeyEnElTopicDeNotificaciones() {
        Notificacion notificacion = new Notificacion("Hola", NotificacionTipo.NOTIFICACION, TOPIC_NOTIFICACIONES);
        when(notificacionService.crearPendiente(eq("Hola"), eq(NotificacionTipo.NOTIFICACION),
                eq(TOPIC_NOTIFICACIONES), eq("ana@correo.cl"), eq(EventoTipo.NOTIFICACION),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(notificacion);
        when(kafkaTemplate.send(eq(TOPIC_NOTIFICACIONES), eq(notificacion.getMensajeId()),
                org.mockito.ArgumentMatchers.any(EventoMensajeria.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        Notificacion resultado = publisher.publicarNotificacion("ana@correo.cl", "Hola");

        ArgumentCaptor<EventoMensajeria> evento = ArgumentCaptor.forClass(EventoMensajeria.class);
        verify(kafkaTemplate).send(eq(TOPIC_NOTIFICACIONES), eq(notificacion.getMensajeId()), evento.capture());
        assertEquals(notificacion.getMensajeId(), resultado.getMensajeId());
        assertEquals("Hola", evento.getValue().contenido());
        assertEquals("ana@correo.cl", evento.getValue().destinatario());
        assertEquals(notificacion.getMensajeId(), evento.getValue().id());
        assertEquals(EventoTipo.NOTIFICACION, evento.getValue().tipo());
        assertNotNull(evento.getValue().correlationId());
    }

    @Test
    void publicaLosPrestamosConSuTipoDeNegocioEnElOtroTopico() {
        Notificacion notificacion = new Notificacion("Prestamo", NotificacionTipo.NOTIFICACION, TOPIC_PRESTAMOS);
        when(notificacionService.crearPendiente(eq("Prestamo"), eq(NotificacionTipo.NOTIFICACION),
                eq(TOPIC_PRESTAMOS), eq("ana@correo.cl"), eq(EventoTipo.PRESTAMO_CREADO),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(notificacion);
        when(kafkaTemplate.send(eq(TOPIC_PRESTAMOS), anyString(),
                org.mockito.ArgumentMatchers.any(EventoMensajeria.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publicarPrestamo(EventoTipo.PRESTAMO_CREADO, "ana@correo.cl", "Prestamo");

        ArgumentCaptor<EventoMensajeria> evento = ArgumentCaptor.forClass(EventoMensajeria.class);
        verify(kafkaTemplate).send(eq(TOPIC_PRESTAMOS), eq(notificacion.getMensajeId()), evento.capture());
        assertEquals(EventoTipo.PRESTAMO_CREADO, evento.getValue().tipo());
    }

    @Test
    void siKafkaFallaMarcaLaNotificacionComoFallidaYLanza503() {
        Notificacion notificacion = new Notificacion("Hola", NotificacionTipo.NOTIFICACION, TOPIC_NOTIFICACIONES);
        when(notificacionService.crearPendiente(eq("Hola"), eq(NotificacionTipo.NOTIFICACION),
                eq(TOPIC_NOTIFICACIONES), org.mockito.ArgumentMatchers.isNull(),
                eq(EventoTipo.NOTIFICACION), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(notificacion);
        when(kafkaTemplate.send(eq(TOPIC_NOTIFICACIONES), anyString(),
                org.mockito.ArgumentMatchers.any(EventoMensajeria.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caído")));

        assertThrows(ServicioMensajeriaException.class,
                () -> publisher.publicarLibre(TOPIC_NOTIFICACIONES, "Hola"));

        verify(notificacionService).marcarFallido(eq(notificacion.getMensajeId()), anyString());
    }
}
