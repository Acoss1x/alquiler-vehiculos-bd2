package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByCorreoIgnoreCase(String correo);
}