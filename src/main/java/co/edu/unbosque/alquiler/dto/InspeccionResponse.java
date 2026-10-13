package co.edu.unbosque.alquiler.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de salida de una inspección.
 */
public record InspeccionResponse(
        String idInspeccion,
        Long idVehiculo,
        Long idTecnico,
        String observacionesGenerales,
        List<PiezaDTO> piezasCambiadas,
        LocalDateTime fechaInspeccion
) {
}
