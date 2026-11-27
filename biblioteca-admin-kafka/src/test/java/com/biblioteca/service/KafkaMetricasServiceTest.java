package com.biblioteca.service;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.dto.kafka.metrics.KafkaMetricsResponse;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Métricas calculadas a partir de un cliente Admin simulado (sin clúster). */
class KafkaMetricasServiceTest {

    private static final String NOTIFICACIONES = "biblioteca.notificaciones";
    private static final String PRESTAMOS = "biblioteca.prestamos";

    private Admin admin;
    private KafkaMetricasService service;

    @BeforeEach
    void setUp() {
        admin = mock(Admin.class);

        KafkaTopicsProperties props = new KafkaTopicsProperties(
                true, ".DLT", 2,
                new KafkaTopicsProperties.Topics(NOTIFICACIONES, PRESTAMOS),
                new KafkaTopicsProperties.TopicDefaults(3, 3),
                Map.of("retention.ms", "604800000", "cleanup.policy", "delete"),
                Map.of("retention.ms", "86400000", "cleanup.policy", "delete"),
                new KafkaTopicsProperties.Retry(3, 10),
                new KafkaTopicsProperties.Groups("g-not", "g-prest", "g-dlt"),
                new KafkaTopicsProperties.Listener(3));

        service = new KafkaMetricasService(null, props, 5) {
            @Override
            protected Admin crearCliente() {
                return admin;
            }
        };

        prepararTopicos();
        prepararOffsets();
        prepararGrupo();
    }

    @Test
    void devuelveOffsetsLagDistribucionYTasas() {
        KafkaMetricsResponse respuesta = service.obtener();

        assertEquals(5, respuesta.pollingIntervalSeconds());
        assertEquals(2, respuesta.topics().size());

        assertEquals(NOTIFICACIONES, respuesta.topics().get(0).name());
        assertEquals(3, respuesta.topics().get(0).partitions());
        assertEquals(1, respuesta.topics().get(0).replicationFactor());
        assertEquals(60L, respuesta.topics().get(0).totalMessages());
        assertEquals(3L, respuesta.topics().get(0).failures());
        assertEquals(0.0, respuesta.topics().get(0).eventsPerMinute()); // primera muestra

        assertEquals(1, respuesta.consumerGroups().size());
        assertEquals("biblioteca-notificaciones", respuesta.consumerGroups().get(0).groupId());
        assertEquals(7L, respuesta.consumerGroups().get(0).totalLag());
        assertEquals(7L, respuesta.consumerGroups().get(0).lagByTopic().get(NOTIFICACIONES));

        assertEquals(3, respuesta.distribution().size());
        assertEquals(2L, lagParticion(respuesta, 0));
        assertEquals(5L, lagParticion(respuesta, 1));
        assertEquals(0L, lagParticion(respuesta, 2));
        assertEquals(7L, respuesta.summary().totalLag());
        assertEquals(3L, respuesta.summary().totalFailures());
    }

    private long lagParticion(KafkaMetricsResponse respuesta, int particion) {
        return respuesta.distribution().stream()
                .filter(fila -> fila.topic().equals(NOTIFICACIONES) && fila.partition() == particion)
                .findFirst()
                .orElseThrow()
                .lag();
    }

    private void prepararTopicos() {
        Node nodo = new Node(1, "localhost", 9092);
        Map<String, TopicDescription> descripciones = Map.of(
                NOTIFICACIONES, descripcion(NOTIFICACIONES, nodo),
                PRESTAMOS, descripcion(PRESTAMOS, nodo),
                NOTIFICACIONES + ".DLT", descripcion(NOTIFICACIONES + ".DLT", nodo),
                PRESTAMOS + ".DLT", descripcion(PRESTAMOS + ".DLT", nodo));

        DescribeTopicsResult resultado = mock(DescribeTopicsResult.class);
        when(resultado.allTopicNames()).thenReturn(KafkaFuture.completedFuture(descripciones));
        when(admin.describeTopics(anyCollection())).thenReturn(resultado);
    }

    private TopicDescription descripcion(String nombre, Node nodo) {
        List<TopicPartitionInfo> particiones = List.of(
                new TopicPartitionInfo(0, nodo, List.of(nodo), List.of(nodo)),
                new TopicPartitionInfo(1, nodo, List.of(nodo), List.of(nodo)),
                new TopicPartitionInfo(2, nodo, List.of(nodo), List.of(nodo)));
        return new TopicDescription(nombre, false, particiones);
    }

    private void prepararOffsets() {
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets = new HashMap<>();
        offsets.put(tp(NOTIFICACIONES, 0), info(10L));
        offsets.put(tp(NOTIFICACIONES, 1), info(20L));
        offsets.put(tp(NOTIFICACIONES, 2), info(30L));
        offsets.put(tp(PRESTAMOS, 0), info(5L));
        offsets.put(tp(PRESTAMOS, 1), info(5L));
        offsets.put(tp(PRESTAMOS, 2), info(5L));
        offsets.put(tp(NOTIFICACIONES + ".DLT", 0), info(1L));
        offsets.put(tp(NOTIFICACIONES + ".DLT", 1), info(1L));
        offsets.put(tp(NOTIFICACIONES + ".DLT", 2), info(1L));
        offsets.put(tp(PRESTAMOS + ".DLT", 0), info(0L));
        offsets.put(tp(PRESTAMOS + ".DLT", 1), info(0L));
        offsets.put(tp(PRESTAMOS + ".DLT", 2), info(0L));

        ListOffsetsResult resultado = mock(ListOffsetsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(offsets));
        when(admin.listOffsets(anyMap())).thenReturn(resultado);
    }

    private void prepararGrupo() {
        String groupId = "biblioteca-notificaciones";
        MemberDescription miembro = new MemberDescription(
                "member-1", "cliente-1", "127.0.0.1",
                new MemberAssignment(Set.of(tp(NOTIFICACIONES, 0))));
        ConsumerGroupDescription descripcion = new ConsumerGroupDescription(
                groupId, false, List.of(miembro), "range", ConsumerGroupState.STABLE, Node.noNode());

        DescribeConsumerGroupsResult detalles = mock(DescribeConsumerGroupsResult.class);
        when(detalles.all()).thenReturn(KafkaFuture.completedFuture(Map.of(groupId, descripcion)));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(detalles);

        ListConsumerGroupOffsetsResult offsetsGrupo = mock(ListConsumerGroupOffsetsResult.class);
        when(offsetsGrupo.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of(
                tp(NOTIFICACIONES, 0), new OffsetAndMetadata(8L),
                tp(NOTIFICACIONES, 1), new OffsetAndMetadata(15L),
                tp(NOTIFICACIONES, 2), new OffsetAndMetadata(30L))));
        when(admin.listConsumerGroupOffsets(groupId)).thenReturn(offsetsGrupo);

        ListConsumerGroupsResult listado = mock(ListConsumerGroupsResult.class);
        var listing = mock(org.apache.kafka.clients.admin.ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn(groupId);
        when(listado.valid()).thenReturn(KafkaFuture.completedFuture(List.of(listing)));
        when(admin.listConsumerGroups()).thenReturn(listado);
    }

    private TopicPartition tp(String topic, int partition) {
        return new TopicPartition(topic, partition);
    }

    private ListOffsetsResult.ListOffsetsResultInfo info(long offset) {
        return new ListOffsetsResult.ListOffsetsResultInfo(offset, -1L, Optional.empty());
    }
}
