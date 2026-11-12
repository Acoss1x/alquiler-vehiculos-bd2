package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotNull;


public record IniciarReservaRequest(@NotNull Long idAsesor) {
}