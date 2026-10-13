package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.LicenciaRequest;
import co.edu.unbosque.alquiler.dto.LicenciaResponse;
import co.edu.unbosque.alquiler.service.LicenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/clientes/{idCliente}/licencia")
public class LicenciaController {

    private final LicenciaService licenciaService;

    public LicenciaController(LicenciaService licenciaService) {
        this.licenciaService = licenciaService;
    }

    @PostMapping
    public ResponseEntity<LicenciaResponse> registrar(@PathVariable Long idCliente,
                                                      @Valid @RequestBody LicenciaRequest request) {
        LicenciaResponse creada = licenciaService.registrar(idCliente, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public LicenciaResponse obtener(@PathVariable Long idCliente) {
        return licenciaService.obtenerPorCliente(idCliente);
    }
}
