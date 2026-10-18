package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

/**
 * Seguro contratado en una reserva, con el costo total pactado (días x costo diario).
 */
public record SeguroContratadoResponse(
        Long idSeguro,
        String nombreSeguro,
        BigDecimal costoContratado
) {
}
