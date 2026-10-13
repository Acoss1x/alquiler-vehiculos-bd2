package co.edu.unbosque.alquiler.dto;

/**
 * Datos de salida de un vehículo, con el nombre de su categoría y sede.
 */
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

