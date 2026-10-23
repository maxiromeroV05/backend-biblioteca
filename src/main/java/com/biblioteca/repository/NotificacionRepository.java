package com.biblioteca.repository;

import com.biblioteca.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    Optional<Notificacion> findByMensajeId(String mensajeId);
}