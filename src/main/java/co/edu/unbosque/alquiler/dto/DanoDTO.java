package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.document.SeveridadDano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DanoDTO(
        @NotBlank @Size(max = 100) String zona,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull SeveridadDano severidad
) {
}
