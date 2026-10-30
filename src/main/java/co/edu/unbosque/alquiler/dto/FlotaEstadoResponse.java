package co.edu.unbosque.alquiler.dto;

/** Cantidad de vehículos en cada estado (DISPONIBLE, ALQUILADO, EN_MANTENIMIENTO). */
public record FlotaEstadoResponse(
        String estado,
        long cantidad
) {
}