package com.biblioteca.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura común de mensajería.
 *
 * Spring Boot detecta este MessageConverter y lo aplica automáticamente al
 * RabbitTemplate (productores) y a los @RabbitListener (consumidores), así los
 * mensajes viajan como JSON. El RabbitAdmin y la fábrica de listeners los crea
 * Boot a partir de spring.rabbitmq.* en application.yml.
 */
@Configuration
public class RabbitInfrastructureConfig {

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
