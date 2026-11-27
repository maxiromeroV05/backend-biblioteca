package com.biblioteca.messaging.kafka;

import com.biblioteca.messaging.event.EventoMensajeria;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidores de Kafka. Los listeners SOLO reciben el registro y delegan en
 * {@link KafkaEventoProcesadorService} (validación, logging y dominio).
 *
 *  - Un grupo por tópico permite balancear carga y dividir responsabilidades.
 *  - El listener de DLT usa StringDeserializer para poder loguear el payload
 *    original aunque no fuera un JSON válido.
 *
 * Con app.kafka.enabled=false los listeners no arrancan.
 */
@Component
public class KafkaEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventoConsumer.class);

    private final KafkaEventoProcesadorService procesador;

    public KafkaEventoConsumer(KafkaEventoProcesadorService procesador) {
        this.procesador = procesador;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.notificaciones}",
            groupId = "${app.kafka.groups.notificaciones}",
            concurrency = "${app.kafka.listener.concurrency:3}",
            autoStartup = "${app.kafka.enabled:true}")
    public void consumir(ConsumerRecord<String, EventoMensajeria> registro) {
        log.debug("Evento de notificaciones recibido en {}/{}", registro.topic(), registro.partition());
        procesador.procesar(registro);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.prestamos}",
            groupId = "${app.kafka.groups.prestamos}",
            concurrency = "${app.kafka.listener.concurrency:3}",
            autoStartup = "${app.kafka.enabled:true}")
    public void consumirPrestamo(ConsumerRecord<String, EventoMensajeria> registro) {
        log.debug("Evento de préstamos recibido en {}/{}", registro.topic(), registro.partition());
        procesador.procesar(registro);
    }

    @KafkaListener(
            topics = {"${app.kafka.topics.notificaciones}${app.kafka.dlt-suffix}",
                      "${app.kafka.topics.prestamos}${app.kafka.dlt-suffix}"},
            groupId = "${app.kafka.groups.dlt}",
            containerFactory = "dltKafkaListenerContainerFactory",
            concurrency = "${app.kafka.listener.concurrency:3}",
            autoStartup = "${app.kafka.enabled:true}")
    public void registrarMensajeMuerto(ConsumerRecord<String, String> registro) {
        procesador.registrarMensajeMuerto(registro);
    }
}
