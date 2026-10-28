package co.edu.unbosque.alquiler.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> credencialesInvalidas(CredencialesInvalidasException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(CuentaNoActivaException.class)
    public ResponseEntity<ApiError> cuentaNoActiva(CuentaNoActivaException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    /** Falta de permisos (por rol); sin este manejador el "Exception" genérico lo volvería un 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex) {
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> noAutenticado(AuthenticationException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Autenticación requerida o token inválido", null);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> solicitudInvalida(SolicitudInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
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

    /** Petición con cuerpo sin "Content-Type: application/json". */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> tipoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return construir(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de contenido no soportado: use application/json", null);
    }

    /** Método HTTP que la ruta no admite (ej. GET sobre un POST). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta", null);
    }

    /** Ruta que no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaNoExiste(NoResourceFoundException ex) {
        return construir(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", null);
    }

    /** Falta un parámetro obligatorio de la URL (ej. fechaInicio en /disponibles). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroFaltante(MissingServletRequestParameterException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", null);
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
        String mensaje = causa == null ? "Error en la base de datos"
                : causa.lines().findFirst().orElse(causa).replaceFirst("^ERROR:\s*", "");
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