package co.edu.unbosque.alquiler.dto;

public record SedeResponse(
        Long idSede,
        String nombreSede,
        String ciudad,
        String direccion,
        Integer capacidadEstacionamiento
) {
}
