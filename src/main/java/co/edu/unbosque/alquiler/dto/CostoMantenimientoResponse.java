package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;


public record CostoMantenimientoResponse(Long idVehiculo, BigDecimal costoTotal) {
}
