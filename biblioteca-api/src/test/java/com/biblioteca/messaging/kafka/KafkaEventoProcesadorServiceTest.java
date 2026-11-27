package com.biblioteca.messaging.kafka;

import com.biblioteca.exception.MensajeInvalidoException;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.service.NotificacionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KafkaEventoProcesadorServiceTest {

    private NotificacionService notificacionService;
    private KafkaEventoProcesadorService procesador;

    @BeforeEach
    void setUp() {
        notificacionService = mock(NotificacionService.class);
        procesador = new KafkaEventoProcesadorService(notificacionService);
    }

    private ConsumerRecord<String, EventoMensajeria> registro(String topic, EventoMensajeria evento) {
        return new ConsumerRecord<>(topic, 0, 0L, evento == null ? null : evento.id(), evento);
    }

    @Test
    void mensajeValidoSeMarcaComoProcesado() {
        EventoMensajeria evento = new EventoMensajeria("abc-1", EventoTipo.NOTIFICACION,
                "ana@correo.cl", "Hola", "2026-11-23T17:00:00-03:00");

        procesador.procesar(registro("biblioteca.notificaciones", evento));

        verify(notificacionService).marcarProcesado("abc-1");
    }

    @Test
    void contenidoVacioEsErrorNoRecuperable() {
        EventoMensajeria evento = new EventoMensajeria("abc-2", EventoTipo.NOTIFICACION, null, "", null);

        assertThrows(MensajeInvalidoException.class,
                () -> procesador.procesar(registro("biblioteca.notificaciones", evento)));

        verify(notificacionService, never()).marcarProcesado("abc-2");
    }

    @Test
    void mensajeSinIdEsErrorNoRecuperable() {
        EventoMensajeria evento = new EventoMensajeria(null, EventoTipo.NOTIFICACION, null, "Hola", null);

        assertThrows(MensajeInvalidoException.class,
                () -> procesador.procesar(registro("biblioteca.prestamos", evento)));
    }

    @Test
    void payloadNuloEsErrorNoRecuperable() {
        assertThrows(MensajeInvalidoException.class,
                () -> procesador.procesar(registro("biblioteca.prestamos", null)));
    }

    @Test
    void eventoPrestamoConTipoPropioSeProcesa() {
        EventoMensajeria evento = new EventoMensajeria("abc-4", EventoTipo.PRESTAMO_DEVUELTO,
                "ana@correo.cl", "Préstamo devuelto", "2026-11-26T18:00:00-03:00");

        procesador.procesar(registro("biblioteca.prestamos", evento));

        verify(notificacionService).marcarProcesado("abc-4");
    }

    @Test
    void eventoSinTipoEsErrorNoRecuperable() {
        EventoMensajeria evento = new EventoMensajeria("abc-5", null, null, "Hola", null);

        assertThrows(MensajeInvalidoException.class,
                () -> procesador.procesar(registro("biblioteca.prestamos", evento)));
    }

    @Test
    void mensajeMuertoMarcaLaNotificacionComoFallidaConElMotivo() {
        ConsumerRecord<String, String> muerto =
                new ConsumerRecord<>("biblioteca.notificaciones.DLT", 0, 0L, "abc-3", "{\"contenido\":\"\"}");
        muerto.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                "El contenido del mensaje está vacío".getBytes(StandardCharsets.UTF_8));
        muerto.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC,
                "biblioteca.notificaciones".getBytes(StandardCharsets.UTF_8));

        procesador.registrarMensajeMuerto(muerto);

        verify(notificacionService).marcarFallido("abc-3", "El contenido del mensaje está vacío");
    }
}
