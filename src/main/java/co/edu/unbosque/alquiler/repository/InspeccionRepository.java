package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.document.TipoInspeccion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspeccionRepository extends MongoRepository<InspeccionVehiculo, String> {

    List<InspeccionVehiculo> findByIdVehiculoOrderByFechaInspeccionDesc(Long idVehiculo);

    List<InspeccionVehiculo> findByIdReservaOrderByFechaInspeccionAsc(Long idReserva);

    boolean existsByIdReservaAndTipo(Long idReserva, TipoInspeccion tipo);

    /** Inspección que contiene esa foto (para comprobar permisos antes de descargarla). */
    @Query("{ 'fotos': ?0 }")
    Optional<InspeccionVehiculo> buscarPorFoto(String idFoto);
}