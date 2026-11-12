package co.edu.unbosque.alquiler.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

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

    public static void exigirPropioOPersonal(Jwt jwt, Long idCliente) {
        if (esCliente(jwt) && !id(jwt).equals(idCliente)) {
            throw new AccessDeniedException("No tiene permisos sobre este recurso");
        }
    }
}