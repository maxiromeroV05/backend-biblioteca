package com.biblioteca.exception;

import com.biblioteca.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza el manejo de excepciones de toda la API para que
 * todos los controladores devuelvan errores con el mismo formato
 * JSON, en lugar de que cada uno maneje sus propios try/catch.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - el recurso solicitado no existe.
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 409 - ya existe un registro con ese valor único (isbn, email, etc).
    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarDuplicado(RegistroDuplicadoException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 409 - la operación no se puede realizar por una regla de negocio.
    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarOperacionInvalida(OperacionInvalidaException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 400 - los datos recibidos no pasan las validaciones (@Valid).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay datos inválidos en la petición");
        cuerpo.put("estado", HttpStatus.BAD_REQUEST.value());
        cuerpo.put("fecha", LocalDateTime.now());

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        cuerpo.put("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    // 500 - cualquier otro error no controlado. Se deja como última red
    // de seguridad para no devolver el stacktrace crudo al cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "Ocurrió un error inesperado: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
