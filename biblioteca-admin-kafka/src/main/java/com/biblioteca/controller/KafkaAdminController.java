package com.biblioteca.controller;

import com.biblioteca.dto.kafka.ConsumerGroupResponse;
import com.biblioteca.dto.kafka.PublicarEventoKafkaRequest;
import com.biblioteca.dto.kafka.PublicarKafkaRequest;
import com.biblioteca.dto.kafka.TopicRequest;
import com.biblioteca.dto.kafka.TopicResponse;
import com.biblioteca.dto.kafka.TopicUpdateRequest;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST de administración de Kafka. Toda la lógica vive en
 * KafkaAdminService; aquí solo se reciben y validan las peticiones.
 *
 *   GET    /api/kafka/topics            lista los tópicos
 *   GET    /api/kafka/topics/{name}     detalle de un tópico (con sus configuraciones)
 *   POST   /api/kafka/topics            crea un tópico
 *   PUT    /api/kafka/topics/{name}     aumenta particiones y/o cambia configuraciones
 *   DELETE /api/kafka/topics/{name}     elimina un tópico
 *
 *   GET    /api/kafka/consumer-groups              lista los consumer groups
 *   GET    /api/kafka/consumer-groups/{groupId}    detalle de un consumer group
 *
 *   POST   /api/kafka/publish           publica un mensaje de prueba simple
 *   POST   /api/kafka/publish-evento    publica el DTO EventoMensajeria completo
 *   POST   /api/kafka/publish-invalid   publica un mensaje inválido (va al DLT)
 *
 * Todas las rutas /api/** exigen JWT (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    // ----------------------------- Tópicos -----------------------------

    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse>> listarTopicos() {
        return ResponseEntity.ok(kafkaAdminService.listarTopicos());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<TopicResponse> obtenerTopico(@PathVariable("name") String name) {
        return ResponseEntity.ok(kafkaAdminService.obtenerTopico(name));
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicResponse> crearTopico(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kafkaAdminService.crearTopico(request));
    }

    @PutMapping("/topics/{name}")
    public ResponseEntity<TopicResponse> actualizarTopico(@PathVariable("name") String name,
                                                          @Valid @RequestBody TopicUpdateRequest request) {
        return ResponseEntity.ok(kafkaAdminService.actualizarTopico(name, request));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable("name") String name) {
        kafkaAdminService.eliminarTopico(name);
        return ResponseEntity.noContent().build();
    }

    // ----------------------- Consumer groups -----------------------

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<ConsumerGroupResponse>> listarGrupos() {
        return ResponseEntity.ok(kafkaAdminService.listarGrupos());
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<ConsumerGroupResponse> obtenerGrupo(@PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(kafkaAdminService.obtenerGrupo(groupId));
    }

    // ------------------------- Publicación -------------------------

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publicar(@Valid @RequestBody PublicarKafkaRequest request) {
        Notificacion notificacion = kafkaAdminService.publicar(request);
        return ResponseEntity.accepted().body(respuesta("Mensaje enviado correctamente a Kafka", notificacion));
    }

    @PostMapping("/publish-evento")
    public ResponseEntity<Map<String, Object>> publicarEvento(
            @Valid @RequestBody PublicarEventoKafkaRequest request) {
        Notificacion notificacion = kafkaAdminService.publicarEvento(request);
        return ResponseEntity.accepted().body(
                respuesta("Evento completo enviado correctamente a Kafka", notificacion));
    }

    @PostMapping("/publish-invalid")
    public ResponseEntity<Map<String, Object>> publicarInvalido() {
        Notificacion notificacion = kafkaAdminService.publicarInvalido();
        return ResponseEntity.accepted().body(
                respuesta("Mensaje inválido enviado a Kafka (terminará en el DLT)", notificacion));
    }

    private Map<String, Object> respuesta(String mensaje, Notificacion notificacion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("id", notificacion.getMensajeId());
        cuerpo.put("estado", notificacion.getEstado().name());
        return cuerpo;
    }
}
