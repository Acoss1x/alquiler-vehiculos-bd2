package co.edu.unbosque.alquiler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos de entrada para registrar una inspección. El vehículo viene en la URL.
 */
public record InspeccionRequest(
        @NotNull Long idTecnico,
        @Size(max = 2000) String observacionesGenerales,
        @Valid List<PiezaDto> piezasCambiadas
) {
}
