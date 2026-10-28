package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Lecturas con derived queries; escrituras con reglas de negocio delegadas a
 * funciones y procedimientos almacenados de PostgreSQL.
 * El EntityGraph carga las relaciones "a uno" en la misma consulta (evita N+1).
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo", "asesor", "sedeEntrega", "sedeRecibe"})
    Optional<Reserva> findById(Long id);


    /** ¿La reserva es de ese cliente? Para que un cliente solo opere sobre las suyas. */
    boolean existsByIdReservaAndClienteIdCliente(Long idReserva, Long idCliente);

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Page<Reserva> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Page<Reserva> findByClienteIdCliente(Long idCliente, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Page<Reserva> findByEstadoReserva(String estadoReserva, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Page<Reserva> findByClienteIdClienteAndEstadoReserva(Long idCliente, String estadoReserva, Pageable pageable);

    /**
     * Llama a la función 'fn_crear_reserva'. Crea la reserva en PENDIENTE con sus
     * seguros y devuelve el id. Los seguros viajan como literal de arreglo de
     * PostgreSQL (por ejemplo "{1,2}") o null.
     */
    @Query(value = """
            SELECT fn_crear_reserva(CAST(:idCliente AS bigint), CAST(:idVehiculo AS bigint),
                                    CAST(:idAsesor AS bigint), CAST(:sedeEntrega AS bigint),
                                    CAST(:sedeRecibe AS bigint), CAST(:fechaInicio AS date),
                                    CAST(:fechaFin AS date), CAST(:idsSeguros AS bigint[]))
            """, nativeQuery = true)
    Long crearReserva(@Param("idCliente") Long idCliente,
                      @Param("idVehiculo") Long idVehiculo,
                      @Param("idAsesor") Long idAsesor,
                      @Param("sedeEntrega") Long sedeEntrega,
                      @Param("sedeRecibe") Long sedeRecibe,
                      @Param("fechaInicio") LocalDate fechaInicio,
                      @Param("fechaFin") LocalDate fechaFin,
                      @Param("idsSeguros") String idsSeguros);

    /** Función 'fn_costo_reserva': días x (tarifa de la categoría + seguros). */
    @Query(value = """
            SELECT fn_costo_reserva(CAST(:idVehiculo AS bigint), CAST(:fechaInicio AS date),
                                    CAST(:fechaFin AS date), CAST(:idsSeguros AS bigint[]))
            """, nativeQuery = true)
    BigDecimal calcularCosto(@Param("idVehiculo") Long idVehiculo,
                             @Param("fechaInicio") LocalDate fechaInicio,
                             @Param("fechaFin") LocalDate fechaFin,
                             @Param("idsSeguros") String idsSeguros);

    /** Función 'fn_saldo_pendiente': costo de la reserva menos pagos aprobados. */
    @Query(value = "SELECT fn_saldo_pendiente(CAST(:idReserva AS bigint))", nativeQuery = true)
    BigDecimal saldoPendiente(@Param("idReserva") Long idReserva);

    /**
     * Procedimiento 'sp_registrar_pago'. Un pago aprobado confirma la reserva
     * PENDIENTE; uno rechazado la cancela.
     */
    @Procedure(procedureName = "sp_registrar_pago")
    void registrarPago(@Param("p_id_reserva") Long idReserva,
                       @Param("p_monto") BigDecimal monto,
                       @Param("p_metodo") String metodo,
                       @Param("p_estado") String estado);

    /** CONFIRMADA -> EN_CURSO con el asesor que entrega (el trigger deja el vehículo ALQUILADO). */
    @Procedure(procedureName = "sp_iniciar_reserva")
    void iniciarReserva(@Param("p_id_reserva") Long idReserva, @Param("p_id_asesor") Long idAsesor);

    /** EN_CURSO -> FINALIZADA, solo sin saldo pendiente (el trigger libera el vehículo). */
    @Procedure(procedureName = "sp_finalizar_reserva")
    void finalizarReserva(@Param("p_id_reserva") Long idReserva);

    /** Cancela la reserva y reembolsa los pagos aprobados. */
    @Procedure(procedureName = "sp_cancelar_reserva")
    void cancelarReserva(@Param("p_id_reserva") Long idReserva);
}
