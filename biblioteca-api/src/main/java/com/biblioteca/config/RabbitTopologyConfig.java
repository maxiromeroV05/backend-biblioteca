package com.biblioteca.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la topología de RabbitMQ: exchanges, colas, DLQ y bindings.
 *
 * Esta clase SOLO configura; no contiene lógica de negocio.
 * Los nombres vienen de application.yml (ver MessagingProperties).
 *
 * Rutas de mensajería:
 *
 *   biblioteca.topic  --notificacion.#-->     notificaciones.queue --(rechazo)--> biblioteca.dlx --> notificaciones.dlq
 *   biblioteca.direct --ticket.crear-->       tickets.queue        --(rechazo)--> biblioteca.dlx --> tickets.dlq
 *   biblioteca.direct --documento.generar-->  documentos.queue     --(rechazo)--> biblioteca.dlx --> documentos.dlq
 *
 * Spring (RabbitAdmin) crea todo esto en RabbitMQ al conectarse.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitTopologyConfig {

    private final MessagingProperties props;

    public RabbitTopologyConfig(MessagingProperties props) {
        this.props = props;
    }

    // ------------------------------------------------------------------
    // Exchanges
    // ------------------------------------------------------------------

    /** Exchange DIRECT: enruta por routing key exacta (tickets y documentos). */
    @Bean
    public DirectExchange bibliotecaDirectExchange() {
        return new DirectExchange(props.exchanges().direct(), true, false);
    }

    /** Exchange TOPIC: enruta por patrón de routing key (notificaciones). */
    @Bean
    public TopicExchange bibliotecaTopicExchange() {
        return new TopicExchange(props.exchanges().topic(), true, false);
    }

    /** Dead letter exchange: recibe los mensajes rechazados de las 3 colas. */
    @Bean
    public DirectExchange bibliotecaDlxExchange() {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }

    // ------------------------------------------------------------------
    // Caso de uso 1: notificaciones
    // ------------------------------------------------------------------

    @Bean
    public Queue notificacionesQueue() {
        return colaConDlq(props.queues().notificaciones());
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.queues().notificaciones().dlq()).build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder
                .bind(notificacionesQueue())
                .to(bibliotecaTopicExchange())
                .with(props.queues().notificaciones().routingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder
                .bind(notificacionesDlq())
                .to(bibliotecaDlxExchange())
                .with(props.queues().notificaciones().dlqRoutingKey());
    }

    // ------------------------------------------------------------------
    // Caso de uso 2: tickets
    // ------------------------------------------------------------------

    @Bean
    public Queue ticketsQueue() {
        return colaConDlq(props.queues().tickets());
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.queues().tickets().dlq()).build();
    }

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder
                .bind(ticketsQueue())
                .to(bibliotecaDirectExchange())
                .with(props.queues().tickets().routingKey());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder
                .bind(ticketsDlq())
                .to(bibliotecaDlxExchange())
                .with(props.queues().tickets().dlqRoutingKey());
    }

    // ------------------------------------------------------------------
    // Caso de uso 3: documentos
    // ------------------------------------------------------------------

    @Bean
    public Queue documentosQueue() {
        return colaConDlq(props.queues().documentos());
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(props.queues().documentos().dlq()).build();
    }

    @Bean
    public Binding documentosBinding() {
        return BindingBuilder
                .bind(documentosQueue())
                .to(bibliotecaDirectExchange())
                .with(props.queues().documentos().routingKey());
    }

    @Bean
    public Binding documentosDlqBinding() {
        return BindingBuilder
                .bind(documentosDlq())
                .to(bibliotecaDlxExchange())
                .with(props.queues().documentos().dlqRoutingKey());
    }

    // ------------------------------------------------------------------

    /** Cola durable cuyos mensajes rechazados van al DLX con la routing key de su DLQ. */
    private Queue colaConDlq(MessagingProperties.QueueDef def) {
        return QueueBuilder
                .durable(def.name())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(def.dlqRoutingKey())
                .build();
    }
}
