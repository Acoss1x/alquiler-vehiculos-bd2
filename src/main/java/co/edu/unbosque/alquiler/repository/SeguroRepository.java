package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Seguro;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeguroRepository extends JpaRepository<Seguro, Long> {
    List<Seguro> findByEstado(String estado, Sort sort);
}
