package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record GastoTecnicoResponse(Long idTecnico, long totalPiezas, BigDecimal costoTotal) {
}
