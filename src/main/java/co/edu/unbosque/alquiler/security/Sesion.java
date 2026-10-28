package co.edu.unbosque.alquiler.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Datos de la persona autenticada (leídos del token) y reglas de propiedad:
 * un cliente solo puede operar sobre lo suyo; el personal (administrador,
 * asesor, técnico) no tiene esa restricción, pero sí las de su rol.
 */
public final class Sesion {

    private Sesion() {
    }

    public static String rol(Jwt jwt) {
        return jwt.getClaimAsString("rol");
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static boolean esCliente(Jwt jwt) {
        return "CLIENTE".equals(rol(jwt));
    }

    public static boolean esAdministrador(Jwt jwt) {
        return "ADMINISTRADOR".equals(rol(jwt));
    }

    /** Un cliente solo puede actuar sobre su propio id; el personal, sobre cualquiera. */
    public static void exigirPropioOPersonal(Jwt jwt, Long idCliente) {
        if (esCliente(jwt) && !id(jwt).equals(idCliente)) {
            throw new AccessDeniedException("No tiene permisos sobre este recurso");
        }
    }
}