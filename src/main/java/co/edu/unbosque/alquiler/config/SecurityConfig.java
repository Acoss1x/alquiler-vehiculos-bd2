package co.edu.unbosque.alquiler.config;

import co.edu.unbosque.alquiler.security.ClaveTemporalFilter;
import co.edu.unbosque.alquiler.security.RespuestaJson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin sesión: cada petición lleva "Authorization: Bearer <token>".
 * El rol del token (claim "rol") se convierte en la autoridad ROLE_<rol>
 * (CLIENTE, ASESOR, TECNICO o ADMINISTRADOR).
 *
 * Aquí se define QUÉ ROLES entran a cada ruta; las reglas de propiedad (un cliente solo
 * ve lo suyo) se aplican en los controladores. Lo que no esté listado se rechaza
 * (denegado por defecto): al crear un endpoint nuevo hay que agregarlo aquí.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String ASESOR = "ASESOR";
    private static final String TECNICO = "TECNICO";
    private static final String CLIENTE = "CLIENTE";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Públicos: acceso y consulta de catálogo ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/categorias/**", "/api/seguros/**", "/api/sedes/**",
                                "/api/vehiculos/disponibles", "/api/reservas/cotizacion").permitAll()

                        // --- Cuenta propia (cualquier persona con sesión) ---
                        .requestMatchers("/api/auth/**").authenticated()

                        // --- Catálogos: solo el administrador modifica ---
                        .requestMatchers(HttpMethod.POST, "/api/sedes", "/api/categorias", "/api/seguros").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/sedes/*", "/api/categorias/*", "/api/seguros/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/sedes/*/estado", "/api/categorias/*/estado",
                                "/api/seguros/*/estado").hasRole(ADMIN)

                        // --- Vehículos ---
                        .requestMatchers(HttpMethod.POST, "/api/vehiculos").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/*/estado").hasAnyRole(ADMIN, TECNICO)
                        .requestMatchers(HttpMethod.GET, "/api/vehiculos").hasAnyRole(ADMIN, ASESOR, TECNICO)
                        .requestMatchers(HttpMethod.GET, "/api/vehiculos/*").authenticated()

                        // --- Inspecciones (MongoDB) ---
                        .requestMatchers(HttpMethod.GET, "/api/inspecciones/fotos/*", "/api/inspecciones/reserva/*")
                        .hasAnyRole(ADMIN, ASESOR, TECNICO, CLIENTE)
                        .requestMatchers("/api/vehiculos/*/inspecciones/**").hasAnyRole(ADMIN, ASESOR, TECNICO)
                        .requestMatchers("/api/inspecciones/**").hasAnyRole(ADMIN, TECNICO)

                        // --- Clientes y licencias ---
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasAnyRole(ADMIN, ASESOR)
                        .requestMatchers(HttpMethod.GET, "/api/clientes").hasAnyRole(ADMIN, ASESOR)
                        .requestMatchers(HttpMethod.GET, "/api/clientes/documento/*").hasAnyRole(ADMIN, ASESOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/estado").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/clientes/*", "/api/clientes/*/licencia")
                        .hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/*").hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/api/clientes/*/licencia").hasAnyRole(ADMIN, ASESOR, CLIENTE)

                        // --- Empleados ---
                        .requestMatchers("/api/empleados/**").hasRole(ADMIN)

                        // --- Reservas y pagos ---
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.GET, "/api/reservas", "/api/reservas/*", "/api/reservas/*/pagos")
                        .hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/pagos", "/api/reservas/*/cancelar")
                        .hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/iniciar", "/api/reservas/*/finalizar")
                        .hasAnyRole(ADMIN, ASESOR)

                        // --- Penalizaciones por daños ---
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/penalizaciones").hasAnyRole(ADMIN, ASESOR)
                        .requestMatchers(HttpMethod.GET, "/api/reservas/*/penalizaciones", "/api/clientes/*/penalizaciones")
                        .hasAnyRole(ADMIN, ASESOR, CLIENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/penalizaciones/*/estado").hasRole(ADMIN)

                        // --- Reportes (RF-10) ---
                        .requestMatchers(HttpMethod.GET, "/api/reportes/vehiculos/*/historial", "/api/reportes/flota/estado")
                        .hasAnyRole(ADMIN, TECNICO)
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasRole(ADMIN)

                        // --- Todo lo demás: denegado ---
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint((request, response, ex) -> RespuestaJson.escribir(
                                response, 401, "Unauthorized", "Autenticación requerida o token inválido"))
                        .accessDeniedHandler((request, response, ex) -> RespuestaJson.escribir(
                                response, 403, "Forbidden", "No tiene permisos para esta operación")))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> RespuestaJson.escribir(
                                response, 401, "Unauthorized", "Autenticación requerida o token inválido"))
                        .accessDeniedHandler((request, response, e) -> RespuestaJson.escribir(
                                response, 403, "Forbidden", "No tiene permisos para esta operación")))
                .addFilterAfter(new ClaveTemporalFilter(), BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}