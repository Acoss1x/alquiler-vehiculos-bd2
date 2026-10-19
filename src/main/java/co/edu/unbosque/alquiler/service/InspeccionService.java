package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.document.PiezaCambiada;
import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.GastoTecnicoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.PiezaDTO;
import co.edu.unbosque.alquiler.dto.PiezaFrecuenciaResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la lógica híbrida: la inspección vive en MongoDB y el bloqueo del
 * vehículo en PostgreSQL. No hay transacción distribuida entre ambos motores,
 * por eso se aplica una compensación manual al registrar.
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
     * Primero Mongo, luego el SP. Si Mongo falla, Postgres no se tocó. Si el SP
     * falla (p. ej. el vehículo está alquilado), se borra el documento guardado.
     */
    @Transactional
    public InspeccionResponse registrar(Long idVehiculo, InspeccionRequest request) {
        validarVehiculo(idVehiculo);
        validarTecnico(request.idTecnico());

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
                .stream().map(this::aRespuesta).toList();
    }

    public InspeccionResponse obtener(Long idVehiculo, String idInspeccion) {
        return aRespuesta(buscarDe(idVehiculo, idInspeccion));
    }

    public List<InspeccionResponse> buscar(String pieza, Long idTecnico, LocalDate desde, LocalDate hasta) {
        return consultasRepository.buscar(pieza, idTecnico, desde, hasta)
                .stream().map(this::aRespuesta).toList();
    }



    public InspeccionResponse actualizar(Long idVehiculo, String idInspeccion, InspeccionRequest request) {
        validarTecnico(request.idTecnico());
        InspeccionVehiculo inspeccion = buscarDe(idVehiculo, idInspeccion);
        inspeccion.setIdTecnico(request.idTecnico());
        inspeccion.setObservacionesGenerales(request.observacionesGenerales());
        inspeccion.setPiezasCambiadas(aPiezas(request.piezasCambiadas()));
        return aRespuesta(inspeccionRepository.save(inspeccion));
    }

    public InspeccionResponse agregarPieza(Long idVehiculo, String idInspeccion, PiezaDTO pieza) {
        buscarDe(idVehiculo, idInspeccion);
        consultasRepository.agregarPieza(idInspeccion,
                new PiezaCambiada(pieza.nombre(), pieza.cantidad(), pieza.costo()));
        return obtener(idVehiculo, idInspeccion);
    }



    public void eliminar(Long idVehiculo, String idInspeccion) {
        inspeccionRepository.delete(buscarDe(idVehiculo, idInspeccion));
    }



    public CostoMantenimientoResponse costoMantenimiento(Long idVehiculo) {
        validarVehiculo(idVehiculo);
        return new CostoMantenimientoResponse(idVehiculo, consultasRepository.costoTotalMantenimiento(idVehiculo));
    }

    public List<PiezaFrecuenciaResponse> piezasMasCambiadas(int limite) {
        return consultasRepository.piezasMasCambiadas(Math.max(1, Math.min(limite, 50)));
    }

    public List<GastoTecnicoResponse> gastoPorTecnico() {
        return consultasRepository.gastoPorTecnico();
    }



    private InspeccionVehiculo buscarDe(Long idVehiculo, String idInspeccion) {
        return inspeccionRepository.findById(idInspeccion)
                .filter(i -> idVehiculo.equals(i.getIdVehiculo()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la inspección " + idInspeccion + " para el vehículo " + idVehiculo));
    }

    private void validarVehiculo(Long idVehiculo) {
        if (!vehiculoRepository.existsById(idVehiculo)) {
            throw new RecursoNoEncontradoException("No existe un vehículo con id " + idVehiculo);
        }
    }

    private void validarTecnico(Long idTecnico) {
        if (!tecnicoRepository.existsById(idTecnico)) {
            throw new RecursoNoEncontradoException("No existe un técnico con id " + idTecnico);
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

    private List<PiezaCambiada> aPiezas(List<PiezaDTO> piezas) {
        if (piezas == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(piezas.stream()
                .map(p -> new PiezaCambiada(p.nombre(), p.cantidad(), p.costo()))
                .toList());
    }

    private InspeccionVehiculo aDocumento(Long idVehiculo, InspeccionRequest request) {
        InspeccionVehiculo inspeccion = new InspeccionVehiculo();
        inspeccion.setIdVehiculo(idVehiculo);
        inspeccion.setIdTecnico(request.idTecnico());
        inspeccion.setObservacionesGenerales(request.observacionesGenerales());
        inspeccion.setPiezasCambiadas(aPiezas(request.piezasCambiadas()));
        return inspeccion;
    }

    private InspeccionResponse aRespuesta(InspeccionVehiculo i) {
        List<PiezaDTO> piezas = i.getPiezasCambiadas().stream()
                .map(p -> new PiezaDTO(p.getNombre(), p.getCantidad(), p.getCosto()))
                .toList();
        return new InspeccionResponse(i.getIdInspeccion(), i.getIdVehiculo(), i.getIdTecnico(),
                i.getObservacionesGenerales(), piezas, i.getFechaInspeccion());
    }
}