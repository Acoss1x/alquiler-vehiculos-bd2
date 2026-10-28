package co.edu.unbosque.alquiler.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Si el token indica que la clave es temporal (restablecida por un empleado),
 * solo deja consultar el perfil y cambiar la clave; todo lo demás se rechaza con 403
 * hasta que la persona defina su propia contraseña.
 */
public class ClaveTemporalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt
                && Boolean.TRUE.equals(jwt.getClaimAsBoolean("cambiarClave"))) {
            String ruta = request.getRequestURI();
            if (!ruta.endsWith("/api/auth/cambiar-clave") && !ruta.endsWith("/api/auth/me")) {
                RespuestaJson.escribir(response, 403, "Forbidden",
                        "Debe cambiar su contraseña temporal antes de continuar");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}