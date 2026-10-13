package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.document.PiezaCambiada;
import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
<<<<<<< HEAD
import co.edu.unbosque.alquiler.dto.PiezaDTO;
=======
import co.edu.unbosque.alquiler.dto.PiezaDto;
>>>>>>> ab8d144af1469c2acbe5d1a01122134522070012
import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.repository.InspeccionConsultasRepository;
import co.edu.unbosque.alquiler.repository.InspeccionRepository;
import co.edu.unbosque.alquiler.repository.TecnicoRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquesta la lógica híbrida: la inspección vive en MongoDB y el bloqueo del
 * vehículo en PostgreSQL. No existe una transacción distribuida entre ambos
 * motores, por eso se aplica una compensación manual.
 */
@Service
public class InspeccionService {

    private static final Logger log = LoggerFactory.getLogger(InspeccionService.class);

    private final InspeccionRepository inspeccionRepository;
    private final InspeccionConsultasRepository consultasRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TecnicoRepository tecnicoRepository;

    public InspeccionService(InspeccionRepository inspeccionRepository,
                             InspeccionConsultasRepository consultasRepository,
                             VehiculoRepository vehiculoRepository,
                             TecnicoRepository tecnicoRepository) {
        this.inspeccionRepository = inspeccionRepository;
        this.consultasRepository = consultasRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    /**
     * Registra la inspección en MongoDB y deja el vehículo EN_MANTENIMIENTO en PostgreSQL.
     * <p>
     * Orden elegido: primero Mongo, luego el SP. Si Mongo falla, Postgres no se tocó.
     * Si el SP falla (por ejemplo, el vehículo está alquilado), se borra el documento
     * recién guardado.
     * </p>
     */
    @Transactional
    public InspeccionResponse registrar(Long idVehiculo, InspeccionRequest request) {
        validarVehiculo(idVehiculo);
        if (!tecnicoRepository.existsById(request.idTecnico())) {
            throw new RecursoNoEncontradoException("No existe un técnico con id " + request.idTecnico());
        }

        InspeccionVehiculo guardada = inspeccionRepository.save(aDocumento(idVehiculo, request));

        try {
            vehiculoRepository.actualizarEstadoVehiculo(idVehiculo, EstadoVehiculo.EN_MANTENIMIENTO.name());
        } catch (RuntimeException ex) {
            compensar(guardada);
            throw ex;
        }
        return aRespuesta(guardada);
    }

    public List<InspeccionResponse> listarPorVehiculo(Long idVehiculo) {
        validarVehiculo(idVehiculo);
        return inspeccionRepository.findByIdVehiculoOrderByFechaInspeccionDesc(idVehiculo)
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    public CostoMantenimientoResponse costoMantenimiento(Long idVehiculo) {
        validarVehiculo(idVehiculo);
        return new CostoMantenimientoResponse(idVehiculo, consultasRepository.costoTotalMantenimiento(idVehiculo));
    }

    private void validarVehiculo(Long idVehiculo) {
        if (!vehiculoRepository.existsById(idVehiculo)) {
            throw new RecursoNoEncontradoException("No existe un vehículo con id " + idVehiculo);
        }
    }

    private void compensar(InspeccionVehiculo guardada) {
        try {
            inspeccionRepository.deleteById(guardada.getIdInspeccion());
        } catch (RuntimeException e) {
            log.error("No se pudo revertir la inspección {} en MongoDB; requiere limpieza manual",
                    guardada.getIdInspeccion(), e);
        }
    }

    private InspeccionVehiculo aDocumento(Long idVehiculo, InspeccionRequest request) {
        InspeccionVehiculo inspeccion = new InspeccionVehiculo();
        inspeccion.setIdVehiculo(idVehiculo);
        inspeccion.setIdTecnico(request.idTecnico());
        inspeccion.setObservacionesGenerales(request.observacionesGenerales());
        if (request.piezasCambiadas() != null) {
            inspeccion.setPiezasCambiadas(request.piezasCambiadas().stream()
                    .map(p -> new PiezaCambiada(p.nombre(), p.cantidad(), p.costo()))
                    .toList());
        }
        return inspeccion;
    }

    private InspeccionResponse aRespuesta(InspeccionVehiculo i) {
<<<<<<< HEAD
        List<PiezaDTO> piezas = i.getPiezasCambiadas().stream()
                .map(p -> new PiezaDTO(p.getNombre(), p.getCantidad(), p.getCosto()))
=======
        List<PiezaDto> piezas = i.getPiezasCambiadas().stream()
                .map(p -> new PiezaDto(p.getNombre(), p.getCantidad(), p.getCosto()))
>>>>>>> ab8d144af1469c2acbe5d1a01122134522070012
                .toList();
        return new InspeccionResponse(
                i.getIdInspeccion(),
                i.getIdVehiculo(),
                i.getIdTecnico(),
                i.getObservacionesGenerales(),
                piezas,
                i.getFechaInspeccion());
    }
}
