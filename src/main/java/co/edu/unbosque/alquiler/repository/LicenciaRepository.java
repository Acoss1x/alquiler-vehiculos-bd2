package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Licencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    Optional<Licencia> findByClienteIdCliente(Long idCliente);

    /**
     * Llama al procedimiento almacenado 'sp_registrar_licencia' en PostgreSQL.
     * El motor valida que el cliente exista, que no tenga otra licencia
     * y que las fechas sean coherentes y la licencia esté vigente.
     */
    @Procedure(procedureName = "sp_registrar_licencia")
    void registrarLicencia(
            @Param("p_id_cliente") Long idCliente,
            @Param("p_numero_licencia") String numeroLicencia,
            @Param("p_categoria") String categoria,
            @Param("p_fecha_expedicion") LocalDate fechaExpedicion,
            @Param("p_fecha_vencimiento") LocalDate fechaVencimiento
    );
}
