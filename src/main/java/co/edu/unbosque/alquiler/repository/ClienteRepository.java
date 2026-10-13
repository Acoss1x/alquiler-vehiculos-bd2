package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    /**
     * Llama al procedimiento almacenado 'sp_registrar_cliente' en PostgreSQL.
     * Delega las validaciones y la transacción al motor relacional
     * (el estado inicial lo asigna el procedimiento).
     */
    @Procedure(procedureName = "sp_registrar_cliente")
    void registrarClienteTransaccional(
            @Param("p_documento") String documento,
            @Param("p_primer_nombre") String primerNombre,
            @Param("p_segundo_nombre") String segundoNombre,
            @Param("p_primer_apellido") String primerApellido,
            @Param("p_segundo_apellido") String segundoApellido,
            @Param("p_correo") String correo,
            @Param("p_telefono") String telefono
    );
}