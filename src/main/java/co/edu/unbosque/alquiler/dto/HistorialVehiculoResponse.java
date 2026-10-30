package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-10: historial consolidado de un vehículo para decidir su mantenimiento.
 * Junta datos de PostgreSQL (reservas, pagos, penalizaciones) y de MongoDB
 * (inspecciones, daños y piezas cambiadas).
 */
public record HistorialVehiculoResponse(
        VehiculoResponse vehiculo,
        Resumen resumen,
        List<ReservaHistorial> reservas,
        List<InspeccionResponse> inspecciones,
        List<PenalizacionResponse> penalizaciones
) {

    public record Resumen(
            long totalReservas,
            long reservasFinalizadas,
            long reservasCanceladas,
            BigDecimal ingresosPorAlquiler,
            long totalInspecciones,
            long totalDanosReportados,
            long totalPiezasCambiadas,
            BigDecimal costoMantenimiento,
            BigDecimal totalPenalizaciones
    ) {
    }

    public record ReservaHistorial(
            Long idReserva,
            String estadoReserva,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal costoTotal,
            Long idCliente,
            String nombreCliente,
            String sedeEntrega,
            String sedeRecibe
    ) {
    }
}