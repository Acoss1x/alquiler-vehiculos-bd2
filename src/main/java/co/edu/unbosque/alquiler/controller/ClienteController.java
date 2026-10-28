package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.ClienteEstadoRequest;
import co.edu.unbosque.alquiler.dto.ClienteRequest;
import co.edu.unbosque.alquiler.dto.ClienteResponse;
import co.edu.unbosque.alquiler.dto.ClienteUpdateRequest;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Los roles que pueden entrar a cada ruta se definen en SecurityConfig; aquí se aplica
 * la regla de propiedad: un cliente solo puede ver y modificar sus propios datos.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /** Registro hecho por un empleado en el mostrador (el cliente que se registra solo usa /api/auth/registro). */
    @PostMapping
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.registrar(request, true);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idCliente()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Sesion.exigirPropioOPersonal(jwt, id);
        return clienteService.obtenerPorId(id);
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ClienteResponse buscarPorDocumento(@PathVariable String numeroDocumento) {
        return clienteService.buscarPorDocumento(numeroDocumento);
    }

    @GetMapping
    public Page<ClienteResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return clienteService.listar(pageable);
    }

    @PatchMapping("/{id}/estado")
    public ClienteResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody ClienteEstadoRequest request) {
        return clienteService.cambiarEstado(id, request.estado());
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody ClienteUpdateRequest request) {
        Sesion.exigirPropioOPersonal(jwt, id);
        return clienteService.actualizar(id, request);
    }
}