package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PenalizacionEstadoRequest(
        @NotBlank
        @Pattern(regexp = "PAGADA|ANULADA", message = "El estado debe ser PAGADA o ANULADA")
        String estado
) {
}
