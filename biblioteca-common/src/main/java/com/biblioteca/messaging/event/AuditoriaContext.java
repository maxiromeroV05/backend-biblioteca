package com.biblioteca.messaging.event;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Datos de auditoría que se incorporan al DTO cuando se publica un evento.
 *
 * <p>El usuario se toma del JWT (preferred_username) si hay una petición HTTP;
 * en publicaciones internas queda {@code null}. El correlationId se lee del MDC
 * (si el front o un filtro lo pone) y, si no hay, se genera uno nuevo para que
 * cada evento se pueda seguir de extremo a extremo.</p>
 */
public final class AuditoriaContext {

    private AuditoriaContext() {
    }

    public static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    public static String correlationId() {
        String id = MDC.get("correlationId");
        return id != null && !id.isBlank() ? id : UUID.randomUUID().toString();
    }
}
