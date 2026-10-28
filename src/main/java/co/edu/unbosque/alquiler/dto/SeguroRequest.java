package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SeguroRequest(
        @NotBlank @Size(max = 50) String nombreSeguro,
        @Size(max = 200) String descripcion,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal costoDiario
) {}