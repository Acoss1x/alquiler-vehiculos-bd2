package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record CotizacionResponse(
        Long idVehiculo,
        int dias,
        BigDecimal costoTotal
) {
}
