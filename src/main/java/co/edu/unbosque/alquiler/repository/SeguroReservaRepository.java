package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.SeguroReserva;
import co.edu.unbosque.alquiler.entity.SeguroReservaId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeguroReservaRepository extends JpaRepository<SeguroReserva, SeguroReservaId> {

    @EntityGraph(attributePaths = {"seguro"})
    List<SeguroReserva> findByReservaIdReserva(Long idReserva);
}
