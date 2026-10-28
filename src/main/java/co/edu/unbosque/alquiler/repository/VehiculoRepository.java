package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Los EntityGraph cargan categoría y sede en la misma consulta, para evitar el
 * problema N+1 al armar las respuestas.
 */
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    long countBySedeIdSede(Long idSede);
    
    @Override
    @EntityGraph(attributePaths = {"categoria", "sede"})
    Optional<Vehiculo> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"categoria", "sede"})
    Page<Vehiculo> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "sede"})
    Optional<Vehiculo> findByPlaca(String placa);

    @EntityGraph(attributePaths = {"categoria", "sede"})
    Page<Vehiculo> findByEstado(String estado, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "sede"})
    Page<Vehiculo> findBySedeIdSede(Long idSede, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "sede"})
    Page<Vehiculo> findBySedeIdSedeAndEstado(Long idSede, String estado, Pageable pageable);

    /**
     * Llama a la función 'fn_vehiculos_disponibles' (RF-03): vehículos que no están
     * en mantenimiento y no tienen otra reserva activa que se cruce con las fechas.
     * Los filtros de categoría y sede son opcionales (null = sin filtro).
     */
    @Query(value = """
            SELECT * FROM fn_vehiculos_disponibles(CAST(:fechaInicio AS date), CAST(:fechaFin AS date),
                                                   CAST(:idCategoria AS bigint), CAST(:idSede AS bigint))
            """, nativeQuery = true)
    List<Vehiculo> buscarDisponibles(@Param("fechaInicio") LocalDate fechaInicio,
                                     @Param("fechaFin") LocalDate fechaFin,
                                     @Param("idCategoria") Long idCategoria,
                                     @Param("idSede") Long idSede);

    /**
     * Llama al procedimiento 'sp_registrar_vehiculo' en PostgreSQL.
     * El motor valida la placa única y la capacidad de estacionamiento de la sede,
     * y asigna el estado inicial DISPONIBLE.
     */
    @Procedure(procedureName = "sp_registrar_vehiculo")
    void registrarVehiculo(
            @Param("p_placa") String placa,
            @Param("p_marca") String marca,
            @Param("p_modelo_anio") Integer modeloAnio,
            @Param("p_kilometraje") Integer kilometraje,
            @Param("p_color") String color,
            @Param("p_id_categoria") Long idCategoria,
            @Param("p_id_sede") Long idSede
    );

    /**
     * Llama al procedimiento 'sp_actualizar_estado_vehiculo' en PostgreSQL.
     * Delega la validación de transiciones (ej. no alquilar un carro en
     * mantenimiento) al motor relacional.
     */
    @Procedure(procedureName = "sp_actualizar_estado_vehiculo")
    void actualizarEstadoVehiculo(
            @Param("p_id_vehiculo") Long idVehiculo,
            @Param("p_nuevo_estado") String nuevoEstado
    );

    /** Procedimiento 'sp_actualizar_kilometraje_vehiculo': el kilometraje nunca baja. */
    @Procedure(procedureName = "sp_actualizar_kilometraje_vehiculo")
    void actualizarKilometrajeVehiculo(
            @Param("p_id_vehiculo") Long idVehiculo,
            @Param("p_kilometraje") Integer kilometraje
    );
}
