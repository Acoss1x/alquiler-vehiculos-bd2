package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
}

