package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import jakarta.validation.constraints.NotNull;


public record VehiculoEstadoRequest(@NotNull EstadoVehiculo estado) {
}

