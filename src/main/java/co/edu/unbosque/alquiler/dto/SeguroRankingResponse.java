package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

/** Seguros ordenados por cuántas veces se contrataron (sin contar reservas canceladas). */
public record SeguroRankingResponse(
        Long idSeguro,
        String nombreSeguro,
        long vecesContratado,
        BigDecimal valorContratado
) {
}