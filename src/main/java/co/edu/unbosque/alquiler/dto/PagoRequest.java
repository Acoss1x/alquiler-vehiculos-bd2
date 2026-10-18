package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.entity.EstadoPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos de entrada para registrar un pago (abono o saldo) de una reserva.
 * {@code estado} es el resultado del cobro: APROBADO o RECHAZADO.
 */
public record PagoRequest(
        @NotNull @Positive BigDecimal monto,
        @NotBlank @Size(max = 30) String metodoPago,
        @NotNull EstadoPago estado
) {
}
