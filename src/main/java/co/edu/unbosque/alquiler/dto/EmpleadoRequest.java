package co.edu.unbosque.alquiler.dto;

import co.edu.unbosque.alquiler.entity.TipoEmpleado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Los últimos cuatro campos dependen del tipo: el servicio exige el que corresponda.
 */
public record EmpleadoRequest(
        @NotBlank @Size(max = 20) String numeroDocumento,
        @NotBlank @Size(max = 50) String primerNombre,
        @NotBlank @Size(max = 50) String primerApellido,
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Pattern(regexp = ClavePolitica.REGEX, message = ClavePolitica.MENSAJE) String clave,
        @NotNull @PastOrPresent LocalDate fechaContratacion,
        @NotNull Long idSede,
        @NotNull TipoEmpleado tipo,
        @Positive Integer mostradorAsignado,
        @Size(max = 50) String especialidad,
        @Size(max = 30) String nivelAcceso,
        @PositiveOrZero BigDecimal bonoGerencial
) {
}
