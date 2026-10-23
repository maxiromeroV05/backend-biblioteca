package com.biblioteca.controller;

import com.biblioteca.dto.rabbit.BindingRequest;
import com.biblioteca.dto.rabbit.ColaRequest;
import com.biblioteca.dto.rabbit.ColaResponse;
import com.biblioteca.dto.rabbit.ExchangeRequest;
import com.biblioteca.dto.rabbit.PublicarRequest;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST de administración de RabbitMQ. Toda la lógica vive en
 * RabbitAdminService; aquí solo se reciben y validan las peticiones.
 *
 *   GET    /api/rabbit/queues            lista colas
 *   POST   /api/rabbit/queues            crea una cola
 *   DELETE /api/rabbit/queues/{name}     elimina una cola
 *
 *   GET    /api/rabbit/exchanges         lista exchanges
 *   POST   /api/rabbit/exchanges         crea un exchange
 *   DELETE /api/rabbit/exchanges/{name}  elimina un exchange
 *
 *   GET    /api/rabbit/bindings          lista bindings
 *   POST   /api/rabbit/bindings          crea un binding
 *   DELETE /api/rabbit/bindings?exchange=&queue=&routingKey=   elimina un binding
 *
 *   POST   /api/rabbit/publish           publica un mensaje de prueba
 *   POST   /api/rabbit/publish-invalid   publica un mensaje inválido (va a la DLQ)
 *
 * Todas las rutas /api/** exigen JWT (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    // ---------------------------- Colas ----------------------------

    @GetMapping("/queues")
    public ResponseEntity<List<ColaResponse>> listarColas() {
        return ResponseEntity.ok(rabbitAdminService.listarColas());
    }

    @PostMapping("/queues")
    public ResponseEntity<ColaResponse> crearCola(@Valid @RequestBody ColaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearCola(request));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable("name") String name) {
        rabbitAdminService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    // -------------------------- Exchanges --------------------------

    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeRequest>> listarExchanges() {
        return ResponseEntity.ok(rabbitAdminService.listarExchanges());
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeRequest> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearExchange(request));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable("name") String name) {
        rabbitAdminService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // -------------------------- Bindings ---------------------------

    @GetMapping("/bindings")
    public ResponseEntity<List<BindingRequest>> listarBindings() {
        return ResponseEntity.ok(rabbitAdminService.listarBindings());
    }

    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> crearBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @ModelAttribute BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }

    // ------------------------- Publicación -------------------------

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publicar(@Valid @RequestBody PublicarRequest request) {
        Notificacion notificacion = rabbitAdminService.publicar(request);
        return ResponseEntity.accepted().body(respuesta("Mensaje enviado correctamente a RabbitMQ", notificacion));
    }

    @PostMapping("/publish-invalid")
    public ResponseEntity<Map<String, Object>> publicarInvalido() {
        Notificacion notificacion = rabbitAdminService.publicarInvalido();
        return ResponseEntity.accepted().body(
                respuesta("Mensaje inválido enviado a RabbitMQ (terminará en la DLQ)", notificacion));
    }

    private Map<String, Object> respuesta(String mensaje, Notificacion notificacion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("id", notificacion.getMensajeId());
        cuerpo.put("estado", notificacion.getEstado().name());
        return cuerpo;
    }
}
