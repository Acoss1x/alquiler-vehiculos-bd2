package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del PATCH que cambia el estado de un vehículo.
 */
public record VehiculoEstadoRequest(@NotNull EstadoVehiculo estado) {
}

