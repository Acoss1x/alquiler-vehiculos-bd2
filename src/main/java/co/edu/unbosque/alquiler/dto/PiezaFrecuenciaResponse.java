package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record PiezaFrecuenciaResponse(String nombre, long totalCambiadas, BigDecimal costoTotal) {
}
