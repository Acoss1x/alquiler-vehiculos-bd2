package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.service.VehiculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;


    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    /**
     * Endpoint para registrar una inspección.
     * Recibe un JSON desde el cliente y ejecuta la lógica híbrida.
     */
    @PostMapping("/{idVehiculo}/inspeccion")
    public ResponseEntity<String> registrarInspeccion(
            @PathVariable Long idVehiculo,
            @RequestBody Map<String, Object> payload) {

        try {

            String mecanico = (String) payload.get("mecanico");
            String notas = (String) payload.get("notas");
            List<String> piezas = (List<String>) payload.get("piezas");

            vehiculoService.registrarInspeccionYBloquearVehiculo(idVehiculo, mecanico, notas, piezas);

            return ResponseEntity.ok("Éxito: Inspección registrada en MongoDB y vehículo bloqueado en PostgreSQL.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al procesar la solicitud: " + e.getMessage());
        }
    }
}
