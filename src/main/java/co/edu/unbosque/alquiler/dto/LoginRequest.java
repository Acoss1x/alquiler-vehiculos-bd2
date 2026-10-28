package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de acceso. No se aplica la política de contraseñas aquí: solo se compara.
 */
public record LoginRequest(
        @NotBlank @Size(max = 100) String correo,
        @NotBlank @Size(max = 72) String clave
) {
}