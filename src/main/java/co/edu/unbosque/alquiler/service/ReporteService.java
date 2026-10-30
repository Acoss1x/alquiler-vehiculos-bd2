package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.DanoDTO;
import co.edu.unbosque.alquiler.dto.EmpleadosSedeResponse;
import co.edu.unbosque.alquiler.dto.FlotaEstadoResponse;
import co.edu.unbosque.alquiler.dto.HistorialVehiculoResponse;
import co.edu.unbosque.alquiler.dto.HistorialVehiculoResponse.ReservaHistorial;
import co.edu.unbosque.alquiler.dto.HistorialVehiculoResponse.Resumen;
import co.edu.unbosque.alquiler.dto.IngresoSedeResponse;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.PenalizacionResponse;
import co.edu.unbosque.alquiler.dto.PiezaDTO;
import co.edu.unbosque.alquiler.dto.SeguroRankingResponse;
import co.edu.unbosque.alquiler.dto.VehiculoResponse;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF-10: reportes que combinan PostgreSQL (transacciones) y MongoDB (inspecciones). */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final int LIMITE_MAXIMO = 50;

    private final ReporteRepository reporteRepository;
    private final VehiculoService vehiculoService;
    private final InspeccionService inspeccionService;

    public ReporteService(ReporteRepository reporteRepository,
                          VehiculoService vehiculoService,
                          InspeccionService inspeccionService) {
        this.reporteRepository = reporteRepository;
        this.vehiculoService = vehiculoService;
        this.inspeccionService = inspeccionService;
    }

    /** Historial de alquileres, daños, revisiones y penalizaciones de un vehículo. */
    public HistorialVehiculoResponse historialVehiculo(Long idVehiculo) {
        VehiculoResponse vehiculo = vehiculoService.obtenerPorId(idVehiculo);
        List<ReservaHistorial> reservas = reporteRepository.reservasDeVehiculo(idVehiculo);
        List<InspeccionResponse> inspecciones = inspeccionService.listarPorVehiculo(idVehiculo);
        List<PenalizacionResponse> penalizaciones = reporteRepository.penalizacionesDeVehiculo(idVehiculo);

        long finalizadas = reservas.stream().filter(r -> "FINALIZADA".equals(r.estadoReserva())).count();
        long canceladas = reservas.stream().filter(r -> "CANCELADA".equals(r.estadoReserva())).count();

        long danos = inspecciones.stream()
                .map(InspeccionResponse::danos)
                .filter(l -> l != null)
                .mapToLong(List::size)
                .sum();
        long piezas = inspecciones.stream()
                .map(InspeccionResponse::piezasCambiadas)
                .filter(l -> l != null)
                .flatMap(List::stream)
                .mapToLong(PiezaDTO::cantidad)
                .sum();

        BigDecimal penalizado = penalizaciones.stream()
                .filter(p -> !"ANULADA".equals(p.estadoCargo()))
                .map(PenalizacionResponse::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Resumen resumen = new Resumen(
                reservas.size(),
                finalizadas,
                canceladas,
                reporteRepository.ingresosDeVehiculo(idVehiculo),
                inspecciones.size(),
                danos,
                piezas,
                inspeccionService.costoMantenimiento(idVehiculo).costoTotal(),
                penalizado);

        return new HistorialVehiculoResponse(vehiculo, resumen, reservas, inspecciones, penalizaciones);
    }

    public List<FlotaEstadoResponse> estadoFlota(Long idSede) {
        return reporteRepository.estadoFlota(idSede);
    }

    public List<IngresoSedeResponse> ingresosPorSede(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return reporteRepository.ingresosPorSede(desde, hasta);
    }

    public List<SeguroRankingResponse> segurosMasContratados(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new SolicitudInvalidaException("El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return reporteRepository.segurosMasContratados(limite);
    }

    public List<EmpleadosSedeResponse> empleadosPorSede() {
        return reporteRepository.empleadosPorSede();
    }
}