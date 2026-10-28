package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cargo por daños. {@code idInspeccion} es opcional: la inspección de Mongo que documenta el daño.
 */
public record PenalizacionRequest(
        @NotBlank @Size(max = 200) String concepto,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal monto,
        @Pattern(regexp = "^[0-9a-f]{24}$", message = "El identificador de inspección no es válido")
        String idInspeccion
) {
}
