package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Penalizacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PenalizacionRepository extends JpaRepository<Penalizacion, Long> {

    boolean existsByIdInspeccionMongo(String idInspeccionMongo);

    @EntityGraph(attributePaths = {"reserva"})
    List<Penalizacion> findByReservaIdReservaOrderByIdPenalizacionAsc(Long idReserva);

    /** Todas las penalizaciones de un cliente (a través de sus reservas), las más recientes primero. */
    @EntityGraph(attributePaths = {"reserva"})
    List<Penalizacion> findByReservaClienteIdClienteOrderByIdPenalizacionDesc(Long idCliente);

    /** Función 'fn_registrar_penalizacion': valida y devuelve el id del cargo creado. */
    @Query(value = """
            SELECT fn_registrar_penalizacion(CAST(:idReserva AS bigint), CAST(:concepto AS varchar),
                                             CAST(:monto AS numeric), CAST(:idInspeccion AS varchar))
            """, nativeQuery = true)
    Long registrar(@Param("idReserva") Long idReserva,
                   @Param("concepto") String concepto,
                   @Param("monto") BigDecimal monto,
                   @Param("idInspeccion") String idInspeccion);

    /** Procedimiento 'sp_cambiar_estado_penalizacion': PENDIENTE -> PAGADA o ANULADA. */
    @Procedure(procedureName = "sp_cambiar_estado_penalizacion")
    void cambiarEstado(@Param("p_id_penalizacion") Long idPenalizacion,
                       @Param("p_estado") String estado);


}
