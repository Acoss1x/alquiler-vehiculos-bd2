package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
        Long idReserva,
        String estadoReserva,
        LocalDateTime fechaCreacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal costoTotal,
        BigDecimal saldoPendiente,
        Long idCliente,
        String nombreCliente,
        Long idVehiculo,
        String placa,
        Long idAsesor,
        String nombreAsesor,
        Long idSedeEntrega,
        String nombreSedeEntrega,
        Long idSedeRecibe,
        String nombreSedeRecibe,
        List<SeguroContratadoResponse> seguros,
        List<PagoResponse> pagos
) {
}
