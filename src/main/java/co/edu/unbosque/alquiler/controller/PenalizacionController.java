package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.PenalizacionEstadoRequest;
import co.edu.unbosque.alquiler.dto.PenalizacionRequest;
import co.edu.unbosque.alquiler.dto.PenalizacionResponse;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.PenalizacionService;
import co.edu.unbosque.alquiler.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Los roles que entran a cada ruta se definen en SecurityConfig; aquí se aplica la
 * regla de propiedad: un cliente solo ve las penalizaciones de sus propias reservas.
 */
@RestController
@RequestMapping("/api")
public class PenalizacionController {

    private final PenalizacionService penalizacionService;
    private final ReservaService reservaService;

    public PenalizacionController(PenalizacionService penalizacionService, ReservaService reservaService) {
        this.penalizacionService = penalizacionService;
        this.reservaService = reservaService;
    }

    @PostMapping("/reservas/{idReserva}/penalizaciones")
    public ResponseEntity<PenalizacionResponse> registrar(@PathVariable Long idReserva,
                                                          @Valid @RequestBody PenalizacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(penalizacionService.registrar(idReserva, request));
    }

    @GetMapping("/reservas/{idReserva}/penalizaciones")
    public List<PenalizacionResponse> listarPorReserva(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable Long idReserva) {
        if (Sesion.esCliente(jwt) && !reservaService.perteneceACliente(idReserva, Sesion.id(jwt))) {
            throw new AccessDeniedException("No tiene permisos sobre esta reserva");
        }
        return penalizacionService.listarPorReserva(idReserva);
    }

    @GetMapping("/clientes/{idCliente}/penalizaciones")
    public List<PenalizacionResponse> listarPorCliente(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable Long idCliente) {
        Sesion.exigirPropioOPersonal(jwt, idCliente);
        return penalizacionService.listarPorCliente(idCliente);
    }

    @PatchMapping("/penalizaciones/{id}/estado")
    public PenalizacionResponse cambiarEstado(@PathVariable Long id,
                                              @Valid @RequestBody PenalizacionEstadoRequest request) {
        return penalizacionService.cambiarEstado(id, request.estado());
    }
}
