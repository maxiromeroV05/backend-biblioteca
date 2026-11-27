package com.biblioteca.controller;

import com.biblioteca.dto.NotificacionResponse;
import com.biblioteca.dto.kafka.NotificacionKafkaRequest;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.messaging.kafka.KafkaEventoPublisher;
import com.biblioteca.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final KafkaEventoPublisher kafkaEventoPublisher;

    public NotificacionController(NotificacionService notificacionService,
                                  KafkaEventoPublisher kafkaEventoPublisher) {
        this.notificacionService = notificacionService;
        this.kafkaEventoPublisher = kafkaEventoPublisher;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar() {
        List<NotificacionResponse> respuesta = notificacionService.listarTodas().stream()
                .map(NotificacionResponse::desdeEntidad)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Publica una notificación en el tópico de negocio biblioteca.notificaciones.
     * Es el productor que da sentido al segundo tópico además del endpoint de admin.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> publicar(@Valid @RequestBody NotificacionKafkaRequest request) {
        Notificacion notificacion = kafkaEventoPublisher.publicarNotificacion(
                request.destinatario(), request.contenido());

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Notificación publicada en Kafka");
        cuerpo.put("id", notificacion.getMensajeId());
        cuerpo.put("estado", notificacion.getEstado().name());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cuerpo);
    }
}
