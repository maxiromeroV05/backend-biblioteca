package com.biblioteca.messaging.kafka;

import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Los listeners no contienen lógica: solo entregan el registro al servicio.
 * La validación y el dominio se prueban en KafkaEventoProcesadorServiceTest.
 */
class KafkaEventoConsumerTest {

    private KafkaEventoProcesadorService procesador;
    private KafkaEventoConsumer consumer;

    @BeforeEach
    void setUp() {
        procesador = mock(KafkaEventoProcesadorService.class);
        consumer = new KafkaEventoConsumer(procesador);
    }

    @Test
    void notificacionesDelegaEnElServicio() {
        EventoMensajeria evento = new EventoMensajeria("abc-1", EventoTipo.NOTIFICACION,
                "ana@correo.cl", "Hola", "2026-11-23T17:00:00-03:00");
        ConsumerRecord<String, EventoMensajeria> registro =
                new ConsumerRecord<>("biblioteca.notificaciones", 0, 0L, "abc-1", evento);

        consumer.consumir(registro);

        verify(procesador).procesar(registro);
    }

    @Test
    void prestamosDelegaEnElServicio() {
        EventoMensajeria evento = new EventoMensajeria("abc-2", EventoTipo.NOTIFICACION,
                "ana@correo.cl", "Préstamo", "2026-11-23T17:00:00-03:00");
        ConsumerRecord<String, EventoMensajeria> registro =
                new ConsumerRecord<>("biblioteca.prestamos", 1, 5L, "abc-2", evento);

        consumer.consumirPrestamo(registro);

        verify(procesador).procesar(registro);
    }

    @Test
    void mensajeMuertoDelegaEnElServicio() {
        ConsumerRecord<String, String> registro =
                new ConsumerRecord<>("biblioteca.notificaciones.DLT", 0, 0L, "abc-3", "{\"contenido\":\"\"}");

        consumer.registrarMensajeMuerto(registro);

        verify(procesador).registrarMensajeMuerto(registro);
    }
}
