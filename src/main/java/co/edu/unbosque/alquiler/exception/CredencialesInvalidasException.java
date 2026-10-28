package co.edu.unbosque.alquiler.exception;

/**
 * Correo o contraseña incorrectos. El mensaje es el mismo en ambos casos para no
 * revelar si el correo existe. El {@link ApiExceptionHandler} la traduce a HTTP 401.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}