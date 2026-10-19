package co.edu.unbosque.alquiler.exception;

/**
 * Se lanza cuando la solicitud es incoherente (por ejemplo, fechas invertidas)
 * y se detecta antes de llegar a la base de datos. El {@link ApiExceptionHandler}
 * la traduce a HTTP 400.
 */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
