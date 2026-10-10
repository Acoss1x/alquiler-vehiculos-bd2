package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.ClienteEstadoRequest;
import co.edu.unbosque.alquiler.dto.ClienteRequest;
import co.edu.unbosque.alquiler.dto.ClienteResponse;
import co.edu.unbosque.alquiler.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idCliente()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
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
}