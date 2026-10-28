package co.edu.unbosque.alquiler.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Escribe errores HTTP con el mismo formato JSON de {@code ApiError} desde los
 * filtros de seguridad, donde todavía no actúa el manejador de excepciones de Spring MVC.
 * Los mensajes son constantes del código (sin comillas), por eso no se escapan.
 */
public final class RespuestaJson {

    private RespuestaJson() {
    }

    public static void escribir(HttpServletResponse response, int status, String razon, String mensaje)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String cuerpo = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"error\":\"" + razon + "\",\"mensaje\":\"" + mensaje + "\",\"detalles\":null}";
        response.getWriter().write(cuerpo);
    }
}