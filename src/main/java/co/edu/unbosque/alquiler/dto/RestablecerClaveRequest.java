package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Persona a la que se le restablece la contraseña (tipo: CLIENTE o EMPLEADO).
 */
public record RestablecerClaveRequest(
        @NotBlank @Pattern(regexp = "CLIENTE|EMPLEADO", message = "El tipo debe ser CLIENTE o EMPLEADO") String tipo,
        @NotNull Long id
) {
}