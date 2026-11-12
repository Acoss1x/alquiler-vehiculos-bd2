package co.edu.unbosque.alquiler.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class Catalogo {

    private Catalogo() {
    }

    public static void exigirAdminSiPideInactivos(Jwt jwt, boolean incluirInactivos) {
        if (incluirInactivos && (jwt == null || !Sesion.esAdministrador(jwt))) {
            throw new AccessDeniedException("Solo un administrador puede ver los registros inactivos");
        }
    }
}
