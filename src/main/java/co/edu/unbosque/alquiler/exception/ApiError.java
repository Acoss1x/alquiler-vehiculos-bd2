package co.edu.unbosque.alquiler.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error que devuelve toda la API.
 *
 * @param detalles errores por campo (solo en validaciones); puede ser null
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        Map<String, String> detalles
) {
}
