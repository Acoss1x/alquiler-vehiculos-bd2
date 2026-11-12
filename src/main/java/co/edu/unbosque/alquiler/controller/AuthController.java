package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CambiarClaveRequest;
import co.edu.unbosque.alquiler.dto.LoginRequest;
import co.edu.unbosque.alquiler.dto.LoginResponse;
import co.edu.unbosque.alquiler.dto.OlvideClaveRequest;
import co.edu.unbosque.alquiler.dto.PerfilResponse;
import co.edu.unbosque.alquiler.dto.RegistroRequest;
import co.edu.unbosque.alquiler.dto.RestablecerClaveRequest;
import co.edu.unbosque.alquiler.service.AuthService;
import co.edu.unbosque.alquiler.service.RecuperacionClaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionClaveService recuperacionService;

    public AuthController(AuthService authService, RecuperacionClaveService recuperacionService) {
        this.authService = authService;
        this.recuperacionService = recuperacionService;
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


    @PostMapping("/olvide-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void olvideClave(@Valid @RequestBody OlvideClaveRequest request) {
        recuperacionService.olvideClave(request.correo());
    }


    @PostMapping("/restablecer-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerClave(@Valid @RequestBody RestablecerClaveRequest request) {
        recuperacionService.restablecer(request);
    }
}
