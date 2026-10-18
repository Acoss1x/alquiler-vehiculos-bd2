package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Versión corta de una reserva, para los listados.
 */
public record ReservaResumenResponse(
        Long idReserva,
        String estadoReserva,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal costoTotal,
        Long idCliente,
        String nombreCliente,
        Long idVehiculo,
        String placa
) {
}
