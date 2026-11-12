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

    @Query(value = """
            SELECT * FROM fn_vehiculos_disponibles(CAST(:fechaInicio AS date), CAST(:fechaFin AS date),
                                                   CAST(:idCategoria AS bigint), CAST(:idSede AS bigint))
            """, nativeQuery = true)
    List<Vehiculo> buscarDisponibles(@Param("fechaInicio") LocalDate fechaInicio,
                                     @Param("fechaFin") LocalDate fechaFin,
                                     @Param("idCategoria") Long idCategoria,
                                     @Param("idSede") Long idSede);

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

    @Procedure(procedureName = "sp_actualizar_estado_vehiculo")
    void actualizarEstadoVehiculo(
            @Param("p_id_vehiculo") Long idVehiculo,
            @Param("p_nuevo_estado") String nuevoEstado
    );

    @Procedure(procedureName = "sp_actualizar_kilometraje_vehiculo")
    void actualizarKilometrajeVehiculo(
            @Param("p_id_vehiculo") Long idVehiculo,
            @Param("p_kilometraje") Integer kilometraje
    );
}
