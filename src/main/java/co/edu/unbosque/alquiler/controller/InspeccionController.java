package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.document.TipoInspeccion;
import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.InspeccionUpdateRequest;
import co.edu.unbosque.alquiler.dto.PiezaDTO;
import co.edu.unbosque.alquiler.security.Sesion;
import co.edu.unbosque.alquiler.service.InspeccionFotoService;
import co.edu.unbosque.alquiler.service.InspeccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Inspecciones de un vehículo (MongoDB). Mantenimiento: técnico o administrador; entrega
 * y devolución: asesor o administrador. Modificar o borrar: quien la registró o un administrador.
 * Registrar un mantenimiento además deja el vehículo EN_MANTENIMIENTO en PostgreSQL.
 */
@RestController
@RequestMapping("/api/vehiculos/{idVehiculo}/inspecciones")
public class InspeccionController {

    private final InspeccionService inspeccionService;
    private final InspeccionFotoService fotoService;

    public InspeccionController(InspeccionService inspeccionService, InspeccionFotoService fotoService) {
        this.inspeccionService = inspeccionService;
        this.fotoService = fotoService;
    }

    @PostMapping
    public ResponseEntity<InspeccionResponse> registrar(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Long idVehiculo,
                                                        @Valid @RequestBody InspeccionRequest request) {
        exigirRolSegunTipo(jwt, request.tipo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspeccionService.registrar(idVehiculo, Sesion.id(jwt), request));
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
    public InspeccionResponse actualizar(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable Long idVehiculo,
                                         @PathVariable String idInspeccion,
                                         @Valid @RequestBody InspeccionUpdateRequest request) {
        exigirAutorOAdmin(jwt, idVehiculo, idInspeccion);
        return inspeccionService.actualizar(idVehiculo, idInspeccion, request);
    }

    @PostMapping("/{idInspeccion}/piezas")
    public ResponseEntity<InspeccionResponse> agregarPieza(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long idVehiculo,
                                                           @PathVariable String idInspeccion,
                                                           @Valid @RequestBody PiezaDTO pieza) {
        exigirAutorOAdmin(jwt, idVehiculo, idInspeccion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspeccionService.agregarPieza(idVehiculo, idInspeccion, pieza));
    }

    @DeleteMapping("/{idInspeccion}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable Long idVehiculo,
                                         @PathVariable String idInspeccion) {
        exigirAutorOAdmin(jwt, idVehiculo, idInspeccion);
        inspeccionService.eliminar(idVehiculo, idInspeccion);
        return ResponseEntity.noContent().build();
    }

    /** Sube una foto (campo multipart {@code archivo}); devuelve la inspección con su lista de fotos. */
    @PostMapping(path = "/{idInspeccion}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InspeccionResponse> subirFoto(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Long idVehiculo,
                                                        @PathVariable String idInspeccion,
                                                        @RequestParam("archivo") MultipartFile archivo) {
        exigirAutorOAdmin(jwt, idVehiculo, idInspeccion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.agregar(idVehiculo, idInspeccion, archivo));
    }

    @DeleteMapping("/{idInspeccion}/fotos/{idFoto}")
    public InspeccionResponse quitarFoto(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable Long idVehiculo,
                                         @PathVariable String idInspeccion,
                                         @PathVariable String idFoto) {
        exigirAutorOAdmin(jwt, idVehiculo, idInspeccion);
        return fotoService.quitar(idVehiculo, idInspeccion, idFoto);
    }

    private void exigirRolSegunTipo(Jwt jwt, TipoInspeccion tipo) {
        if (Sesion.esAdministrador(jwt)) {
            return;
        }
        String esperado = tipo == TipoInspeccion.MANTENIMIENTO ? "TECNICO" : "ASESOR";
        if (!esperado.equals(Sesion.rol(jwt))) {
            throw new AccessDeniedException("Una inspección de " + tipo + " la registra un " + esperado.toLowerCase()
                    + " o un administrador");
        }
    }

    private void exigirAutorOAdmin(Jwt jwt, Long idVehiculo, String idInspeccion) {
        if (Sesion.esAdministrador(jwt)) {
            return;
        }
        InspeccionResponse inspeccion = inspeccionService.obtener(idVehiculo, idInspeccion);
        if (!Sesion.id(jwt).equals(inspeccion.idEmpleado())) {
            throw new AccessDeniedException("Solo quien registró la inspección o un administrador puede modificarla");
        }
    }
}