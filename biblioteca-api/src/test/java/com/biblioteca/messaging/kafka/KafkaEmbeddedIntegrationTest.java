package com.biblioteca.messaging.kafka;

import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prueba de integración end-to-end: levanta un broker Kafka real embebido,
 * publica DTOs en los DOS tópicos y comprueba que los @KafkaListener los
 * consumen y convierten a EventoMensajeria (tipo incluido).
 *
 * También publica un mensaje inválido y verifica que, tras los reintentos,
 * acaba en el DLT y es inspeccionado por el listener de mensajes muertos.
 */
@EmbeddedKafka(
        partitions = 2,
        topics = {"biblioteca.notificaciones", "biblioteca.prestamos",
                  "biblioteca.notificaciones.DLT", "biblioteca.prestamos.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@SpringBootTest(properties = {
        "app.kafka.enabled=true",
        "app.kafka.topic-defaults.partitions=2",
        "app.kafka.topic-defaults.replicas=1",
        "app.kafka.listener.concurrency=2",
        "app.kafka.retry.max-attempts=1",
        "app.kafka.retry.backoff-ms=100",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class KafkaEmbeddedIntegrationTest {

    private static final String NOTIFICACIONES = "biblioteca.notificaciones";
    private static final String PRESTAMOS = "biblioteca.prestamos";

    @Autowired
    private KafkaTemplate<String, EventoMensajeria> kafkaTemplate;

    @MockitoSpyBean
    private KafkaEventoProcesadorService procesador;

    @Test
    void publicaYConsumeEnAmbosTopicosConDtoTipado() throws Exception {
        kafkaTemplate.send(NOTIFICACIONES, "n-1", new EventoMensajeria(
                "n-1", EventoTipo.NOTIFICACION, "ana@correo.cl", "Hola", "2026-11-26T18:00:00-03:00"))
                .get(10, TimeUnit.SECONDS);
        kafkaTemplate.send(PRESTAMOS, "p-1", new EventoMensajeria(
                "p-1", EventoTipo.PRESTAMO_CREADO, "ana@correo.cl", "Préstamo creado", "2026-11-26T18:01:00-03:00"))
                .get(10, TimeUnit.SECONDS);

        ArgumentCaptor<ConsumerRecord<String, EventoMensajeria>> captor =
                ArgumentCaptor.forClass(ConsumerRecord.class);
        verify(procesador, timeout(20_000).times(2)).procesar(captor.capture());

        List<ConsumerRecord<String, EventoMensajeria>> recibidos = captor.getAllValues();
        EventoMensajeria notificacion = recibidos.stream()
                .filter(r -> NOTIFICACIONES.equals(r.topic())).findFirst().orElseThrow().value();
        EventoMensajeria prestamo = recibidos.stream()
                .filter(r -> PRESTAMOS.equals(r.topic())).findFirst().orElseThrow().value();

        assertEquals(EventoTipo.NOTIFICACION, notificacion.tipo());
        assertEquals(EventoTipo.PRESTAMO_CREADO, prestamo.tipo());
        assertEquals("Hola", notificacion.contenido());
        assertEquals("ana@correo.cl", prestamo.destinatario());
    }

    @Test
    void mensajeInvalidoTerminaEnElDltYEsRegistrado() throws Exception {
        kafkaTemplate.send(NOTIFICACIONES, "inv-1", new EventoMensajeria(
                "inv-1", EventoTipo.NOTIFICACION, null, "", "2026-11-26T18:02:00-03:00"))
                .get(10, TimeUnit.SECONDS);

        ArgumentCaptor<ConsumerRecord<String, String>> captor = ArgumentCaptor.forClass(ConsumerRecord.class);
        verify(procesador, timeout(20_000)).registrarMensajeMuerto(captor.capture());

        ConsumerRecord<String, String> muerto = captor.getValue();
        assertEquals(NOTIFICACIONES + ".DLT", muerto.topic());
        assertEquals("inv-1", muerto.key());
        assertNotNull(muerto.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE));
        String motivo = new String(muerto.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE).value(),
                StandardCharsets.UTF_8);
        assertEquals(true, motivo.contains("El contenido del mensaje está vacío"));
        assertNotNull(muerto.value()); // el payload original se conserva como texto
    }
}
