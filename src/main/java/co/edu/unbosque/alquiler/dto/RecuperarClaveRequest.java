package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Recuperación de contraseña por la propia persona: se identifica con el correo y el
 * número de documento registrados y define su nueva contraseña.
 */
public record RecuperarClaveRequest(
        @NotBlank @Size(max = 100) String correo,
        @NotBlank @Size(max = 20) String numeroDocumento,
        @NotBlank @Pattern(regexp = ClavePolitica.REGEX, message = ClavePolitica.MENSAJE) String claveNueva
) {
}