package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Sede;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
    List<Sede> findByEstado(String estado, Sort sort);
}

