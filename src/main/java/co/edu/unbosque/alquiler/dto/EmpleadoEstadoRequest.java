package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmpleadoEstadoRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO")
        String estado
) {
}
