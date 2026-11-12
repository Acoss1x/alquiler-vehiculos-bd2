package co.edu.unbosque.alquiler.dto;


public record ClienteResponse(
        Long idCliente,
        String numeroDocumento,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String telefono,
        String estado
) {
}
