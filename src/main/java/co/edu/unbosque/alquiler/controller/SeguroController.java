package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.EstadoCatalogoRequest;
import co.edu.unbosque.alquiler.dto.SeguroRequest;
import co.edu.unbosque.alquiler.dto.SeguroResponse;
import co.edu.unbosque.alquiler.security.Catalogo;
import co.edu.unbosque.alquiler.service.SeguroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/seguros")
public class SeguroController {

    private final SeguroService seguroService;

    public SeguroController(SeguroService seguroService) {
        this.seguroService = seguroService;
    }

    @GetMapping
    public List<SeguroResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        Catalogo.exigirAdminSiPideInactivos(jwt, incluirInactivos);
        return seguroService.listar(incluirInactivos);
    }

    @GetMapping("/{id}")
    public SeguroResponse obtener(@PathVariable Long id) {
        return seguroService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SeguroResponse> crear(@Valid @RequestBody SeguroRequest request) {
        SeguroResponse creado = seguroService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idSeguro()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public SeguroResponse actualizar(@PathVariable Long id, @Valid @RequestBody SeguroRequest request) {
        return seguroService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public SeguroResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return seguroService.cambiarEstado(id, request.estado());
    }
}