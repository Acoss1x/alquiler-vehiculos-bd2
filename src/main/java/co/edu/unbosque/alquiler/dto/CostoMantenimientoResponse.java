package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

/**
 * Costo acumulado de piezas cambiadas en todas las inspecciones de un vehículo.
 */
public record CostoMantenimientoResponse(Long idVehiculo, BigDecimal costoTotal) {
}
