package com.biblioteca.service;

import com.biblioteca.config.KafkaTopicsProperties;
import com.biblioteca.dto.kafka.ConsumerGroupMemberResponse;
import com.biblioteca.dto.kafka.ConsumerGroupResponse;
import com.biblioteca.dto.kafka.PublicarEventoKafkaRequest;
import com.biblioteca.dto.kafka.PublicarKafkaRequest;
import com.biblioteca.dto.kafka.TopicRequest;
import com.biblioteca.dto.kafka.TopicResponse;
import com.biblioteca.dto.kafka.TopicUpdateRequest;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.exception.ServicioMensajeriaException;
import com.biblioteca.exception.SolicitudInvalidaException;
import com.biblioteca.messaging.kafka.KafkaAdminPublisher;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Administración de Kafka: crear, listar, modificar y eliminar tópicos y sus
 * configuraciones. Usa el cliente Admin de Kafka contra el mismo clúster que
 * el resto de la aplicación (spring.kafka.bootstrap-servers).
 *
 * Cada operación abre y cierra su propio cliente Admin, con timeouts cortos:
 * si Kafka no responde, la API contesta 503 en pocos segundos.
 */
@Service
public class KafkaAdminService {

    private static final long ESPERA_SEGUNDOS = 10;
    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final KafkaAdmin kafkaAdmin;
    private final KafkaTopicsProperties props;
    private final KafkaAdminPublisher publisher;

    public KafkaAdminService(KafkaAdmin kafkaAdmin,
                             KafkaTopicsProperties props,
                             KafkaAdminPublisher publisher) {
        this.kafkaAdmin = kafkaAdmin;
        this.props = props;
        this.publisher = publisher;
    }

    // ----------------------------- Tópicos -----------------------------

