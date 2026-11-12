package co.edu.unbosque.alquiler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public record RegistroRequest(
        @NotNull @Valid ClienteRequest cliente,
        @Valid LicenciaRequest licencia
) {
}