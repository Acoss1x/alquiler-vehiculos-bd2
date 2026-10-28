package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.document.NivelCombustible;
import co.edu.unbosque.alquiler.document.TipoInspeccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos para registrar una inspección. El vehículo viene en la URL y el empleado
 * responsable sale del token. {@code idReserva} es obligatorio en entrega y devolución
 * y debe ir vacío en mantenimiento; las piezas solo aplican a mantenimiento.
 */
public record InspeccionRequest(
        @NotNull TipoInspeccion tipo,
        Long idReserva,
        @NotNull NivelCombustible combustible,
        @NotNull @PositiveOrZero Integer kilometraje,
        @Size(max = 2000) String observacionesGenerales,
        @Valid @Size(max = 50) List<DanoDTO> danos,
        @Valid @Size(max = 50) List<PiezaDTO> piezasCambiadas
) {
}