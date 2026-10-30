package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.dto.EmpleadosSedeResponse;
import co.edu.unbosque.alquiler.dto.FlotaEstadoResponse;
import co.edu.unbosque.alquiler.dto.HistorialVehiculoResponse.ReservaHistorial;
import co.edu.unbosque.alquiler.dto.IngresoSedeResponse;
import co.edu.unbosque.alquiler.dto.PenalizacionResponse;
import co.edu.unbosque.alquiler.dto.SeguroRankingResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Consultas de reporte (RF-10) sobre PostgreSQL: JOIN, GROUP BY y funciones de agregación.
 * Son solo de lectura; las de MongoDB (daños, piezas, costo de mantenimiento) las aporta
 * InspeccionService.
 */
@Repository
public class ReporteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReporteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Todas las reservas de un vehículo, de la más reciente a la más antigua. */
    public List<ReservaHistorial> reservasDeVehiculo(Long idVehiculo) {
        String sql = """
                SELECT r.id_reserva, r.estado_reserva, r.fecha_inicio, r.fecha_fin, r.costo_total,
                       c.id_cliente,
                       c.primer_nombre || ' ' || c.primer_apellido AS nombre_cliente,
                       se.nombre_sede AS sede_entrega,
                       sr.nombre_sede AS sede_recibe
                  FROM reserva r
                  JOIN cliente c  ON c.id_cliente = r.cliente_id_cliente
                  JOIN sede se    ON se.id_sede   = r.sede_entrega
                  JOIN sede sr    ON sr.id_sede   = r.sede_recibe
                 WHERE r.vehiculo_id_vehiculo = :idVehiculo
                 ORDER BY r.fecha_inicio DESC, r.id_reserva DESC
                """;
        return jdbc.query(sql, Map.of("idVehiculo", idVehiculo), (rs, n) -> new ReservaHistorial(
                rs.getLong("id_reserva"),
                rs.getString("estado_reserva"),
                rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class),
                rs.getBigDecimal("costo_total"),
                rs.getLong("id_cliente"),
                rs.getString("nombre_cliente"),
                rs.getString("sede_entrega"),
                rs.getString("sede_recibe")));
    }

    /** Suma de los pagos aprobados de todas las reservas del vehículo (los reembolsados no cuentan). */
    public BigDecimal ingresosDeVehiculo(Long idVehiculo) {
        String sql = """
                SELECT COALESCE(SUM(p.monto), 0)
                  FROM pago p
                  JOIN reserva r ON r.id_reserva = p.reserva_id_reserva
                 WHERE r.vehiculo_id_vehiculo = :idVehiculo
                   AND p.estado_pago = 'APROBADO'
                """;
        BigDecimal total = jdbc.queryForObject(sql, Map.of("idVehiculo", idVehiculo), BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Penalizaciones por daños cobradas en las reservas del vehículo. */
    public List<PenalizacionResponse> penalizacionesDeVehiculo(Long idVehiculo) {
        String sql = """
                SELECT pe.id_penalizacion, pe.reserva_id_reserva, pe.fecha_cargo, pe.concepto,
                       pe.monto, pe.estado_cargo, pe.id_inspeccion_mongo
                  FROM penalizacion pe
                  JOIN reserva r ON r.id_reserva = pe.reserva_id_reserva
                 WHERE r.vehiculo_id_vehiculo = :idVehiculo
                 ORDER BY pe.id_penalizacion DESC
                """;
        return jdbc.query(sql, Map.of("idVehiculo", idVehiculo), (rs, n) -> new PenalizacionResponse(
                rs.getLong("id_penalizacion"),
                rs.getLong("reserva_id_reserva"),
                rs.getObject("fecha_cargo", LocalDate.class),
                rs.getString("concepto"),
                rs.getBigDecimal("monto"),
                rs.getString("estado_cargo"),
                rs.getString("id_inspeccion_mongo")));
    }

    /** Vehículos por estado, incluyendo los estados en cero. {@code idSede} es opcional. */
    public List<FlotaEstadoResponse> estadoFlota(Long idSede) {
        String sql = """
                SELECT e.estado, COUNT(v.id_vehiculo) AS cantidad
                  FROM (VALUES ('DISPONIBLE'), ('ALQUILADO'), ('EN_MANTENIMIENTO')) AS e(estado)
                  LEFT JOIN vehiculo v
                         ON v.estado = e.estado
                        AND (CAST(:idSede AS bigint) IS NULL OR v.sede_id_sede = CAST(:idSede AS bigint))
                 GROUP BY e.estado
                 ORDER BY e.estado
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idSede", idSede, Types.BIGINT);
        return jdbc.query(sql, params, (rs, n) -> new FlotaEstadoResponse(
                rs.getString("estado"),
                rs.getLong("cantidad")));
    }

    /**
     * Ingresos (pagos aprobados) por sede de entrega, con rango opcional sobre la fecha de
     * inicio de la reserva. Incluye las sedes sin ingresos.
     */
    public List<IngresoSedeResponse> ingresosPorSede(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT s.id_sede, s.nombre_sede,
                       COALESCE(SUM(p.monto), 0) AS ingresos,
                       COUNT(p.id_pago)          AS pagos_aprobados
                  FROM sede s
                  LEFT JOIN reserva r
                         ON r.sede_entrega = s.id_sede
                        AND (CAST(:desde AS date) IS NULL OR r.fecha_inicio >= CAST(:desde AS date))
                        AND (CAST(:hasta AS date) IS NULL OR r.fecha_inicio <= CAST(:hasta AS date))
                  LEFT JOIN pago p
                         ON p.reserva_id_reserva = r.id_reserva
                        AND p.estado_pago = 'APROBADO'
                 GROUP BY s.id_sede, s.nombre_sede
                 ORDER BY ingresos DESC, s.nombre_sede
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("desde", desde, Types.DATE)
                .addValue("hasta", hasta, Types.DATE);
        return jdbc.query(sql, params, (rs, n) -> new IngresoSedeResponse(
                rs.getLong("id_sede"),
                rs.getString("nombre_sede"),
                rs.getBigDecimal("ingresos"),
                rs.getLong("pagos_aprobados")));
    }

    /** Seguros más contratados en reservas no canceladas, desde la tabla intermedia seg_res. */
    public List<SeguroRankingResponse> segurosMasContratados(int limite) {
        String sql = """
                SELECT s.id_seguro, s.nombre_seguro,
                       COUNT(sr.reserva_id_reserva)          AS veces,
                       COALESCE(SUM(sr.costo_contratado), 0) AS valor
                  FROM seguro s
                  JOIN seg_res sr ON sr.seguro_id_seguro = s.id_seguro
                  JOIN reserva r  ON r.id_reserva = sr.reserva_id_reserva
                                 AND r.estado_reserva <> 'CANCELADA'
                 GROUP BY s.id_seguro, s.nombre_seguro
                 ORDER BY veces DESC, valor DESC, s.nombre_seguro
                 LIMIT :limite
                """;
        return jdbc.query(sql, Map.of("limite", limite), (rs, n) -> new SeguroRankingResponse(
                rs.getLong("id_seguro"),
                rs.getString("nombre_seguro"),
                rs.getLong("veces"),
                rs.getBigDecimal("valor")));
    }

    /** Empleados activos por sede y rol, usando las tablas de la especialización de EMPLEADO. */
    public List<EmpleadosSedeResponse> empleadosPorSede() {
        String sql = """
                SELECT s.id_sede, s.nombre_sede, x.rol, COUNT(*) AS cantidad
                  FROM sede s
                  JOIN empleado e ON e.sede_id_sede = s.id_sede
                  JOIN (SELECT id_empleado, CAST('ADMINISTRADOR' AS varchar) AS rol FROM administrador
                        UNION ALL
                        SELECT id_empleado, CAST('ASESOR' AS varchar) FROM asesor
                        UNION ALL
                        SELECT id_empleado, CAST('TECNICO' AS varchar) FROM tecnico) x
                    ON x.id_empleado = e.id_empleado
                 WHERE e.estado = 'ACTIVO'
                 GROUP BY s.id_sede, s.nombre_sede, x.rol
                 ORDER BY s.nombre_sede, x.rol
                """;
        return jdbc.query(sql, (rs, n) -> new EmpleadosSedeResponse(
                rs.getLong("id_sede"),
                rs.getString("nombre_sede"),
                rs.getString("rol"),
                rs.getLong("cantidad")));
    }
}