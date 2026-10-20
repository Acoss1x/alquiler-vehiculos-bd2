package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El documento no se edita; el correo sigue siendo único (la BD lo hace cumplir).
 */
public record ClienteUpdateRequest(
        @NotBlank @Size(max = 50) String primerNombre,
        @Size(max = 50) String segundoNombre,
        @NotBlank @Size(max = 50) String primerApellido,
        @Size(max = 50) String segundoApellido,
        @NotBlank @Email @Size(max = 100) String correo,
        @Size(max = 20) String telefono
) {
}