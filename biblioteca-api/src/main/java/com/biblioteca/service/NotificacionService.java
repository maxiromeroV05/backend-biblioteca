package com.biblioteca.service;

import com.biblioteca.dto.auditoria.AuditoriaResponse;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionEstado;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.repository.NotificacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion crearPendiente(String contenido, NotificacionTipo tipo, String cola) {
        return crearPendiente(contenido, tipo, cola, null, null, null, null);
    }

    public Notificacion crearPendiente(String contenido, NotificacionTipo tipo, String cola, String destinatario) {
        return crearPendiente(contenido, tipo, cola, destinatario, null, null, null);
    }

    /** Versión con los datos de auditoría del DTO publicado. */
    public Notificacion crearPendiente(String contenido, NotificacionTipo tipo, String cola,
                                       String destinatario, EventoTipo eventoTipo,
                                       String correlationId, String usuario) {
        Notificacion notificacion = new Notificacion(contenido, tipo, cola);
        notificacion.setDestinatario(destinatario);
        notificacion.setEventoTipo(eventoTipo);
        notificacion.setCorrelationId(correlationId);
        notificacion.setUsuario(usuario);
        return notificacionRepository.save(notificacion);
    }

    public void marcarProcesado(String mensajeId) {
        Notificacion notificacion = notificacionRepository.findByMensajeId(mensajeId)
                .orElseThrow(() -> new IllegalArgumentException("No existe notificación con mensajeId: " + mensajeId));

        notificacion.setEstado(NotificacionEstado.PROCESADO);
        notificacion.setFechaProcesamiento(LocalDateTime.now());
        notificacion.setError(null);

        notificacionRepository.save(notificacion);
    }

    public void marcarFallido(String mensajeId, String error) {
        Notificacion notificacion = notificacionRepository.findByMensajeId(mensajeId)
                .orElseThrow(() -> new IllegalArgumentException("No existe notificación con mensajeId: " + mensajeId));

        notificacion.setEstado(NotificacionEstado.FALLIDO);
        notificacion.setFechaProcesamiento(LocalDateTime.now());
        // La columna error admite 1000 caracteres.
        notificacion.setError(error != null && error.length() > 1000 ? error.substring(0, 1000) : error);

        notificacionRepository.save(notificacion);
    }

    /** Todas las notificaciones, de la más reciente a la más antigua. */
    public List<Notificacion> listarTodas() {
        return notificacionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Historial filtrable por fecha, tipo de evento, usuario y correlationId.
     * Todos los filtros son opcionales; los que llegan null se ignoran.
     */
    public List<AuditoriaResponse> buscarAuditoria(LocalDateTime desde, LocalDateTime hasta,
                                                   EventoTipo tipo, String usuario,
                                                   String correlationId) {
        List<Specification<Notificacion>> filtros = new ArrayList<>();
        if (desde != null) {
            filtros.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
        }
        if (hasta != null) {
            filtros.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
        }
        if (tipo != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("eventoTipo"), tipo));
        }
        if (usuario != null && !usuario.isBlank()) {
            filtros.add((root, query, cb) -> cb.equal(root.get("usuario"), usuario));
        }
        if (correlationId != null && !correlationId.isBlank()) {
            filtros.add((root, query, cb) -> cb.equal(root.get("correlationId"), correlationId));
        }

        Specification<Notificacion> spec = (root, query, cb) -> cb.conjunction();
        for (Specification<Notificacion> filtro : filtros) {
            spec = spec.and(filtro);
        }

        return notificacionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream()
                .map(this::aAuditoria)
                .toList();
    }

    private AuditoriaResponse aAuditoria(Notificacion n) {
        return new AuditoriaResponse(
                n.getId(),
                n.getMensajeId(),
                n.getEventoTipo() != null ? n.getEventoTipo().name() : null,
                n.getEstado() != null ? n.getEstado().name() : null,
                n.getUsuario(),
                n.getCorrelationId(),
                n.getDestinatario(),
                n.getContenido(),
                n.getCola(),
                n.getFechaCreacion() != null ? n.getFechaCreacion().toString() : null,
                n.getFechaProcesamiento() != null ? n.getFechaProcesamiento().toString() : null,
                n.getError()
        );
    }
}
