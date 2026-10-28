package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo del PATCH que cambia el estado de un cliente.
 */
public record ClienteEstadoRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVO|INACTIVO|BLOQUEADO", message = "El estado debe ser ACTIVO, INACTIVO o BLOQUEADO")
        String estado
) {
}