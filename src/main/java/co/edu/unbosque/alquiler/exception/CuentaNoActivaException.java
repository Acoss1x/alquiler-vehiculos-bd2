package co.edu.unbosque.alquiler.exception;

/**
 * La contraseña es correcta pero la cuenta está inactiva o bloqueada.
 * El {@link ApiExceptionHandler} la traduce a HTTP 403.
 */
public class CuentaNoActivaException extends RuntimeException {
    public CuentaNoActivaException(String estado) {
        super("La cuenta no está activa (estado: " + estado + "). Comuníquese con la empresa");
    }
}