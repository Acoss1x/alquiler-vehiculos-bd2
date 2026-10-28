package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PenalizacionResponse(
        Long idPenalizacion,
        Long idReserva,
        LocalDate fechaCargo,
        String concepto,
        BigDecimal monto,
        String estadoCargo,
        String idInspeccion
) {
}
