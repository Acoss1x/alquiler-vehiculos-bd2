package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Llama al procedimiento almacenado 'sp_registrar_cliente' en PostgreSQL.
     * Esto delega la lógica transaccional y las validaciones al motor relacional.
     */
    @Procedure(procedureName = "sp_registrar_cliente")
    void registrarClienteTransaccional(
            @Param("p_documento") String documento,
            @Param("p_nombre") String nombre,
            @Param("p_apellido") String apellido,
            @Param("p_correo") String correo,
            @Param("p_telefono") String telefono
    );
}
