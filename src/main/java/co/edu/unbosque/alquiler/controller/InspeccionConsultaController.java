package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.GastoTecnicoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.PiezaFrecuenciaResponse;
import co.edu.unbosque.alquiler.service.InspeccionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inspecciones")
public class InspeccionConsultaController {

    private final InspeccionService inspeccionService;

    public InspeccionConsultaController(InspeccionService inspeccionService) {
        this.inspeccionService = inspeccionService;
    }

    /** Ejemplo: {@code GET /api/inspecciones/buscar?pieza=freno&desde=2026-01-01&hasta=2026-12-31}. */
    @GetMapping("/buscar")
    public List<InspeccionResponse> buscar(
            @RequestParam(required = false) String pieza,
            @RequestParam(required = false) Long idTecnico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return inspeccionService.buscar(pieza, idTecnico, desde, hasta);
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
