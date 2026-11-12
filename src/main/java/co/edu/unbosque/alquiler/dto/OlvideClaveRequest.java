package co.edu.unbosque.alquiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record OlvideClaveRequest(
        @NotBlank @Size(max = 100) String correo
) {
}
