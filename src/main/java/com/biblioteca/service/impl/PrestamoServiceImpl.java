package com.biblioteca.service.impl;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Libro;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.entity.Usuario;
import com.biblioteca.exception.OperacionInvalidaException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.UsuarioRepository;
import com.biblioteca.service.LibroService;
import com.biblioteca.service.PrestamoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoServiceImpl implements PrestamoService {

    // Cantidad de días por defecto que dura un préstamo si no se
    // indica una fecha de devolución esperada.
    private static final int DIAS_PRESTAMO_POR_DEFECTO = 14;

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroService libroService;

    public PrestamoServiceImpl(PrestamoRepository prestamoRepository,
                                LibroRepository libroRepository,
                                UsuarioRepository usuarioRepository,
                                LibroService libroService) {
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroService = libroService;
    }

    @Override
    public List<Prestamo> listarTodos() {
        return prestamoRepository.findAll();
    }

    @Override
    public Prestamo buscarPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el préstamo con id " + id));
    }

    @Override
    public List<Prestamo> listarPorUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public Prestamo crear(Prestamo prestamo) {
        Libro libro = libroRepository.findById(prestamo.getLibro().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el libro con id " + prestamo.getLibro().getId()));

        Usuario usuario = usuarioRepository.findById(prestamo.getUsuario().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario con id " + prestamo.getUsuario().getId()));

        if (libro.getCantidadDisponible() <= 0) {
            throw new OperacionInvalidaException("No hay ejemplares disponibles de '" + libro.getTitulo() + "'");
        }

        prestamo.setId(null);
        prestamo.setLibro(libro);
        prestamo.setUsuario(usuario);
        prestamo.setFechaPrestamo(LocalDate.now());
        if (prestamo.getFechaDevolucionEsperada() == null) {
            prestamo.setFechaDevolucionEsperada(LocalDate.now().plusDays(DIAS_PRESTAMO_POR_DEFECTO));
        }
        prestamo.setFechaDevolucionReal(null);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        Prestamo guardado = prestamoRepository.save(prestamo);

        // Solo se descuenta disponibilidad una vez que el préstamo
        // quedó guardado correctamente.
        libroService.decrementarDisponibilidad(libro.getId());

        return guardado;
    }

    @Override
    public Prestamo actualizar(Long id, Prestamo prestamo) {
        Prestamo existente = buscarPorId(id);

        // Solo se permite corregir la fecha esperada de devolución y las observaciones;
        // el libro, el usuario y el estado se manejan con reglas propias (crear/devolver).
        existente.setFechaDevolucionEsperada(prestamo.getFechaDevolucionEsperada());
        existente.setObservaciones(prestamo.getObservaciones());

        return prestamoRepository.save(existente);
    }

    @Override
    public Prestamo registrarDevolucion(Long id) {
        Prestamo prestamo = buscarPorId(id);

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new OperacionInvalidaException("Este préstamo ya fue devuelto");
        }

        prestamo.setFechaDevolucionReal(LocalDate.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        Prestamo actualizado = prestamoRepository.save(prestamo);

        libroService.incrementarDisponibilidad(prestamo.getLibro().getId());

        return actualizado;
    }

    @Override
    public void eliminar(Long id) {
        Prestamo prestamo = buscarPorId(id);
        prestamoRepository.delete(prestamo);
    }

    /**
     * Tarea programada que corre todos los días a la 1:00 AM y marca
     * como ATRASADO cualquier préstamo ACTIVO cuya fecha de
     * devolución esperada ya pasó. Se activa con @EnableScheduling
     * en la clase principal.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void marcarPrestamosAtrasados() {
        List<Prestamo> activos = prestamoRepository.findByEstado(EstadoPrestamo.ACTIVO);
        LocalDate hoy = LocalDate.now();
        for (Prestamo prestamo : activos) {
            if (prestamo.getFechaDevolucionEsperada().isBefore(hoy)) {
                prestamo.setEstado(EstadoPrestamo.ATRASADO);
                prestamoRepository.save(prestamo);
            }
        }
    }
}
