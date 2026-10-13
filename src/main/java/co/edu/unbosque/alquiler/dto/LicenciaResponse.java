package co.edu.unbosque.alquiler.dto;

import java.time.LocalDate;

/**
 * Datos de salida de una licencia.
 */
public record LicenciaResponse(
        Long idLicencia,
        Long idCliente,
        String numeroLicencia,
        String categoriaLicencia,
        LocalDate fechaExpedicion,
        LocalDate fechaVencimiento
) {
}
