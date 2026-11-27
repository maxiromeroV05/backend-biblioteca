package com.biblioteca.service;

import com.biblioteca.dto.auditoria.AuditoriaResponse;
import com.biblioteca.entity.Notificacion;
import com.biblioteca.entity.NotificacionTipo;
import com.biblioteca.messaging.event.EventoTipo;
import com.biblioteca.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba del mapeo del historial de auditoría con sus filtros opcionales. */
class NotificacionServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void buscarAuditoriaDevuelveLosDatosDeTrazabilidad() {
        NotificacionRepository repository = mock(NotificacionRepository.class);
        NotificacionService service = new NotificacionService(repository);

        Notificacion notificacion = new Notificacion("Préstamo creado",
                NotificacionTipo.NOTIFICACION, "biblioteca.prestamos");
        notificacion.setEventoTipo(EventoTipo.PRESTAMO_CREADO);
        notificacion.setUsuario("ana@correo.cl");
        notificacion.setCorrelationId("corr-123");
        notificacion.setDestinatario("ana@correo.cl");

        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(notificacion));

        LocalDateTime desde = LocalDateTime.of(2026, 11, 27, 0, 0);
        List<AuditoriaResponse> respuesta = service.buscarAuditoria(
                desde, null, EventoTipo.PRESTAMO_CREADO, "ana@correo.cl", "corr-123");

        assertEquals(1, respuesta.size());
        AuditoriaResponse fila = respuesta.get(0);
        assertEquals("PRESTAMO_CREADO", fila.eventoTipo());
        assertEquals("ana@correo.cl", fila.usuario());
        assertEquals("corr-123", fila.correlationId());
        assertEquals("biblioteca.prestamos", fila.canal());
        assertEquals("PENDIENTE", fila.estado());
        verify(repository).findAll(any(Specification.class), any(Sort.class));
    }
}
