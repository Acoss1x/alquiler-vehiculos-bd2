package co.edu.unbosque.alquiler.dto;

import java.time.LocalDate;

public record LicenciaResponse(
        Long idLicencia,
        Long idCliente,
        String numeroLicencia,
        String categoriaLicencia,
        LocalDate fechaExpedicion,
        LocalDate fechaVencimiento
) {
}
