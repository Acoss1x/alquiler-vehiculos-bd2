package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CategoriaRequest(
        @NotBlank @Size(max = 50) String nombreCategoria,
        @NotNull @Positive Integer capacidadPasajeros,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal tarifaDiaria,
        @NotNull @Pattern(regexp = "MANUAL|AUTOMATICA", message = "La transmisión debe ser MANUAL o AUTOMATICA")
        String tipoTransmision
) {
}
