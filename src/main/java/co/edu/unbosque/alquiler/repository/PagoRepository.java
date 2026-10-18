package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByReservaIdReservaOrderByIdPagoAsc(Long idReserva);
}
