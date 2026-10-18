package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoResponse(
        Long idPago,
        LocalDate fechaPago,
        BigDecimal monto,
        String metodoPago,
        String estadoPago
) {
}