    /** Lista los tópicos del clúster (sin los internos como __consumer_offsets). */
    public List<TopicResponse> listarTopicos() {
        try (Admin admin = crearCliente()) {
            Set<String> nombres = admin.listTopics(new ListTopicsOptions().listInternal(false))
                    .names().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (nombres.isEmpty()) {
                return List.of();
            }
            Map<String, TopicDescription> descripciones = admin.describeTopics(nombres)
                    .allTopicNames().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);

            return descripciones.values().stream()
                    .sorted(Comparator.comparing(TopicDescription::name))
                    .map(d -> aRespuesta(d, Map.of()))
                    .toList();
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    /** Detalle de un tópico, incluidas sus configuraciones propias. */
    public TopicResponse obtenerTopico(String nombre) {
        try (Admin admin = crearCliente()) {
            return describir(admin, nombre);
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    public TopicResponse crearTopico(TopicRequest request) {
        int particiones = request.partitions() != null
                ? request.partitions() : props.topicDefaults().partitions();
        int replicas = request.replicationFactor() != null
                ? request.replicationFactor() : props.topicDefaults().replicas();
        Map<String, String> configs = request.configs() != null ? request.configs() : Map.of();

        NewTopic nuevo = new NewTopic(request.name(), particiones, (short) replicas);
        if (!configs.isEmpty()) {
            nuevo.configs(configs);
        }

        try (Admin admin = crearCliente()) {
            admin.createTopics(List.of(nuevo)).all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw traducir(e);
        }
        return new TopicResponse(request.name(), particiones, replicas, new LinkedHashMap<>(configs));
    }

    /**
     * Modifica un tópico: aumenta sus particiones y/o cambia sus configuraciones.
     * El factor de replicación no se puede cambiar con esta operación.
     */
    public TopicResponse actualizarTopico(String nombre, TopicUpdateRequest request) {
        validarNoInterno(nombre);
        boolean sinParticiones = request.partitions() == null;
        boolean sinConfigs = request.configs() == null || request.configs().isEmpty();
        if (sinParticiones && sinConfigs) {
            throw new SolicitudInvalidaException("Indica las particiones o las configuraciones a modificar");
        }

        try (Admin admin = crearCliente()) {
            TopicDescription actual = admin.describeTopics(List.of(nombre))
                    .allTopicNames().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(nombre);
            int particionesActuales = actual.partitions().size();

            if (!sinParticiones) {
                int pedidas = request.partitions();
                if (pedidas < particionesActuales) {
                    throw new SolicitudInvalidaException("Kafka solo permite aumentar las particiones: el tópico tiene "
                            + particionesActuales + " y se pidieron " + pedidas);
                }
                if (pedidas > particionesActuales) {
                    admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(pedidas)))
                            .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
                }
            }

            if (!sinConfigs) {
                ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
                Collection<AlterConfigOp> operaciones = new ArrayList<>();
                for (Map.Entry<String, String> entrada : request.configs().entrySet()) {
                    String valor = entrada.getValue();
                    if (valor == null || valor.isBlank()) {
                        // Sin valor: se elimina el override y vuelve el valor por defecto.
                        operaciones.add(new AlterConfigOp(new ConfigEntry(entrada.getKey(), null),
                                AlterConfigOp.OpType.DELETE));
                    } else {
                        operaciones.add(new AlterConfigOp(new ConfigEntry(entrada.getKey(), valor),
                                AlterConfigOp.OpType.SET));
                    }
                }
                Map<ConfigResource, Collection<AlterConfigOp>> cambios = new HashMap<>();
                cambios.put(recurso, operaciones);
                admin.incrementalAlterConfigs(cambios).all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            }

            // Los metadatos de particiones pueden tardar en propagarse: si se
            // aumentaron, se informa el valor pedido en vez del que aún se ve.
            TopicResponse resultado = describir(admin, nombre);
            int particionesFinales = Math.max(resultado.partitions(),
                    sinParticiones ? 0 : request.partitions());
            return new TopicResponse(resultado.name(), particionesFinales,
                    resultado.replicationFactor(), resultado.configs());
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    public void eliminarTopico(String nombre) {
        validarNoInterno(nombre);
        try (Admin admin = crearCliente()) {
            admin.deleteTopics(List.of(nombre)).all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    // ---------------------------- Publicación ----------------------------

    public Notificacion publicar(PublicarKafkaRequest request) {
        return publisher.publicar(request.topic(), request.contenido());
    }

    /** Publica el DTO completo (mismo cuerpo que viaja por Kafka). */
    public Notificacion publicarEvento(PublicarEventoKafkaRequest request) {
        return publisher.publicar(request.topic(), request.evento());
    }

    public Notificacion publicarInvalido() {
        return publisher.publicarInvalido();
    }

    // ------------------------- Consumer groups -------------------------

    /** Lista los consumer groups válidos del clúster con su estado y sus miembros. */
    public List<ConsumerGroupResponse> listarGrupos() {
        try (Admin admin = crearCliente()) {
            Collection<ConsumerGroupListing> grupos = admin.listConsumerGroups()
                    .valid().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (grupos.isEmpty()) {
                return List.of();
            }
            List<String> ids = grupos.stream().map(ConsumerGroupListing::groupId).sorted().toList();
            Map<String, ConsumerGroupDescription> descripciones = admin.describeConsumerGroups(ids)
                    .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            List<ConsumerGroupResponse> respuesta = new ArrayList<>();
            for (ConsumerGroupDescription descripcion : descripciones.values()) {
                ConsumerGroupResponse item = aRespuesta(admin, descripcion);
                if (item != null) {
                    respuesta.add(item);
                }
            }
            return respuesta.stream()
                    .sorted(Comparator.comparing(ConsumerGroupResponse::groupId))
                    .toList();
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    /** Detalle de un consumer group (miembros, particiones asignadas y tópicos). */
    public ConsumerGroupResponse obtenerGrupo(String groupId) {
        try (Admin admin = crearCliente()) {
            ConsumerGroupResponse respuesta = aRespuesta(admin, admin.describeConsumerGroups(List.of(groupId))
                    .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(groupId));
            if (respuesta == null) {
                throw new RecursoNoEncontradoException("El consumer group no existe");
            }
            return respuesta;
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    private ConsumerGroupResponse aRespuesta(Admin admin, ConsumerGroupDescription descripcion) throws Exception {
        if (descripcion == null) {
            return null;
        }
        List<ConsumerGroupMemberResponse> miembros = descripcion.members().stream()
                .map(this::aRespuesta)
                .sorted(Comparator.comparing(ConsumerGroupMemberResponse::memberId))
                .toList();

        Set<String> topics = new TreeSet<>();
        miembros.forEach(m -> m.assignedPartitions().forEach(p -> topics.add(p.substring(0, p.lastIndexOf('-')))));
        try {
            Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(descripcion.groupId())
                    .partitionsToOffsetAndMetadata().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (offsets != null) {
                offsets.keySet().forEach(tp -> topics.add(tp.topic()));
            }
        } catch (Exception e) {
            // Un grupo sin offsets confirmados (recién creado) sigue siendo inspeccionable.
            log.debug("No se pudieron leer los offsets del grupo {}: {}", descripcion.groupId(), e.getMessage());
        }

        return new ConsumerGroupResponse(
                descripcion.groupId(),
                descripcion.groupState().name(),
                List.copyOf(topics),
                miembros);
    }

    private ConsumerGroupMemberResponse aRespuesta(MemberDescription miembro) {
        List<String> particiones = miembro.assignment().topicPartitions().stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .sorted()
                .toList();
        return new ConsumerGroupMemberResponse(
                miembro.consumerId(), miembro.clientId(), miembro.host(), particiones);
    }

    // ------------------------------ Internos ------------------------------

    /** Cliente Admin nuevo con timeouts cortos. Protegido para poder sustituirlo en los tests. */
    protected Admin crearCliente() {
        Map<String, Object> configuracion = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        configuracion.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configuracion.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 8000);
        return Admin.create(configuracion);
    }

    private TopicResponse describir(Admin admin, String nombre) throws Exception {
        TopicDescription descripcion = admin.describeTopics(List.of(nombre))
                .allTopicNames().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(nombre);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = admin.describeConfigs(List.of(recurso))
                .all().get(ESPERA_SEGUNDOS, TimeUnit.SECONDS).get(recurso);

        // Solo las configuraciones fijadas en el propio tópico (no las heredadas del broker).
        Map<String, String> propias = new LinkedHashMap<>();
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                if (entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                    propias.put(entrada.name(), entrada.value());
                }
            }
        }
        return aRespuesta(descripcion, propias);
    }

    private TopicResponse aRespuesta(TopicDescription descripcion, Map<String, String> configs) {
        int replicas = descripcion.partitions().isEmpty()
                ? 0 : descripcion.partitions().get(0).replicas().size();
        return new TopicResponse(descripcion.name(), descripcion.partitions().size(), replicas, configs);
    }

    private void validarNoInterno(String nombre) {
        if (nombre.startsWith("__")) {
            throw new SolicitudInvalidaException("Los tópicos internos de Kafka (" + nombre + ") no se pueden modificar");
        }
    }

    /** Convierte los errores del cliente Kafka en las excepciones de la API (404, 409, 400, 503). */
    private RuntimeException traducir(Exception e) {
        if (e instanceof SolicitudInvalidaException
                || e instanceof RecursoNoEncontradoException
                || e instanceof RegistroDuplicadoException) {
            return (RuntimeException) e;
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        Throwable causa = (e instanceof ExecutionException && e.getCause() != null) ? e.getCause() : e;

        if (causa instanceof UnknownTopicOrPartitionException) {
            return new RecursoNoEncontradoException("El tópico no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new RegistroDuplicadoException("Ya existe un tópico con ese nombre");
        }
        if (causa instanceof InvalidTopicException
                || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException) {
            return new SolicitudInvalidaException(causa.getMessage());
        }
        return new ServicioMensajeriaException(
                "No se pudo completar la operación en Kafka: " + causa.getMessage(), causa);
    }
}
