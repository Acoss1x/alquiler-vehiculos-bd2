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
 * Públicos: login, registro y el catálogo de consulta. Todo lo demás exige token.
 * El rol del token (claim "rol") se convierte en la autoridad ROLE_<rol>.
 */
@Configuration
public class SecurityConfig {

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
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/categorias/**", "/api/seguros/**",
                                "/api/vehiculos/disponibles", "/api/reservas/cotizacion").permitAll()
                        .anyRequest().authenticated())
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