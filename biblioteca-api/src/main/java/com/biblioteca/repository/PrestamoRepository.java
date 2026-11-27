package com.biblioteca.repository;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioId(Long usuarioId);

    List<Prestamo> findByLibroId(Long libroId);

    List<Prestamo> findByEstado(EstadoPrestamo estado);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    boolean existsByLibroIdAndEstado(Long libroId, EstadoPrestamo estado);
}
