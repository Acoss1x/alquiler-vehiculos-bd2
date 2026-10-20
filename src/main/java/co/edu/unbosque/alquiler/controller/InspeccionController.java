package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.PiezaDTO;
import co.edu.unbosque.alquiler.service.InspeccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de las inspecciones de un vehículo (MongoDB). Registrar además deja el
 * vehículo EN_MANTENIMIENTO en PostgreSQL.
 */
@RestController
@RequestMapping("/api/vehiculos/{idVehiculo}/inspecciones")
public class InspeccionController {

    private final InspeccionService inspeccionService;

    public InspeccionController(InspeccionService inspeccionService) {
        this.inspeccionService = inspeccionService;
    }

    @PostMapping
    public ResponseEntity<InspeccionResponse> registrar(@PathVariable Long idVehiculo,
                                                        @Valid @RequestBody InspeccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspeccionService.registrar(idVehiculo, request));
    }

    @GetMapping
    public List<InspeccionResponse> listar(@PathVariable Long idVehiculo) {
        return inspeccionService.listarPorVehiculo(idVehiculo);
    }

    @GetMapping("/costo-total")
    public CostoMantenimientoResponse costoTotal(@PathVariable Long idVehiculo) {
        return inspeccionService.costoMantenimiento(idVehiculo);
    }

    @GetMapping("/{idInspeccion}")
    public InspeccionResponse obtener(@PathVariable Long idVehiculo, @PathVariable String idInspeccion) {
        return inspeccionService.obtener(idVehiculo, idInspeccion);
    }

    @PutMapping("/{idInspeccion}")
    public InspeccionResponse actualizar(@PathVariable Long idVehiculo,
                                         @PathVariable String idInspeccion,
                                         @Valid @RequestBody InspeccionRequest request) {
        return inspeccionService.actualizar(idVehiculo, idInspeccion, request);
    }

    @PostMapping("/{idInspeccion}/piezas")
    public ResponseEntity<InspeccionResponse> agregarPieza(@PathVariable Long idVehiculo,
                                                           @PathVariable String idInspeccion,
                                                           @Valid @RequestBody PiezaDTO pieza) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspeccionService.agregarPieza(idVehiculo, idInspeccion, pieza));
    }

    @DeleteMapping("/{idInspeccion}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idVehiculo, @PathVariable String idInspeccion) {
        inspeccionService.eliminar(idVehiculo, idInspeccion);
        return ResponseEntity.noContent().build();
    }
}