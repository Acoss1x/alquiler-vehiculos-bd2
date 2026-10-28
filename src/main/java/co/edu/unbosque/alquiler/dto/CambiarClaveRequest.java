package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contraseña de la persona autenticada.
 */
public record CambiarClaveRequest(
        @NotBlank @Size(max = 72) String claveActual,
        @NotBlank @Pattern(regexp = ClavePolitica.REGEX, message = ClavePolitica.MENSAJE) String claveNueva
) {
}