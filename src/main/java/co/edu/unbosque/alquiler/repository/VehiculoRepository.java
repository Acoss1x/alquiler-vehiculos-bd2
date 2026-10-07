package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    /**
     * Llama al procedimiento almacenado 'sp_actualizar_estado_vehiculo' en PostgreSQL.
     * Delega la lógica de validación (ej. evitar alquilar un carro en mantenimiento)
     * directamente al motor relacional.
     */
    @Procedure(procedureName = "sp_actualizar_estado_vehiculo")
    void actualizarEstadoVehiculo(
            @Param("p_id_vehiculo") Long idVehiculo,
            @Param("p_nuevo_estado") String nuevoEstado
    );
}