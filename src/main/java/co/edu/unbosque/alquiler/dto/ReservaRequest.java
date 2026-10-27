package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de entrada para crear una reserva. No incluye estado ni costo: la base
 * de datos crea la reserva en PENDIENTE y calcula el costo pactado.
 */
public record ReservaRequest(
        @NotNull Long idCliente,
        @NotNull Long idVehiculo,
        Long idAsesor,
        @NotNull Long idSedeEntrega,
        @NotNull Long idSedeRecibe,
        @NotNull @FutureOrPresent LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        List<@NotNull Long> idsSeguros
) {
}
