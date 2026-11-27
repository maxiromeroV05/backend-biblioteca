package com.biblioteca.service;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.dto.kafka.metrics.ConsumerGroupMetrics;
import com.biblioteca.dto.kafka.metrics.KafkaMetricsResponse;
import com.biblioteca.dto.kafka.metrics.MetricsSummary;
import com.biblioteca.dto.kafka.metrics.PartitionConsumption;
import com.biblioteca.dto.kafka.metrics.TopicMetrics;
import com.biblioteca.exception.ServicioMensajeriaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Métricas de Kafka para el panel del frontend. Todo se consulta con el
 * cliente Admin: offsets finales, offsets confirmados por consumer group, lag
 * y número de mensajes en los DLT.
 *
 * Las tasas por minuto se calculan comparando la instantánea actual con la
 * anterior (en memoria), por eso la primera respuesta devuelve 0 y el front
 * debe hacer polling con el intervalo indicado en la respuesta.
 */
@Service
public class KafkaMetricasService {

    private static final long ESPERA_SEGUNDOS = 10;
    private static final Logger log = LoggerFactory.getLogger(KafkaMetricasService.class);

    private final KafkaAdmin kafkaAdmin;
    private final KafkaTopicsProperties props;
    private final int pollingIntervalSeconds;
    private final ConcurrentHashMap<String, Muestra> muestras = new ConcurrentHashMap<>();

    public KafkaMetricasService(
            KafkaAdmin kafkaAdmin,
            KafkaTopicsProperties props,
            @Value("${app.kafka.metrics.polling-interval-seconds:5}") int pollingIntervalSeconds) {
        this.kafkaAdmin = kafkaAdmin;
        this.props = props;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public KafkaMetricsResponse obtener() {
        try (Admin admin = crearCliente()) {
            Instant ahora = Instant.now();
            List<String> topics = List.of(props.topics().notificaciones(), props.topics().prestamos());

            Map<String, TopicDescription> descripciones = admin.describeTopics(topics)
                    .allTopicNames().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);

            Map<TopicPartition, Long> offsFinales = new HashMap<>();
            Map<TopicPartition, Long> offsDlt = new HashMap<>();
            for (String topic : topics) {
                TopicDescription descripcion = descripciones.get(topic);
                if (descripcion == null) {
                    continue;
                }
                offsFinales.putAll(offsetsFinales(admin, particiones(topic, descripcion)));

                String dlt = props.dlt(topic);
                try {
                    TopicDescription dltDesc = admin.describeTopics(List.of(dlt))
                            .allTopicNames().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(dlt);
                    if (dltDesc != null) {
                        offsDlt.putAll(offsetsFinales(admin, particiones(dlt, dltDesc)));
                    }
                } catch (Exception e) {
                    // Un DLT aún no creado no debe romper el panel de métricas.
                    log.debug("No se pudieron leer los offsets del DLT {}: {}", dlt, e.getMessage());
                }
            }

            List<TopicMetrics> metricasTopicos = new ArrayList<>();
            double totalEventosPorMinuto = 0;
            long totalFallos = 0;
            for (String topic : topics) {
                TopicDescription descripcion = descripciones.get(topic);
                int particiones = descripcion == null ? 0 : descripcion.partitions().size();
                int replicas = descripcion == null || descripcion.partitions().isEmpty()
                        ? 0 : descripcion.partitions().get(0).replicas().size();

                long mensajes = sumar(offsFinales, topic);
                long fallos = sumar(offsDlt, props.dlt(topic));
                double eventosPorMinuto = tasa("topic:" + topic, mensajes, ahora);
                double fallosPorMinuto = tasa("dlt:" + topic, fallos, ahora);

                metricasTopicos.add(new TopicMetrics(topic, particiones, replicas, mensajes,
                        eventosPorMinuto, fallos, fallosPorMinuto));
                totalEventosPorMinuto += eventosPorMinuto;
                totalFallos += fallos;
            }

            List<ConsumerGroupMetrics> grupos = new ArrayList<>();
            List<PartitionConsumption> distribucion = new ArrayList<>();
            long totalLag = 0;

            Collection<ConsumerGroupListing> listings = admin.listConsumerGroups()
                    .valid().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (listings != null) {
                for (ConsumerGroupListing listing : listings) {
                    try {
                        String groupId = listing.groupId();
                        ConsumerGroupDescription grupo = admin.describeConsumerGroups(List.of(groupId))
                                .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(groupId);
                        if (grupo == null) {
                            continue;
                        }
                        Map<TopicPartition, OffsetAndMetadata> confirmados =
                                admin.listConsumerGroupOffsets(groupId)
                                        .partitionsToOffsetAndMetadata()
                                        .get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
                        if (confirmados == null) {
                            confirmados = Map.of();
                        }

                        Map<TopicPartition, Long> finalesGrupo = offsetsFinales(admin, confirmados.keySet());
                        Map<String, Long> offsetsPorParticion = new LinkedHashMap<>();
                        Map<String, Long> lagPorTopico = new LinkedHashMap<>();
                        long lagGrupo = 0;

                        for (Map.Entry<TopicPartition, OffsetAndMetadata> entrada : confirmados.entrySet()) {
                            TopicPartition tp = entrada.getKey();
                            long confirmado = entrada.getValue().offset();
                            long fin = finalesGrupo.getOrDefault(tp, confirmado);
                            long lag = Math.max(0, fin - confirmado);
                            String clave = tp.topic() + "-" + tp.partition();

                            offsetsPorParticion.put(clave, confirmado);
                            lagPorTopico.merge(tp.topic(), lag, Long::sum);
                            distribucion.add(new PartitionConsumption(
                                    groupId, tp.topic(), tp.partition(), confirmado, fin, lag));
                            lagGrupo += lag;
                        }

                        grupos.add(new ConsumerGroupMetrics(
                                groupId,
                                grupo.state() != null ? grupo.state().name() : "UNKNOWN",
                                grupo.members().size(),
                                offsetsPorParticion,
                                lagPorTopico,
                                lagGrupo));
                        totalLag += lagGrupo;
                    } catch (Exception e) {
                        log.debug("No se pudieron leer las métricas del grupo {}: {}",
                                listing.groupId(), e.getMessage());
                    }
                }
            }

            return new KafkaMetricsResponse(
                    OffsetDateTime.now(ZoneOffset.UTC),
                    pollingIntervalSeconds,
                    metricasTopicos,
                    grupos,
                    distribucion,
                    new MetricsSummary(redondear(totalEventosPorMinuto), totalFallos, totalLag));
        } catch (Exception e) {
            throw falla(e);
        }
    }

