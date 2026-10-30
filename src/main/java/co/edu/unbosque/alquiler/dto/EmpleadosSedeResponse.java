package co.edu.unbosque.alquiler.dto;

/** Empleados activos de una sede, agrupados por rol (ADMINISTRADOR, ASESOR, TECNICO). */
public record EmpleadosSedeResponse(
        Long idSede,
        String nombreSede,
        String rol,
        long cantidad
) {
}