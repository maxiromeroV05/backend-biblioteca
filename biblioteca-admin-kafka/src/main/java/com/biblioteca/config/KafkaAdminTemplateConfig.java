package com.biblioteca.config;

import com.biblioteca.messaging.event.EventoMensajeria;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Productor tipado del microservicio administrador de Kafka.
 *
 * El productor de negocio vive en biblioteca-api, pero este servicio publica
 * mensajes de prueba por `/api/kafka/publish`; necesita el mismo contrato
 * (key String, valor EventoMensajeria en JSON) para que los consumidores de la
 * API puedan procesarlos y enviarlos al DLT cuando sean inválidos.
 */
@Configuration
public class KafkaAdminTemplateConfig {

    @Bean
    public KafkaTemplate<String, EventoMensajeria> kafkaTemplate(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }
}
