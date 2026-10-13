package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un vehículo. El estado inicial lo asigna el
 * procedimiento almacenado.
 */
public record VehiculoRequest(
        @NotBlank @Size(max = 10) String placa,
        @NotBlank @Size(max = 50) String marca,
        @NotNull @Min(1950) @Max(2100) Integer modeloAnio,
        @NotNull @PositiveOrZero Integer kilometraje,
        @Size(max = 30) String color,
        @NotNull Long idCategoria,
        @NotNull Long idSede
) {
}
