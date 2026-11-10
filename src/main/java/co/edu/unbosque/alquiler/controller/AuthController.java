package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CambiarClaveRequest;
import co.edu.unbosque.alquiler.dto.LoginRequest;
import co.edu.unbosque.alquiler.dto.LoginResponse;
import co.edu.unbosque.alquiler.dto.PerfilResponse;
import co.edu.unbosque.alquiler.dto.RecuperarClaveRequest;
import co.edu.unbosque.alquiler.dto.RegistroRequest;
import co.edu.unbosque.alquiler.exception.ApiError;
import co.edu.unbosque.alquiler.exception.DemasiadosIntentosException;
import co.edu.unbosque.alquiler.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Autenticación: login, registro y recuperación de clave son públicos; el resto exige el token (Bearer).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registro(request);
    }

    /** Perfil actual, leído de la base de datos (refleja cambios de estado). */
    @GetMapping("/me")
    public PerfilResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.perfil(jwt.getClaimAsString("tipo"), Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/cambiar-clave")
    public LoginResponse cambiarClave(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CambiarClaveRequest request) {
        return authService.cambiarClave(jwt.getClaimAsString("tipo"), Long.valueOf(jwt.getSubject()), request);
    }

    /** Pública: la persona que olvidó su clave la redefine con su correo y su número de documento. */
    @PostMapping("/recuperar-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarClave(@Valid @RequestBody RecuperarClaveRequest request) {
        authService.recuperarClave(request);
    }

    /** Demasiados intentos fallidos de recuperación sobre el mismo correo. */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ApiError> demasiadosIntentos(DemasiadosIntentosException ex) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .body(new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), ex.getMessage(), null));
    }
}