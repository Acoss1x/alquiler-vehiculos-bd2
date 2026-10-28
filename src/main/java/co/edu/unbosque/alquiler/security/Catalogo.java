package co.edu.unbosque.alquiler.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Regla común de los catálogos (sedes, categorías, seguros): las listas públicas
 * muestran solo lo ACTIVO; ver también lo INACTIVO es cosa del administrador.
 */
public final class Catalogo {

    private Catalogo() {
    }

    /** jwt puede ser null: las listas son públicas y quien consulta puede no haber iniciado sesión. */
    public static void exigirAdminSiPideInactivos(Jwt jwt, boolean incluirInactivos) {
        if (incluirInactivos && (jwt == null || !Sesion.esAdministrador(jwt))) {
            throw new AccessDeniedException("Solo un administrador puede ver los registros inactivos");
        }
    }
}
