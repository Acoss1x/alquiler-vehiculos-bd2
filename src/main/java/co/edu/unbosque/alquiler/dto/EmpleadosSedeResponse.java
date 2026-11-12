package co.edu.unbosque.alquiler.dto;

public record EmpleadosSedeResponse(
        Long idSede,
        String nombreSede,
        String rol,
        long cantidad
) {
}