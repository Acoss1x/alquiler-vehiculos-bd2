package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.EmpleadosSedeResponse;
import co.edu.unbosque.alquiler.dto.FlotaEstadoResponse;
import co.edu.unbosque.alquiler.dto.HistorialVehiculoResponse;
import co.edu.unbosque.alquiler.dto.IngresoSedeResponse;
import co.edu.unbosque.alquiler.dto.SeguroRankingResponse;
import co.edu.unbosque.alquiler.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * RF-10. Los roles que entran a cada ruta se definen en SecurityConfig:
 * el historial de un vehículo y el estado de la flota, administrador y técnico;
 * el resto de reportes, solo el administrador.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Alquileres, inspecciones, daños, piezas y penalizaciones de un vehículo. */
    @GetMapping("/vehiculos/{idVehiculo}/historial")
    public HistorialVehiculoResponse historialVehiculo(@PathVariable Long idVehiculo) {
        return reporteService.historialVehiculo(idVehiculo);
    }

    /** Vehículos por estado: {@code ?idSede=1} es opcional. */
    @GetMapping("/flota/estado")
    public List<FlotaEstadoResponse> estadoFlota(@RequestParam(required = false) Long idSede) {
        return reporteService.estadoFlota(idSede);
    }

    /** Ingresos por sede de entrega: {@code ?desde=2026-10-01&hasta=2026-10-31}, ambos opcionales. */
    @GetMapping("/ingresos-por-sede")
    public List<IngresoSedeResponse> ingresosPorSede(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.ingresosPorSede(desde, hasta);
    }

    /** Ranking de seguros: {@code ?limite=5} (1 a 50, por defecto 10). */
    @GetMapping("/seguros-mas-contratados")
    public List<SeguroRankingResponse> segurosMasContratados(@RequestParam(defaultValue = "10") int limite) {
        return reporteService.segurosMasContratados(limite);
    }

    @GetMapping("/empleados-por-sede")
    public List<EmpleadosSedeResponse> empleadosPorSede() {
        return reporteService.empleadosPorSede();
    }
}