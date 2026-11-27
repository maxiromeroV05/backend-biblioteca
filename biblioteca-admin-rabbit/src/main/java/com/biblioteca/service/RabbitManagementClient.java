package com.biblioteca.service;

import com.biblioteca.config.MessagingProperties;
import com.biblioteca.exception.ServicioMensajeriaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Cliente de solo lectura de la API HTTP de RabbitMQ (plugin management, puerto 15672).
 *
 * El protocolo AMQP permite crear y borrar colas, exchanges y bindings, pero NO
 * listarlos; para los listados se usa esta API.
 */
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final String baseUrl;
    private final String vhost;

    public RabbitManagementClient(MessagingProperties props,
                                  @Value("${spring.rabbitmq.username}") String usuario,
                                  @Value("${spring.rabbitmq.password}") String clave,
                                  @Value("${app.rabbit.vhost:/}") String vhost) {
        String url = props.management().url();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        // El vhost "/" viaja codificado como %2F en la URL.
        this.vhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8);
        this.restClient = RestClient.builder()
                .defaultHeaders(headers -> headers.setBasicAuth(usuario, clave))
                .build();
    }

    public List<Map<String, Object>> colas() {
        return obtener("queues");
    }

    public List<Map<String, Object>> exchanges() {
        return obtener("exchanges");
    }

    public List<Map<String, Object>> bindings() {
        return obtener("bindings");
    }

    private List<Map<String, Object>> obtener(String recurso) {
        try {
            List<Map<String, Object>> respuesta = restClient.get()
                    .uri(URI.create(baseUrl + "/api/" + recurso + "/" + vhost))
                    .retrieve()
                    .body(LISTA);
            return respuesta != null ? respuesta : List.of();
        } catch (RestClientException e) {
            throw new ServicioMensajeriaException(
                    "No se pudo consultar la API de administración de RabbitMQ en " + baseUrl, e);
        }
    }
}
