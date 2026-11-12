package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LicenciaRequest(
        @NotBlank @Size(max = 50) String numeroLicencia,
        @NotBlank @Size(max = 5) String categoriaLicencia,
        @NotNull @PastOrPresent LocalDate fechaExpedicion,
        @NotNull LocalDate fechaVencimiento
) {
}