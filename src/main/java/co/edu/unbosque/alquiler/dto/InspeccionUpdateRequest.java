package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.document.NivelCombustible;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InspeccionUpdateRequest(
        @NotNull NivelCombustible combustible,
        @Size(max = 2000) String observacionesGenerales,
        @Valid @Size(max = 50) List<DanoDTO> danos,
        @Valid @Size(max = 50) List<PiezaDTO> piezasCambiadas
) {
}
