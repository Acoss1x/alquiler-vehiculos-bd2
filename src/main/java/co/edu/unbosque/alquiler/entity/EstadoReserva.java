package co.edu.unbosque.alquiler.entity;

/**
 * Estados de una reserva. En la base de datos se guarda como texto
 * (columna {@code estado_reserva}) y debe coincidir con el CHECK de la tabla RESERVA.
 * Flujo: PENDIENTE -> CONFIRMADA -> EN_CURSO -> FINALIZADA, o CANCELADA.
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA
}
