package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para entregar el vehículo: el asesor con contrato activo que registra la entrega.
 */
public record IniciarReservaRequest(@NotNull Long idAsesor) {
}