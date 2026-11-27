package com.biblioteca.config;

import com.biblioteca.exception.MensajeInvalidoException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejo de mensajes que no se pudieron procesar (equivalente a la DLQ de RabbitMQ).
 *
 *  1. MensajeInvalidoException (error NO recuperable): el mensaje va directo al
 *     tópico de mensajes muertos (DLT), sin reintentos.
 *  2. Cualquier otra excepción (error recuperable): se reintenta hasta
 *     app.kafka.retry.max-attempts veces y, si se agotan, va al DLT.
 *
 * El DLT de un tópico "x" es "x.DLT" (ver KafkaTopicsProperties.dlt). Los DLT se
 * publican como bytes (sin converter de DTO) para conservar el payload original
 * también cuando falla la deserialización. Un listener con StringDeserializer
 * registra cada mensaje muerto en los logs.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private final JsonMapper jsonMapper;

    public KafkaErrorHandlingConfig(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            @Qualifier("dltKafkaTemplate") KafkaTemplate<String, byte[]> dltKafkaTemplate,
            KafkaTopicsProperties props) {

        // Partición -1: el productor elige la partición del DLT según la key del mensaje.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (registro, error) -> new TopicPartition(props.dlt(registro.topic()), -1)) {

            @Override
            protected ProducerRecord<Object, Object> createProducerRecord(
                    ConsumerRecord<?, ?> registro, TopicPartition destino, Headers cabeceras,
                    byte[] keyBytes, byte[] valueBytes) {

                // Si la deserialización falló, Spring Kafka entrega los bytes originales.
                // Si falló la validación, serializamos el DTO tal como llegó.
                byte[] valor = valueBytes != null
                        ? valueBytes
                        : jsonMapper.writeValueAsString(registro.value()).getBytes(StandardCharsets.UTF_8);
                Object key = keyBytes != null ? new String(keyBytes, StandardCharsets.UTF_8) : registro.key();
                Integer particion = destino.partition() < 0 ? null : destino.partition();
                return new ProducerRecord<>(destino.topic(), particion, key, valor, cabeceras);
            }
        };
        recoverer.setLogRecoveryRecord(true);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(props.retry().backoffMs(), props.retry().maxAttempts()));

        handler.addNotRetryableExceptions(MensajeInvalidoException.class);
        return handler;
    }

    /**
     * Los DLT se leen como String: así se puede loguear el payload original incluso
     * cuando no era un JSON válido y la deserialización a DTO falló.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory(
            KafkaProperties properties, KafkaTopicsProperties appProps) {

        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, appProps.groups().dlt());

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(config);
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(appProps.listener().concurrency());
        return factory;
    }
}
