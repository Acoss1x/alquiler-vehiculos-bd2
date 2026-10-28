package co.edu.unbosque.alquiler.entity;

/**
 * Estados posibles de un vehículo. En la base de datos se guarda como texto
 * (columna {@code estado}) y debe coincidir con el CHECK de la tabla VEHICULO.
 */
public enum EstadoVehiculo {
    DISPONIBLE,
    ALQUILADO,
    EN_MANTENIMIENTO,
    BAJA,
    VENDIDO
}

