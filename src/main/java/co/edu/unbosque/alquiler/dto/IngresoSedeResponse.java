package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

/** Dinero recaudado (pagos aprobados) por las reservas que se entregan en cada sede. */
public record IngresoSedeResponse(
        Long idSede,
        String nombreSede,
        BigDecimal ingresos,
        long pagosAprobados
) {
}