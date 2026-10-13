package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.service.InspeccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inspecciones mecánicas de un vehículo (MongoDB) y bloqueo en PostgreSQL.
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
}

