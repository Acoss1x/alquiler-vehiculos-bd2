package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record SeguroResponse(
        Long idSeguro,
        String nombreSeguro,
        String descripcion,
        BigDecimal costoDiario
) {
}
