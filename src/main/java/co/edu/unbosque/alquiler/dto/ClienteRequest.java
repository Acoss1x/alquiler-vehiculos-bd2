package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de entrada para registrar un cliente. No incluye id ni estado:
 * los asigna la base de datos.
 */
public record ClienteRequest(
        @NotBlank @Size(max = 20) String numeroDocumento,
        @NotBlank @Size(max = 50) String primerNombre,
        @Size(max = 50) String segundoNombre,
        @NotBlank @Size(max = 50) String primerApellido,
        @Size(max = 50) String segundoApellido,
        @NotBlank @Email @Size(max = 100) String correo,
        @Size(max = 20) String telefono,
        @NotBlank @Pattern(regexp = ClavePolitica.REGEX, message = ClavePolitica.MENSAJE) String clave
) {
}
