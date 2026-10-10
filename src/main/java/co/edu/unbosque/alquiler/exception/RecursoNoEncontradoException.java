package co.edu.unbosque.alquiler.exception;

/**
 * Se lanza cuando un recurso solicitado (cliente, reserva, etc.) no existe.
 * El {@link ApiExceptionHandler} la traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
