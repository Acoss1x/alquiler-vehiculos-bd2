package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record SeguroContratadoResponse(
        Long idSeguro,
        String nombreSeguro,
        BigDecimal costoContratado
) {
}
