package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.EmpleadoEstadoRequest;
import co.edu.unbosque.alquiler.dto.EmpleadoRequest;
import co.edu.unbosque.alquiler.dto.EmpleadoResponse;
import co.edu.unbosque.alquiler.entity.TipoEmpleado;
import co.edu.unbosque.alquiler.service.EmpleadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody EmpleadoRequest request) {
        EmpleadoResponse creado = empleadoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idEmpleado()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** Ejemplo: {@code GET /api/empleados?tipo=ASESOR&idSede=1}. */
    @GetMapping
    public List<EmpleadoResponse> listar(@RequestParam(required = false) TipoEmpleado tipo,
                                         @RequestParam(required = false) Long idSede) {
        return empleadoService.listar(tipo, idSede);
    }

    @GetMapping("/{id}")
    public EmpleadoResponse obtener(@PathVariable Long id) {
        return empleadoService.obtenerPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public EmpleadoResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody EmpleadoEstadoRequest request) {
        return empleadoService.cambiarEstado(id, request.estado());
    }
}
