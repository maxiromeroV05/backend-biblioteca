package com.biblioteca.service;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.dto.kafka.ConsumerGroupResponse;
import com.biblioteca.dto.kafka.PublicarEventoKafkaRequest;
import com.biblioteca.dto.kafka.TopicRequest;
import com.biblioteca.dto.kafka.TopicResponse;
import com.biblioteca.dto.kafka.TopicUpdateRequest;
import com.biblioteca.messaging.event.EventoMensajeria;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.messaging.kafka.KafkaAdminPublisher;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.exception.SolicitudInvalidaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del administrador de tópicos y consumer groups con un cliente Admin
 * simulado (no necesita un clúster Kafka levantado).
 */
class KafkaAdminServiceTest {

    private Admin admin;
    private KafkaAdminPublisher publisher;
    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        admin = mock(Admin.class);

        KafkaTopicsProperties props = new KafkaTopicsProperties(
                true, ".DLT", 2,
                new KafkaTopicsProperties.Topics("biblioteca.notificaciones", "biblioteca.prestamos"),
                new KafkaTopicsProperties.TopicDefaults(3, 3),
                Map.of("retention.ms", "604800000", "cleanup.policy", "delete"),
                Map.of("retention.ms", "86400000", "cleanup.policy", "delete"),
                new KafkaTopicsProperties.Retry(3, 10),
                new KafkaTopicsProperties.Groups("g-not", "g-prest", "g-dlt"),
                new KafkaTopicsProperties.Listener(3));

        // Se sustituyen el cliente Admin y el publicador reales por simulados.
        publisher = mock(KafkaAdminPublisher.class);
        service = new KafkaAdminService(null, props, publisher) {
            @Override
            protected Admin crearCliente() {
                return admin;
            }
        };
    }

    @Test
    void crearTopicoUsaLosValoresPorDefectoSiNoSeIndican() {
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(anyCollection())).thenReturn(resultado);

        TopicResponse respuesta = service.crearTopico(new TopicRequest("mi.topico", null, null, null));

        assertEquals("mi.topico", respuesta.name());
        assertEquals(3, respuesta.partitions());
        assertEquals(3, respuesta.replicationFactor());
        verify(admin).createTopics(anyCollection());
    }

    @Test
    void crearTopicoExistenteDevuelveConflicto() {
        KafkaFutureImpl<Void> falla = new KafkaFutureImpl<>();
        falla.completeExceptionally(new TopicExistsException("ya existe"));
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(falla);
        when(admin.createTopics(anyCollection())).thenReturn(resultado);

        assertThrows(RegistroDuplicadoException.class,
                () -> service.crearTopico(new TopicRequest("mi.topico", 1, 1, null)));
    }

    @Test
    void noSePermiteReducirParticiones() {
        Node nodo = new Node(1, "localhost", 9092);
        List<TopicPartitionInfo> particiones = List.of(
                new TopicPartitionInfo(0, nodo, List.of(nodo), List.of(nodo)),
                new TopicPartitionInfo(1, nodo, List.of(nodo), List.of(nodo)),
                new TopicPartitionInfo(2, nodo, List.of(nodo), List.of(nodo)));
        TopicDescription descripcion = new TopicDescription("mi.topico", false, particiones);

        DescribeTopicsResult resultado = mock(DescribeTopicsResult.class);
        when(resultado.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("mi.topico", descripcion)));
        when(admin.describeTopics(anyCollection())).thenReturn(resultado);

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> service.actualizarTopico("mi.topico", new TopicUpdateRequest(2, null)));

        assertEquals(true, error.getMessage().contains("solo permite aumentar"));
    }

    @Test
    void actualizarSinCambiosEsInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.actualizarTopico("mi.topico", new TopicUpdateRequest(null, null)));
    }

    @Test
    void losTopicosInternosNoSePuedenEliminar() {
        assertThrows(SolicitudInvalidaException.class, () -> service.eliminarTopico("__consumer_offsets"));
    }

    @Test
    void listaConsumerGroupsConSusTopicos() {
        prepararGrupo("biblioteca-notificaciones");
        ListConsumerGroupsResult listado = mock(ListConsumerGroupsResult.class);
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn("biblioteca-notificaciones");
        when(listado.valid()).thenReturn(KafkaFuture.completedFuture(List.of(listing)));
        when(admin.listConsumerGroups()).thenReturn(listado);

        List<ConsumerGroupResponse> grupos = service.listarGrupos();

        assertEquals(1, grupos.size());
        assertEquals("biblioteca-notificaciones", grupos.get(0).groupId());
        assertEquals(List.of("biblioteca.notificaciones"), grupos.get(0).topics());
        assertEquals(1, grupos.get(0).members().size());
        assertEquals(List.of("biblioteca.notificaciones-0"),
                grupos.get(0).members().get(0).assignedPartitions());
    }

    @Test
    void detalleDeConsumerGroupInexistenteDevuelve404() {
        when(admin.describeConsumerGroups(anyCollection()))
                .thenReturn(new DescribeConsumerGroupsResult(Map.of()));

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerGrupo("no-existe"));
    }

    @Test
    void publicarEventoDelegaElDtoCompletoEnElPublicador() {
        EventoMensajeria evento = new EventoMensajeria(
                "msg-1", EventoTipo.PRESTAMO_CREADO, "ana@correo.cl",
                "Préstamo creado", "2026-11-27T10:00:00Z", "corr-123", "ana@correo.cl");
        PublicarEventoKafkaRequest request =
                new PublicarEventoKafkaRequest("biblioteca.prestamos", evento);

        service.publicarEvento(request);

        verify(publisher).publicar("biblioteca.prestamos", evento);
    }

    private void prepararGrupo(String groupId) {
        TopicPartition particion = new TopicPartition("biblioteca.notificaciones", 0);
        MemberDescription miembro = new MemberDescription(
                "member-1", "cliente-1", "127.0.0.1",
                new MemberAssignment(Set.of(particion)));
        ConsumerGroupDescription descripcion = new ConsumerGroupDescription(
                groupId, false, List.of(miembro), "range", ConsumerGroupState.STABLE, Node.noNode());
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(
                new DescribeConsumerGroupsResult(Map.of(groupId, KafkaFuture.completedFuture(descripcion))));

        ListConsumerGroupOffsetsResult offsets = mock(ListConsumerGroupOffsetsResult.class);
        when(offsets.partitionsToOffsetAndMetadata())
                .thenReturn(KafkaFuture.completedFuture(Map.of(particion, new OffsetAndMetadata(12L))));
        when(admin.listConsumerGroupOffsets(groupId)).thenReturn(offsets);
    }
}
