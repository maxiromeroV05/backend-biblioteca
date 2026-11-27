package com.biblioteca.service;

import com.biblioteca.dto.rabbit.BindingRequest;
import com.biblioteca.dto.rabbit.ColaRequest;
import com.biblioteca.dto.rabbit.ColaResponse;
import com.biblioteca.dto.rabbit.ExchangeRequest;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.exception.SolicitudInvalidaException;
import com.biblioteca.messaging.producer.RabbitAdminPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del administrador de RabbitMQ (crear y eliminar colas, exchanges y bindings)
 * con AmqpAdmin y el cliente de management simulados: no necesitan RabbitMQ levantado.
 */
class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitManagementClient management;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        management = mock(RabbitManagementClient.class);
        service = new RabbitAdminService(amqpAdmin, management, mock(RabbitAdminPublisher.class));
    }

    /** Simula que el exchange existe en RabbitMQ (lo consulta la API de management). */
    private void exchangeExiste(String nombre) {
        when(management.exchanges()).thenReturn(List.of(Map.<String, Object>of("name", nombre, "type", "direct")));
    }

    // ------------------------------- Colas -------------------------------

    @Test
    void crearColaLaDeclaraEnRabbit() {
        ColaResponse respuesta = service.crearCola(new ColaRequest("reportes.queue", true, null, null, null));

        assertEquals("reportes.queue", respuesta.getName());
        assertTrue(respuesta.isDurable());
        verify(amqpAdmin).declareQueue(any(Queue.class));
    }

    @Test
    void crearColaExistenteDevuelveConflicto() {
        when(amqpAdmin.getQueueProperties("reportes.queue")).thenReturn(new Properties());

        assertThrows(RegistroDuplicadoException.class,
                () -> service.crearCola(new ColaRequest("reportes.queue", true, null, null, null)));

        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void eliminarColaExistenteLaBorra() {
        when(amqpAdmin.getQueueProperties("reportes.queue")).thenReturn(new Properties());

        service.eliminarCola("reportes.queue");

        verify(amqpAdmin).deleteQueue("reportes.queue");
    }

    @Test
    void eliminarColaInexistenteDevuelve404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarCola("no.existe"));

        verify(amqpAdmin, never()).deleteQueue("no.existe");
    }

    // ----------------------------- Exchanges -----------------------------

    @Test
    void crearExchangeLoDeclaraEnRabbit() {
        ExchangeRequest respuesta = service.crearExchange(new ExchangeRequest("eventos.topic", "topic", true, false));

        assertEquals("eventos.topic", respuesta.getName());
        assertEquals("topic", respuesta.getType());
        verify(amqpAdmin).declareExchange(any(Exchange.class));
    }

    @Test
    void crearExchangeConTipoInvalidoEsRechazado() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.crearExchange(new ExchangeRequest("eventos.x", "otro", true, false)));

        verify(amqpAdmin, never()).declareExchange(any(Exchange.class));
    }

    @Test
    void eliminarExchangeExistenteLoBorra() {
        exchangeExiste("eventos.topic");

        service.eliminarExchange("eventos.topic");

        verify(amqpAdmin).deleteExchange("eventos.topic");
    }

    @Test
    void losExchangesInternosNoSePuedenEliminar() {
        assertThrows(SolicitudInvalidaException.class, () -> service.eliminarExchange("amq.direct"));

        verify(amqpAdmin, never()).deleteExchange("amq.direct");
    }

    // ------------------------------ Bindings ------------------------------

    @Test
    void crearBindingEntreExchangeYColaExistentes() {
        exchangeExiste("eventos.topic");
        when(amqpAdmin.getQueueProperties("reportes.queue")).thenReturn(new Properties());

        BindingRequest respuesta = service.crearBinding(new BindingRequest("eventos.topic", "reportes.queue", "reporte.#"));

        assertEquals("reporte.#", respuesta.getRoutingKey());
        verify(amqpAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void crearBindingConColaInexistenteDevuelve404() {
        exchangeExiste("eventos.topic");

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearBinding(new BindingRequest("eventos.topic", "no.existe", "reporte.#")));

        verify(amqpAdmin, never()).declareBinding(any(Binding.class));
    }

    @Test
    void eliminarBindingExistenteLoBorra() {
        exchangeExiste("eventos.topic");
        when(amqpAdmin.getQueueProperties("reportes.queue")).thenReturn(new Properties());
        when(management.bindings()).thenReturn(List.of(Map.<String, Object>of(
                "source", "eventos.topic",
                "destination", "reportes.queue",
                "destination_type", "queue",
                "routing_key", "reporte.#")));

        service.eliminarBinding(new BindingRequest("eventos.topic", "reportes.queue", "reporte.#"));

        verify(amqpAdmin).removeBinding(any(Binding.class));
    }
}
