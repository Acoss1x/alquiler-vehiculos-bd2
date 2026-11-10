package co.edu.unbosque.alquiler.exception;

/**
 * Se lanza cuando una misma cuenta acumula demasiados intentos fallidos de recuperación
 * de contraseña. Se traduce a HTTP 429.
 */
public class DemasiadosIntentosException extends RuntimeException {
    public DemasiadosIntentosException() {
        super("Demasiados intentos fallidos. Espera unos minutos antes de volver a intentarlo");
    }
}