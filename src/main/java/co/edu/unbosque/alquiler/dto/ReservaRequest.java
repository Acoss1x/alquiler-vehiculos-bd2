package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

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
