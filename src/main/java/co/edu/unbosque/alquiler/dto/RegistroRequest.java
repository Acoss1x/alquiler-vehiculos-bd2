package co.edu.unbosque.alquiler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Registro de un cliente por sí mismo. La licencia es opcional al registrarse
 * (es obligatoria solo para confirmar un alquiler).
 */
public record RegistroRequest(
        @NotNull @Valid ClienteRequest cliente,
        @Valid LicenciaRequest licencia
) {
}