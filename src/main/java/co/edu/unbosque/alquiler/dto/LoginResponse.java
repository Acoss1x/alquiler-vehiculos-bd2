package co.edu.unbosque.alquiler.dto;


public record LoginResponse(
        String token,
        String tipoToken,
        long expiraEnSegundos,
        PerfilResponse usuario
) {
}