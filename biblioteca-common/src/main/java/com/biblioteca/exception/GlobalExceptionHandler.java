package com.biblioteca.exception;

import com.biblioteca.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    // 400 - la petición tiene sentido de forma aislada pero sus datos son incoherentes.
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 400 - el cuerpo de la petición no es un JSON válido.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse("El cuerpo de la petición no es un JSON válido",
                HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 503 - no se pudo contactar a RabbitMQ (API de administración) o a Kafka.
    @ExceptionHandler(ServicioMensajeriaException.class)
    public ResponseEntity<ErrorResponse> manejarMensajeriaNoDisponible(ServicioMensajeriaException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // 502 - RabbitMQ rechazó o no pudo ejecutar la operación.
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorResponse> manejarErrorRabbit(AmqpException ex) {
        ErrorResponse error = new ErrorResponse("Error al comunicarse con RabbitMQ: " + ex.getMessage(),
                HttpStatus.BAD_GATEWAY.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    // 502 - Kafka rechazó o no pudo ejecutar la operación.
    @ExceptionHandler(org.apache.kafka.common.KafkaException.class)
    public ResponseEntity<ErrorResponse> manejarErrorKafka(org.apache.kafka.common.KafkaException ex) {
        ErrorResponse error = new ErrorResponse("Error al comunicarse con Kafka: " + ex.getMessage(),
                HttpStatus.BAD_GATEWAY.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
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

    // 404 - la ruta pedida no existe (antes caía en el handler general y devolvía 500).
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(NoResourceFoundException ex) {
        ErrorResponse error = new ErrorResponse("La ruta solicitada no existe",
                HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 405 - la ruta existe pero no acepta ese método HTTP (por ejemplo, POST en una ruta GET).
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse("Método HTTP no permitido para esta ruta",
                HttpStatus.METHOD_NOT_ALLOWED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // 400 - un parámetro de la URL tiene un tipo incorrecto (por ejemplo, /api/libros/abc).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse("El parámetro '" + ex.getName() + "' tiene un valor no válido",
                HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 500 - cualquier otro error no controlado. El detalle va solo al log: al cliente
    // no se le devuelve el mensaje interno ni el stacktrace.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(Exception ex) {
        log.error("Error no controlado", ex);
        ErrorResponse error = new ErrorResponse(
                "Ocurrió un error inesperado. Intenta nuevamente más tarde.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
