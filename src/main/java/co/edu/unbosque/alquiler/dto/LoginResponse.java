package co.edu.unbosque.alquiler.dto;

/**
 * Respuesta de login, registro y cambio de clave: el token (Bearer) y el perfil.
 */
public record LoginResponse(
        String token,
        String tipoToken,
        long expiraEnSegundos,
        PerfilResponse usuario
) {
}