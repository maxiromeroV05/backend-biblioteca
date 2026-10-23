package com.biblioteca.service;

import com.biblioteca.dto.rabbit.BindingRequest;
import com.biblioteca.dto.rabbit.ColaRequest;
import com.biblioteca.dto.rabbit.ColaResponse;
import com.biblioteca.dto.rabbit.ExchangeRequest;
import com.biblioteca.dto.rabbit.PublicarRequest;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.exception.SolicitudInvalidaException;
import com.biblioteca.messaging.producer.EventoPublisher;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Servicio de administración de RabbitMQ. Concentra TODA la lógica de crear,
 * listar y eliminar colas, exchanges y bindings; los controladores solo llaman
 * a estos métodos y no conocen la librería de RabbitMQ.
 *
 *  - Crear y eliminar: AMQP (AmqpAdmin).
 *  - Listar: API HTTP de management (RabbitManagementClient).
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementClient management;
    private final EventoPublisher publisher;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
                              RabbitManagementClient management,
                              EventoPublisher publisher) {
        this.amqpAdmin = amqpAdmin;
        this.management = management;
        this.publisher = publisher;
    }

    // ------------------------------------------------------------------
    // Colas
    // ------------------------------------------------------------------

    public List<ColaResponse> listarColas() {
        return management.colas().stream()
                .map(this::aColaResponse)
                .toList();
    }

    public ColaResponse crearCola(ColaRequest request) {
        boolean tieneDlx = hayTexto(request.getDeadLetterExchange());
        boolean tieneDlk = hayTexto(request.getDeadLetterRoutingKey());

        if (tieneDlk && !tieneDlx) {
            throw new SolicitudInvalidaException(
                    "No se puede indicar una dead letter routing key sin un dead letter exchange");
        }
        if (tieneDlx && !existeExchange(request.getDeadLetterExchange())) {
            throw new SolicitudInvalidaException(
                    "El dead letter exchange '" + request.getDeadLetterExchange() + "' no existe");
        }
        if (existeCola(request.getName())) {
            throw new RegistroDuplicadoException("Ya existe una cola llamada '" + request.getName() + "'");
        }

        boolean durable = !Boolean.FALSE.equals(request.getDurable());
        QueueBuilder builder = durable
                ? QueueBuilder.durable(request.getName())
                : QueueBuilder.nonDurable(request.getName());

        if (tieneDlx) {
            builder.deadLetterExchange(request.getDeadLetterExchange());
        }
        if (tieneDlk) {
            builder.deadLetterRoutingKey(request.getDeadLetterRoutingKey());
        }
        if (request.getMessageTtl() != null) {
            builder.ttl(request.getMessageTtl().intValue());
        }

        Queue cola = builder.build();
        amqpAdmin.declareQueue(cola);

        return new ColaResponse(
                request.getName(),
                durable,
                tieneDlx ? request.getDeadLetterExchange() : null,
                tieneDlk ? request.getDeadLetterRoutingKey() : null,
                request.getMessageTtl(),
                0L,
                0L);
    }

    public void eliminarCola(String nombre) {
        if (!existeCola(nombre)) {
            throw new RecursoNoEncontradoException("No existe la cola '" + nombre + "'");
        }
        amqpAdmin.deleteQueue(nombre);
    }

    // ------------------------------------------------------------------
    // Exchanges
    // ------------------------------------------------------------------

    public List<ExchangeRequest> listarExchanges() {
        return management.exchanges().stream()
                .filter(e -> {
                    String nombre = texto(e.get("name"));
                    // Se omiten el exchange por defecto ("") y los internos (amq.*).
                    return nombre != null && !nombre.isEmpty() && !nombre.startsWith("amq.");
                })
                .map(e -> new ExchangeRequest(
                        texto(e.get("name")),
                        texto(e.get("type")),
                        Boolean.TRUE.equals(e.get("durable")),
                        Boolean.TRUE.equals(e.get("auto_delete"))))
                .toList();
    }

    public ExchangeRequest crearExchange(ExchangeRequest request) {
        if (existeExchange(request.getName())) {
            throw new RegistroDuplicadoException("Ya existe un exchange llamado '" + request.getName() + "'");
        }

        boolean durable = !Boolean.FALSE.equals(request.getDurable());
        boolean autoDelete = Boolean.TRUE.equals(request.getAutoDelete());

        Exchange exchange = switch (request.getType()) {
            case "direct" -> new DirectExchange(request.getName(), durable, autoDelete);
            case "topic" -> new TopicExchange(request.getName(), durable, autoDelete);
            case "fanout" -> new FanoutExchange(request.getName(), durable, autoDelete);
            case "headers" -> new HeadersExchange(request.getName(), durable, autoDelete);
            default -> throw new SolicitudInvalidaException(
                    "Tipo de exchange no soportado: " + request.getType());
        };
        amqpAdmin.declareExchange(exchange);

        return new ExchangeRequest(request.getName(), request.getType(), durable, autoDelete);
    }

    public void eliminarExchange(String nombre) {
        if (nombre.startsWith("amq.")) {
            throw new SolicitudInvalidaException("No se pueden eliminar los exchanges internos (amq.*)");
        }
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("No existe el exchange '" + nombre + "'");
        }
        amqpAdmin.deleteExchange(nombre);
    }

    // ------------------------------------------------------------------
    // Bindings
    // ------------------------------------------------------------------

    public List<BindingRequest> listarBindings() {
        return management.bindings().stream()
                .filter(b -> "queue".equals(texto(b.get("destination_type")))
                        && !texto(b.get("source")).isEmpty())
                .map(b -> new BindingRequest(
                        texto(b.get("source")),
                        texto(b.get("destination")),
                        texto(b.get("routing_key"))))
                .toList();
    }

    public BindingRequest crearBinding(BindingRequest request) {
        validarExtremosDelBinding(request);
        amqpAdmin.declareBinding(aBinding(request));
        return request;
    }

    public void eliminarBinding(BindingRequest request) {
        validarExtremosDelBinding(request);

        boolean existe = listarBindings().stream().anyMatch(b ->
                b.getExchange().equals(request.getExchange())
                        && b.getQueue().equals(request.getQueue())
                        && b.getRoutingKey().equals(request.getRoutingKey()));
        if (!existe) {
            throw new RecursoNoEncontradoException("No existe ese binding entre '"
                    + request.getExchange() + "' y '" + request.getQueue()
                    + "' con routing key '" + request.getRoutingKey() + "'");
        }
        amqpAdmin.removeBinding(aBinding(request));
    }

    // ------------------------------------------------------------------
    // Publicación de prueba (panel de mensajería)
    // ------------------------------------------------------------------

    public Notificacion publicar(PublicarRequest request) {
        if (!existeExchange(request.getExchange())) {
            throw new RecursoNoEncontradoException("No existe el exchange '" + request.getExchange() + "'");
        }
        return publisher.publicarLibre(request.getExchange(), request.getRoutingKey(), request.getPayload());
    }

    /** Publica un mensaje con contenido vacío para demostrar el paso a la DLQ. */
    public Notificacion publicarInvalido() {
        return publisher.publicarMensajeInvalido();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void validarExtremosDelBinding(BindingRequest request) {
        if (!existeExchange(request.getExchange())) {
            throw new RecursoNoEncontradoException("No existe el exchange '" + request.getExchange() + "'");
        }
        if (!existeCola(request.getQueue())) {
            throw new RecursoNoEncontradoException("No existe la cola '" + request.getQueue() + "'");
        }
    }

    private Binding aBinding(BindingRequest request) {
        return new Binding(request.getQueue(), Binding.DestinationType.QUEUE,
                request.getExchange(), request.getRoutingKey(), null);
    }

    private boolean existeCola(String nombre) {
        return amqpAdmin.getQueueProperties(nombre) != null;
    }

    private boolean existeExchange(String nombre) {
        return management.exchanges().stream().anyMatch(e -> nombre.equals(texto(e.get("name"))));
    }

    private ColaResponse aColaResponse(Map<String, Object> cola) {
        Map<String, Object> argumentos = cola.get("arguments") instanceof Map<?, ?> mapa
                ? castMapa(mapa)
                : Map.of();

        Object ttl = argumentos.get("x-message-ttl");
        return new ColaResponse(
                texto(cola.get("name")),
                Boolean.TRUE.equals(cola.get("durable")),
                texto(argumentos.get("x-dead-letter-exchange")),
                texto(argumentos.get("x-dead-letter-routing-key")),
                ttl instanceof Number n ? n.longValue() : null,
                numero(cola.get("messages")),
                numero(cola.get("consumers")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMapa(Map<?, ?> mapa) {
        return (Map<String, Object>) mapa;
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    private static long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }
}
