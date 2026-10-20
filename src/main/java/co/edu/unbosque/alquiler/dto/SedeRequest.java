package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SedeRequest(
        @NotBlank @Size(max = 100) String nombreSede,
        @NotBlank @Size(max = 50) String ciudad,
        @NotBlank @Size(max = 150) String direccion,
        @NotNull @Positive Integer capacidadEstacionamiento
) {
}
