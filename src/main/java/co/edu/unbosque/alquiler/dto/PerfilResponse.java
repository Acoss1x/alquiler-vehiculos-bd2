package co.edu.unbosque.alquiler.dto;

/**
 * Datos de la persona autenticada. tipo: CLIENTE o EMPLEADO; rol: CLIENTE,
 * ADMINISTRADOR, ASESOR o TECNICO. Nunca incluye la contraseña.
 */
public record PerfilResponse(
        Long id,
        String tipo,
        String rol,
        String nombre,
        String correo,
        String estado,
        boolean debeCambiarClave
) {
}