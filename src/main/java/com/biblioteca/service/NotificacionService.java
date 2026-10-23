package com.biblioteca.service;

import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionEstado;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.repository.NotificacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion crearPendiente(String contenido, NotificacionTipo tipo, String cola) {
        return crearPendiente(contenido, tipo, cola, null);
    }

    public Notificacion crearPendiente(String contenido, NotificacionTipo tipo, String cola, String destinatario) {
        Notificacion notificacion = new Notificacion(contenido, tipo, cola);
        notificacion.setDestinatario(destinatario);
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
}
