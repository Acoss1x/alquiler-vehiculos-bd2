package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.VehiculoEstadoRequest;
import co.edu.unbosque.alquiler.dto.VehiculoRequest;
import co.edu.unbosque.alquiler.dto.VehiculoResponse;
import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import co.edu.unbosque.alquiler.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> registrar(@Valid @RequestBody VehiculoRequest request) {
        VehiculoResponse creado = vehiculoService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idVehiculo()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /**
     * RF-03: vehículos disponibles para un rango de fechas, con filtros opcionales:
     * {@code ?fechaInicio=2026-11-10&fechaFin=2026-11-12&idCategoria=2&idSede=1}.
     */
    @GetMapping("/disponibles")
    public List<VehiculoResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Long idSede) {
        return vehiculoService.disponibles(fechaInicio, fechaFin, idCategoria, idSede);
    }

    @GetMapping("/{id}")
    public VehiculoResponse obtenerPorId(@PathVariable Long id) {
        return vehiculoService.obtenerPorId(id);
    }

    /**
     * Lista vehículos con filtros opcionales: {@code ?idSede=1&estado=DISPONIBLE}.
     */
    @GetMapping
    public Page<VehiculoResponse> listar(@RequestParam(required = false) Long idSede,
                                         @RequestParam(required = false) EstadoVehiculo estado,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return vehiculoService.listar(idSede, estado, pageable);
    }

    @PatchMapping("/{id}/estado")
    public VehiculoResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody VehiculoEstadoRequest request) {
        return vehiculoService.cambiarEstado(id, request.estado());
    }
}