package co.edu.unbosque.alquiler.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con un formato uniforme.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> detalles.put(e.getField(), e.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonMalformado(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido (JSON malformado o valor no permitido)", null);
    }

    /** Parámetro de URL o query con tipo/valor inválido (ej. ?estado=XYZ). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + ex.getName() + "'", null);
    }

    /** UNIQUE, FK o NOT NULL violados en la base de datos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex) {
        return construir(HttpStatus.CONFLICT,
                "Violación de integridad: dato duplicado o referencia inválida", null);
    }

    /** PostgreSQL o MongoDB no disponibles. */
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiError> baseNoDisponible(DataAccessResourceFailureException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, "La base de datos no está disponible", null);
    }

    /**
     * Errores lanzados desde la BD (por ejemplo, RAISE EXCEPTION en un procedimiento
     * almacenado). Se devuelve solo la primera línea del mensaje de la causa raíz.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> reglaDeNegocioBD(DataAccessException ex) {
        String causa = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        String mensaje = causa == null ? "Error en la base de datos" : causa.lines().findFirst().orElse(causa);
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, mensaje, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, Map<String, String> detalles) {
        ApiError cuerpo = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
