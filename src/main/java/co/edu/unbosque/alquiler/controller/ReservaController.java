package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.CotizacionResponse;
import co.edu.unbosque.alquiler.dto.PagoRequest;
import co.edu.unbosque.alquiler.dto.PagoResponse;
import co.edu.unbosque.alquiler.dto.ReservaRequest;
import co.edu.unbosque.alquiler.dto.ReservaResponse;
import co.edu.unbosque.alquiler.dto.ReservaResumenResponse;
import co.edu.unbosque.alquiler.entity.EstadoReserva;
import co.edu.unbosque.alquiler.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
        ReservaResponse creada = reservaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.idReserva()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtenerPorId(@PathVariable Long id) {
        return reservaService.obtenerPorId(id);
    }

    /** Listado con filtros opcionales: {@code ?idCliente=1&estado=CONFIRMADA}. */
    @GetMapping
    public Page<ReservaResumenResponse> listar(@RequestParam(required = false) Long idCliente,
                                               @RequestParam(required = false) EstadoReserva estado,
                                               @PageableDefault(size = 20) Pageable pageable) {
        return reservaService.listar(idCliente, estado, pageable);
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
    public List<PagoResponse> listarPagos(@PathVariable Long id) {
        return reservaService.listarPagos(id);
    }

    /** Registra un pago (abono o saldo) y devuelve la reserva actualizada. */
    @PostMapping("/{id}/pagos")
    public ResponseEntity<ReservaResponse> registrarPago(@PathVariable Long id,
                                                         @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.registrarPago(id, request));
    }

    @PostMapping("/{id}/iniciar")
    public ReservaResponse iniciar(@PathVariable Long id) {
        return reservaService.iniciar(id);
    }

    @PostMapping("/{id}/finalizar")
    public ReservaResponse finalizar(@PathVariable Long id) {
        return reservaService.finalizar(id);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return reservaService.cancelar(id);
    }
}
