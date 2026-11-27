package com.biblioteca.repository;

import com.biblioteca.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {

    Optional<Notificacion> findByMensajeId(String mensajeId);
}
