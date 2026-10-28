package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.document.TipoInspeccion;
import co.edu.unbosque.alquiler.dto.GastoTecnicoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.PiezaFrecuenciaResponse;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.InspeccionFotoService;
import co.edu.unbosque.alquiler.service.InspeccionService;
import co.edu.unbosque.alquiler.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inspecciones")
public class InspeccionConsultaController {

    private final InspeccionService inspeccionService;
    private final InspeccionFotoService fotoService;
    private final ReservaService reservaService;

    public InspeccionConsultaController(InspeccionService inspeccionService,
                                        InspeccionFotoService fotoService,
                                        ReservaService reservaService) {
        this.inspeccionService = inspeccionService;
        this.fotoService = fotoService;
        this.reservaService = reservaService;
    }

    /** Ejemplo: {@code GET /api/inspecciones/buscar?pieza=freno&tipo=MANTENIMIENTO&desde=2026-01-01}. */
    @GetMapping("/buscar")
    public List<InspeccionResponse> buscar(
            @RequestParam(required = false) String pieza,
            @RequestParam(required = false) Long idEmpleado,
            @RequestParam(required = false) TipoInspeccion tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return inspeccionService.buscar(pieza, idEmpleado, tipo, desde, hasta);
    }

    /** Entrega y devolución de una reserva. Un cliente solo ve las de sus reservas. */
    @GetMapping("/reserva/{idReserva}")
    public List<InspeccionResponse> porReserva(@AuthenticationPrincipal Jwt jwt, @PathVariable Long idReserva) {
        if (Sesion.esCliente(jwt) && !reservaService.perteneceACliente(idReserva, Sesion.id(jwt))) {
            throw new AccessDeniedException("No tiene permisos sobre esta reserva");
        }
        return inspeccionService.listarPorReserva(idReserva);
    }

    /** Descarga una foto. Un cliente solo accede a las de inspecciones de sus reservas. */
    @GetMapping("/fotos/{idFoto}")
    public ResponseEntity<byte[]> foto(@AuthenticationPrincipal Jwt jwt, @PathVariable String idFoto) {
        InspeccionResponse inspeccion = inspeccionService.obtenerPorFoto(idFoto);   // 404 si ninguna la referencia
        if (Sesion.esCliente(jwt) && (inspeccion.idReserva() == null
                || !reservaService.perteneceACliente(inspeccion.idReserva(), Sesion.id(jwt)))) {
            throw new AccessDeniedException("No tiene permisos sobre esta foto");
        }
        InspeccionFotoService.Archivo archivo = fotoService.obtener(idFoto);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .body(archivo.datos());
    }

    @GetMapping("/estadisticas/piezas-mas-cambiadas")
    public List<PiezaFrecuenciaResponse> piezasMasCambiadas(@RequestParam(defaultValue = "5") int limite) {
        return inspeccionService.piezasMasCambiadas(limite);
    }

    @GetMapping("/estadisticas/gasto-por-tecnico")
    public List<GastoTecnicoResponse> gastoPorTecnico() {
        return inspeccionService.gastoPorTecnico();
    }
}