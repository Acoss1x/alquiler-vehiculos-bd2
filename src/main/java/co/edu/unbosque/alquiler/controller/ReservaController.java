package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CotizacionResponse;
import co.edu.unbosque.alquiler.dto.IniciarReservaRequest;
import co.edu.unbosque.alquiler.dto.PagoRequest;
import co.edu.unbosque.alquiler.dto.PagoResponse;
import co.edu.unbosque.alquiler.dto.ReservaRequest;
import co.edu.unbosque.alquiler.dto.ReservaResponse;
import co.edu.unbosque.alquiler.dto.ReservaResumenResponse;
import co.edu.unbosque.alquiler.entity.EstadoReserva;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Los roles que pueden entrar a cada ruta se definen en SecurityConfig; aquí se aplican
 * las reglas de propiedad: un cliente solo ve y opera sobre sus propias reservas.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ReservaRequest request) {
        if (Sesion.esCliente(jwt)) {
            Sesion.exigirPropioOPersonal(jwt, request.idCliente());
            if (request.idAsesor() != null) {
                throw new AccessDeniedException("Un cliente no puede asignar un asesor a su reserva");
            }
        }
        ReservaResponse creada = reservaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.idReserva()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtenerPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        ReservaResponse reserva = reservaService.obtenerPorId(id);
        Sesion.exigirPropioOPersonal(jwt, reserva.idCliente());
        return reserva;
    }

    /**
     * Listado con filtros opcionales: {@code ?idCliente=1&estado=CONFIRMADA}.
     * Un cliente siempre ve solo las suyas, sin importar el filtro que envíe.
     */
    @GetMapping
    public Page<ReservaResumenResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) Long idCliente,
                                               @RequestParam(required = false) EstadoReserva estado,
                                               @PageableDefault(size = 20) Pageable pageable) {
        Long filtroCliente = Sesion.esCliente(jwt) ? Sesion.id(jwt) : idCliente;
        return reservaService.listar(filtroCliente, estado, pageable);
    }

    /**
     * RF-07: {@code /api/reservas/cotizacion?idVehiculo=1&fechaInicio=2026-11-10&fechaFin=2026-11-12&idsSeguros=1,2}.
     */
    @GetMapping("/cotizacion")
    public CotizacionResponse cotizar(
            @RequestParam Long idVehiculo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) List<Long> idsSeguros) {
        return reservaService.cotizar(idVehiculo, fechaInicio, fechaFin, idsSeguros);
    }

    @GetMapping("/{id}/pagos")
    public List<PagoResponse> listarPagos(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        exigirReservaPropia(jwt, id);
        return reservaService.listarPagos(id);
    }

    /** Registra un pago (abono o saldo) y devuelve la reserva actualizada. */
    @PostMapping("/{id}/pagos")
    public ResponseEntity<ReservaResponse> registrarPago(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody PagoRequest request) {
        exigirReservaPropia(jwt, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.registrarPago(id, request));
    }

    /** Entrega del vehículo: un asesor solo puede registrarla a su propio nombre; un administrador, a nombre de cualquiera. */
    @PostMapping("/{id}/iniciar")
    public ReservaResponse iniciar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody IniciarReservaRequest request) {
        if ("ASESOR".equals(Sesion.rol(jwt)) && !Sesion.id(jwt).equals(request.idAsesor())) {
            throw new AccessDeniedException("Un asesor solo puede registrar entregas a su nombre");
        }
        return reservaService.iniciar(id, request.idAsesor());
    }

    @PostMapping("/{id}/finalizar")
    public ReservaResponse finalizar(@PathVariable Long id) {
        return reservaService.finalizar(id);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        exigirReservaPropia(jwt, id);
        return reservaService.cancelar(id);
    }

    /** Un cliente solo puede operar sobre sus reservas; el personal, sobre cualquiera. */
    private void exigirReservaPropia(Jwt jwt, Long idReserva) {
        if (Sesion.esCliente(jwt) && !reservaService.perteneceACliente(idReserva, Sesion.id(jwt))) {
            throw new AccessDeniedException("No tiene permisos sobre esta reserva");
        }
    }
}