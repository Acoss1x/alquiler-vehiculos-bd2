package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de enlace para restablecer la contraseña: se envía al correo registrado.
 */
public record OlvideClaveRequest(
        @NotBlank @Size(max = 100) String correo
) {
}
