package co.edu.unbosque.alquiler.exception;

public class CuentaNoActivaException extends RuntimeException {
    public CuentaNoActivaException(String estado) {
        super("La cuenta no está activa (estado: " + estado + "). Comuníquese con la empresa");
    }
}