package co.edu.unbosque.alquiler.dto;

public record VehiculoResponse(
        Long idVehiculo,
        String placa,
        String marca,
        Integer modeloAnio,
        Integer kilometraje,
        String estado,
        String color,
        Long idCategoria,
        String nombreCategoria,
        Long idSede,
        String nombreSede
) {
}

