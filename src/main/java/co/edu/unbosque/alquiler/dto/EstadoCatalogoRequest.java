package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo del PATCH que activa o desactiva una sede, una categoría o un seguro.
 */
public record EstadoCatalogoRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO")
        String estado
) {}
