package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.entity.InspeccionVehiculo;
import co.edu.unbosque.alquiler.repository.InspeccionRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio que orquesta la lógica de negocio integrando PostgreSQL y MongoDB.
 */
@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final InspeccionRepository inspeccionRepository;


    public VehiculoService(VehiculoRepository vehiculoRepository, InspeccionRepository inspeccionRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.inspeccionRepository = inspeccionRepository;
    }

    /**
     * Registra una inspección mecánica en MongoDB y bloquea el vehículo en PostgreSQL.
     *
     * @param idVehiculo ID del vehículo en PostgreSQL
     * @param mecanico Nombre del mecánico
     * @param notas Observaciones generales (texto largo)
     * @param piezas Lista variable de piezas cambiadas (ideal para NoSQL)
     */
    public void registrarInspeccionYBloquearVehiculo(Long idVehiculo, String mecanico, String notas, List<String> piezas) {


        vehiculoRepository.actualizarEstadoVehiculo(idVehiculo, "EN_MANTENIMIENTO");


        InspeccionVehiculo inspeccion = new InspeccionVehiculo();
        inspeccion.setIdVehiculo(idVehiculo);
        inspeccion.setNombreMecanico(mecanico);
        inspeccion.setObservacionesGenerales(notas);
        inspeccion.setPiezasCambiadas(piezas);
        inspeccion.setFechaInspeccion(LocalDateTime.now());


        inspeccionRepository.save(inspeccion);
    }
}
