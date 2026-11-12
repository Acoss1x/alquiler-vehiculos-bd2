package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record SeguroRankingResponse(
        Long idSeguro,
        String nombreSeguro,
        long vecesContratado,
        BigDecimal valorContratado
) {
}