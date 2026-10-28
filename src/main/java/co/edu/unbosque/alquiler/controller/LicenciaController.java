package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.LicenciaRequest;
import co.edu.unbosque.alquiler.dto.LicenciaResponse;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.LicenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Un cliente solo puede registrar y consultar su propia licencia; el personal autorizado, la de cualquiera.
 */
@RestController
@RequestMapping("/api/clientes/{idCliente}/licencia")
public class LicenciaController {

    private final LicenciaService licenciaService;

    public LicenciaController(LicenciaService licenciaService) {
        this.licenciaService = licenciaService;
    }

    @PostMapping
    public ResponseEntity<LicenciaResponse> registrar(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long idCliente,
                                                      @Valid @RequestBody LicenciaRequest request) {
        Sesion.exigirPropioOPersonal(jwt, idCliente);
        LicenciaResponse creada = licenciaService.registrar(idCliente, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public LicenciaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long idCliente) {
        Sesion.exigirPropioOPersonal(jwt, idCliente);
        return licenciaService.obtenerPorCliente(idCliente);
    }
}