package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record IngresoSedeResponse(
        Long idSede,
        String nombreSede,
        BigDecimal ingresos,
        long pagosAprobados
) {
}