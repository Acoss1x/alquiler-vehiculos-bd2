package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.entity.TipoEmpleado;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoResponse(
        Long idEmpleado,
        String numeroDocumento,
        String primerNombre,
        String primerApellido,
        String correo,
        LocalDate fechaContratacion,
        String estado,
        Long idSede,
        String nombreSede,
        TipoEmpleado tipo,
        Integer mostradorAsignado,
        String especialidad,
        String nivelAcceso,
        BigDecimal bonoGerencial
) {
}