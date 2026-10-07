package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.InspeccionVehiculo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspeccionRepository extends MongoRepository<InspeccionVehiculo, String> {
    List<InspeccionVehiculo> findByIdVehiculo(Long idVehiculo);
}