package com.biblioteca.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String mensajeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificacionTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificacionEstado estado;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaProcesamiento;

    @Column(length = 100)
    private String cola;

    @Column(length = 150)
    private String destinatario;

    @Column(length = 1000)
    private String error;

    public Notificacion() {
    }

    public Notificacion(String contenido, NotificacionTipo tipo, String cola) {
        this.mensajeId = UUID.randomUUID().toString();
        this.contenido = contenido;
        this.tipo = tipo;
        this.estado = NotificacionEstado.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
        this.cola = cola;
    }

    public Long getId() {
        return id;
    }

    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public NotificacionTipo getTipo() {
        return tipo;
    }

    public void setTipo(NotificacionTipo tipo) {
        this.tipo = tipo;
    }

    public NotificacionEstado getEstado() {
        return estado;
    }

    public void setEstado(NotificacionEstado estado) {
        this.estado = estado;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getCola() {
        return cola;
    }

    public void setCola(String cola) {
        this.cola = cola;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}