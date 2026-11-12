package co.edu.unbosque.alquiler.dto;


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