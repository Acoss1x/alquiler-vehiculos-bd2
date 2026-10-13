package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de entrada para registrar la licencia de un cliente.
 * Las reglas que cruzan campos o consultan datos (vigencia, una licencia por cliente)
 * las valida el procedimiento almacenado.
 */
public record LicenciaRequest(
        @NotBlank @Size(max = 50) String numeroLicencia,
        @NotBlank @Size(max = 5) String categoriaLicencia,
        @NotNull @PastOrPresent LocalDate fechaExpedicion,
        @NotNull LocalDate fechaVencimiento
) {
}