package co.edu.unbosque.alquiler.dto;

public final class ClavePolitica {

    public static final String REGEX = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{10,72}";
    public static final String MENSAJE =
            "La contraseña debe tener entre 10 y 72 caracteres e incluir mayúscula, minúscula y número";

    private ClavePolitica() {
    }
}