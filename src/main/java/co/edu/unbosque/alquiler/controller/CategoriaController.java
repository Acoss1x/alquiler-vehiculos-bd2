package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CategoriaRequest;
import co.edu.unbosque.alquiler.dto.CategoriaResponse;
import co.edu.unbosque.alquiler.dto.EstadoCatalogoRequest;
import co.edu.unbosque.alquiler.security.Catalogo;
import co.edu.unbosque.alquiler.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse creada = categoriaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.idCategoria()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        Catalogo.exigirAdminSiPideInactivos(jwt, incluirInactivos);
        return categoriaService.listar(incluirInactivos);
    }

    @PatchMapping("/{id}/estado")
    public CategoriaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return categoriaService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }
}