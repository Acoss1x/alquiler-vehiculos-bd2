package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeguroRepository extends JpaRepository<Seguro, Long> {
}
