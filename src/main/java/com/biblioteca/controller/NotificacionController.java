package com.biblioteca.controller;

import com.biblioteca.dto.NotificacionResponse;
import com.biblioteca.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar() {
        List<NotificacionResponse> respuesta = notificacionService.listarTodas().stream()
                .map(NotificacionResponse::desdeEntidad)
                .toList();
        return ResponseEntity.ok(respuesta);
    }
}
