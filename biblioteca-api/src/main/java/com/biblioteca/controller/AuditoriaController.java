package com.biblioteca.controller;

import com.biblioteca.dto.auditoria.AuditoriaResponse;
import com.biblioteca.exception.SolicitudInvalidaException;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.service.NotificacionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Historial de eventos para el frontend. Todos los filtros son opcionales:
 *
 * GET /api/auditoria?desde=...&hasta=...&tipo=PRESTAMO_CREADO&usuario=ana&correlationId=...
 *
 * Las fechas van en ISO-8601 local (2026-11-27T10:00:00).
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final NotificacionService notificacionService;

    public AuditoriaController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaResponse>> buscar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String correlationId) {

        EventoTipo eventoTipo = parsearTipo(tipo);
        return ResponseEntity.ok(
                notificacionService.buscarAuditoria(desde, hasta, eventoTipo, usuario, correlationId));
    }

    private EventoTipo parsearTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return EventoTipo.valueOf(tipo.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException(
                    "Tipo de evento no válido: " + tipo
                            + ". Use NOTIFICACION, TICKET, DOCUMENTO, PRESTAMO_CREADO o PRESTAMO_DEVUELTO");
        }
    }
}