    /** Cliente Admin nuevo con timeouts cortos. Protegido para sustituirlo en tests. */
    protected Admin crearCliente() {
        Map<String, Object> configuracion = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        configuracion.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configuracion.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 8000);
        return Admin.create(configuracion);
    }

    private List<TopicPartition> particiones(String topic, TopicDescription descripcion) {
        return descripcion.partitions().stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .toList();
    }

    private Map<TopicPartition, Long> offsetsFinales(Admin admin, Collection<TopicPartition> particiones)
            throws Exception {
        if (particiones.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        for (TopicPartition tp : particiones) {
            consulta.put(tp, OffsetSpec.latest());
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> resultado = admin.listOffsets(consulta)
                .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        Map<TopicPartition, Long> salida = new HashMap<>();
        resultado.forEach((tp, info) -> salida.put(tp, info != null ? info.offset() : 0L));
        return salida;
    }

    private long sumar(Map<TopicPartition, Long> offsets, String topic) {
        return offsets.entrySet().stream()
                .filter(e -> e.getKey().topic().equals(topic))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /** Tasa por minuto entre dos sondeos; la primera muestra siempre es 0. */
    private double tasa(String clave, long valorActual, Instant ahora) {
        Muestra anterior = muestras.put(clave, new Muestra(valorActual, ahora));
        if (anterior == null) {
            return 0.0;
        }
        long milis = Duration.between(anterior.at(), ahora).toMillis();
        if (milis <= 0) {
            return 0.0;
        }
        return redondear(Math.max(0, valorActual - anterior.valor()) * 60000.0 / milis);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private ServicioMensajeriaException falla(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        Throwable causa = e;
        return new ServicioMensajeriaException(
                "No se pudieron obtener las métricas de Kafka: " + causa.getMessage(), causa);
    }

    private record Muestra(long valor, Instant at) {
    }
}
