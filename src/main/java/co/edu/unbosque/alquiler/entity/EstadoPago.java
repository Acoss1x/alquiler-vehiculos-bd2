package co.edu.unbosque.alquiler.entity;

/**
 * Estados de un pago. REEMBOLSADO lo asigna la base de datos al cancelar una
 * reserva; por la API solo se registran pagos APROBADO o RECHAZADO.
 */
public enum EstadoPago {
    APROBADO,
    RECHAZADO,
    REEMBOLSADO
}
