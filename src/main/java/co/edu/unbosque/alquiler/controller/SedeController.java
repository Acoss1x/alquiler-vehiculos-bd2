package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.EstadoCatalogoRequest;
import co.edu.unbosque.alquiler.dto.SedeRequest;
import co.edu.unbosque.alquiler.dto.SedeResponse;
import co.edu.unbosque.alquiler.security.Catalogo;
import co.edu.unbosque.alquiler.service.SedeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    /** Pública: solo sedes activas. {@code ?incluirInactivos=true} es solo para el administrador. */
    @GetMapping
    public List<SedeResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        Catalogo.exigirAdminSiPideInactivos(jwt, incluirInactivos);
        return sedeService.listar(incluirInactivos);
    }

    @GetMapping("/{id}")
    public SedeResponse obtener(@PathVariable Long id) {
        return sedeService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody SedeRequest request) {
        SedeResponse creada = sedeService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.idSede()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public SedeResponse actualizar(@PathVariable Long id, @Valid @RequestBody SedeRequest request) {
        return sedeService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public SedeResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return sedeService.cambiarEstado(id, request.estado());
    }
}
