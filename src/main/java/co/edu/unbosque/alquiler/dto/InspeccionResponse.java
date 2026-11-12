package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.document.NivelCombustible;
import co.edu.unbosque.alquiler.document.TipoInspeccion;

import java.time.LocalDateTime;
import java.util.List;


public record InspeccionResponse(
        String idInspeccion,
        TipoInspeccion tipo,
        Long idVehiculo,
        Long idReserva,
        Long idEmpleado,
        NivelCombustible combustible,
        Integer kilometraje,
        String observacionesGenerales,
        List<DanoDTO> danos,
        List<PiezaDTO> piezasCambiadas,
        List<String> fotos,
        LocalDateTime fechaInspeccion
) {
}