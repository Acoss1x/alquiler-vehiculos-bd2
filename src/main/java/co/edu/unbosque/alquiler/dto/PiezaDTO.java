package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record PiezaDTO(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @Positive Integer cantidad,
        @NotNull @PositiveOrZero BigDecimal costo
) {
}

