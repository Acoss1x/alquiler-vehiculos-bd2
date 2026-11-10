package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nueva contraseña definida con el token que llegó en el enlace del correo.
 */
public record RestablecerClaveRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Pattern(regexp = ClavePolitica.REGEX, message = ClavePolitica.MENSAJE) String claveNueva
) {
}
