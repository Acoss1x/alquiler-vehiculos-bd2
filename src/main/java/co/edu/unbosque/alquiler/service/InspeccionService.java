package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.document.DanoVehiculo;
import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.document.PiezaCambiada;
import co.edu.unbosque.alquiler.document.TipoInspeccion;
import co.edu.unbosque.alquiler.dto.CostoMantenimientoResponse;
import co.edu.unbosque.alquiler.dto.DanoDTO;
import co.edu.unbosque.alquiler.dto.GastoTecnicoResponse;
import co.edu.unbosque.alquiler.dto.InspeccionRequest;
import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.dto.InspeccionUpdateRequest;
import co.edu.unbosque.alquiler.dto.PiezaDTO;
import co.edu.unbosque.alquiler.dto.PiezaFrecuenciaResponse;
import co.edu.unbosque.alquiler.entity.Empleado;
import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import co.edu.unbosque.alquiler.entity.Reserva;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.EmpleadoRepository;
import co.edu.unbosque.alquiler.repository.InspeccionConsultasRepository;
import co.edu.unbosque.alquiler.repository.InspeccionRepository;
import co.edu.unbosque.alquiler.repository.PenalizacionRepository;
import co.edu.unbosque.alquiler.repository.ReservaRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la lógica híbrida: la inspección vive en MongoDB (las fotos, en GridFS) y
 * el kilometraje y el estado del vehículo, en PostgreSQL. No hay transacción distribuida
 * entre ambos motores, por eso se aplica una compensación manual al registrar.
 */
@Service
public class InspeccionService {

    private static final Logger log = LoggerFactory.getLogger(InspeccionService.class);

    private final InspeccionRepository inspeccionRepository;
    private final InspeccionConsultasRepository consultasRepository;
    private final VehiculoRepository vehiculoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ReservaRepository reservaRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final GridFsTemplate gridFsTemplate;

    public InspeccionService(InspeccionRepository inspeccionRepository,
                             InspeccionConsultasRepository consultasRepository,
                             VehiculoRepository vehiculoRepository,
                             EmpleadoRepository empleadoRepository,
                             ReservaRepository reservaRepository,
                             PenalizacionRepository penalizacionRepository,
                             GridFsTemplate gridFsTemplate) {
        this.inspeccionRepository = inspeccionRepository;
        this.consultasRepository = consultasRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.empleadoRepository = empleadoRepository;
        this.reservaRepository = reservaRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    /**
     * Primero se validan las reglas, luego Mongo y al final los procedimientos de PostgreSQL.
     * Si un procedimiento falla (p. ej. kilometraje menor, o vehículo alquilado al mandarlo a
     * mantenimiento), se borra el documento guardado y PostgreSQL revierte lo suyo.
     */
    @Transactional
    public InspeccionResponse registrar(Long idVehiculo, Long idEmpleado, InspeccionRequest request) {
        validarVehiculo(idVehiculo);
        validarEmpleadoActivo(idEmpleado);
        validarSegunTipo(idVehiculo, request);

        InspeccionVehiculo guardada = inspeccionRepository.save(aDocumento(idVehiculo, idEmpleado, request));

        try {
            vehiculoRepository.actualizarKilometrajeVehiculo(idVehiculo, request.kilometraje());
            if (request.tipo() == TipoInspeccion.MANTENIMIENTO) {
                vehiculoRepository.actualizarEstadoVehiculo(idVehiculo, EstadoVehiculo.EN_MANTENIMIENTO.name());
            }
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

    public List<InspeccionResponse> listarPorReserva(Long idReserva) {
        if (!reservaRepository.existsById(idReserva)) {
            throw new RecursoNoEncontradoException("No existe una reserva con id " + idReserva);
        }
        return inspeccionRepository.findByIdReservaOrderByFechaInspeccionAsc(idReserva)
                .stream().map(this::aRespuesta).toList();
    }

    public InspeccionResponse obtener(Long idVehiculo, String idInspeccion) {
        return aRespuesta(buscarDe(idVehiculo, idInspeccion));
    }

    public InspeccionResponse obtenerPorFoto(String idFoto) {
        return inspeccionRepository.buscarPorFoto(idFoto).map(this::aRespuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la foto " + idFoto));
    }

    public List<InspeccionResponse> buscar(String pieza, Long idEmpleado, TipoInspeccion tipo,
                                           LocalDate desde, LocalDate hasta) {
        return consultasRepository.buscar(pieza, idEmpleado, tipo, desde, hasta)
                .stream().map(this::aRespuesta).toList();
    }

    public InspeccionResponse actualizar(Long idVehiculo, String idInspeccion, InspeccionUpdateRequest request) {
        InspeccionVehiculo inspeccion = buscarDe(idVehiculo, idInspeccion);
        List<PiezaCambiada> piezas = aPiezas(request.piezasCambiadas());
        exigir(piezas.isEmpty() || inspeccion.getTipo() == TipoInspeccion.MANTENIMIENTO,
                "Las piezas cambiadas solo se registran en una inspección de mantenimiento");
        consultasRepository.actualizarContenido(idInspeccion, request.observacionesGenerales(),
                request.combustible(), aDanos(request.danos()), piezas);
        return obtener(idVehiculo, idInspeccion);
    }

    public InspeccionResponse agregarPieza(Long idVehiculo, String idInspeccion, PiezaDTO pieza) {
        InspeccionVehiculo inspeccion = buscarDe(idVehiculo, idInspeccion);
        exigir(inspeccion.getTipo() == TipoInspeccion.MANTENIMIENTO,
                "Las piezas cambiadas solo se registran en una inspección de mantenimiento");
        consultasRepository.agregarPieza(idInspeccion,
                new PiezaCambiada(pieza.nombre(), pieza.cantidad(), pieza.costo()));
        return obtener(idVehiculo, idInspeccion);
    }

    /** No se elimina una inspección citada por una penalización; sus fotos se borran con ella. */
    public void eliminar(Long idVehiculo, String idInspeccion) {
        InspeccionVehiculo inspeccion = buscarDe(idVehiculo, idInspeccion);
        exigir(!penalizacionRepository.existsByIdInspeccionMongo(idInspeccion),
                "No se puede eliminar la inspección: una penalización la cita como evidencia");
        inspeccionRepository.delete(inspeccion);
        List<ObjectId> archivos = inspeccion.getFotos().stream()
                .filter(ObjectId::isValid).map(ObjectId::new).toList();
        if (!archivos.isEmpty()) {
            gridFsTemplate.delete(Query.query(Criteria.where("_id").in(archivos)));
        }
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

    // ---------- Validaciones ----------

    private void validarSegunTipo(Long idVehiculo, InspeccionRequest request) {
        if (request.tipo() == TipoInspeccion.MANTENIMIENTO) {
            exigir(request.idReserva() == null, "Una inspección de mantenimiento no se asocia a una reserva");
            return;
        }

        exigir(request.piezasCambiadas() == null || request.piezasCambiadas().isEmpty(),
                "Las piezas cambiadas solo se registran en una inspección de mantenimiento");
        exigir(request.idReserva() != null, "La reserva es obligatoria en una entrega o una devolución");

        Reserva reserva = reservaRepository.findById(request.idReserva())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una reserva con id " + request.idReserva()));
        exigir(reserva.getVehiculo().getIdVehiculo().equals(idVehiculo),
                "La reserva " + request.idReserva() + " no corresponde a este vehículo");

        String estado = reserva.getEstadoReserva();
        if (request.tipo() == TipoInspeccion.ENTREGA) {
            exigir("CONFIRMADA".equals(estado) || "EN_CURSO".equals(estado),
                    "La entrega se registra con la reserva CONFIRMADA o EN_CURSO (estado actual: " + estado + ")");
        } else {
            exigir("EN_CURSO".equals(estado),
                    "La devolución se registra con la reserva EN_CURSO (estado actual: " + estado + ")");
        }
        exigir(!inspeccionRepository.existsByIdReservaAndTipo(request.idReserva(), request.tipo()),
                "La reserva " + request.idReserva() + " ya tiene una inspección de " + request.tipo());
    }

    private void validarVehiculo(Long idVehiculo) {
        if (!vehiculoRepository.existsById(idVehiculo)) {
            throw new RecursoNoEncontradoException("No existe un vehículo con id " + idVehiculo);
        }
    }

    private void validarEmpleadoActivo(Long idEmpleado) {
        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un empleado con id " + idEmpleado));
        exigir("ACTIVO".equals(empleado.getEstado()), "El empleado " + idEmpleado + " no está activo");
    }

    private void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new SolicitudInvalidaException(mensaje);
        }
    }

    // ---------- Apoyo ----------

    private InspeccionVehiculo buscarDe(Long idVehiculo, String idInspeccion) {
        return inspeccionRepository.findById(idInspeccion)
                .filter(i -> idVehiculo.equals(i.getIdVehiculo()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la inspección " + idInspeccion + " para el vehículo " + idVehiculo));
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

    private List<DanoVehiculo> aDanos(List<DanoDTO> danos) {
        if (danos == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(danos.stream()
                .map(d -> new DanoVehiculo(d.zona(), d.descripcion(), d.severidad()))
                .toList());
    }

    private InspeccionVehiculo aDocumento(Long idVehiculo, Long idEmpleado, InspeccionRequest request) {
        InspeccionVehiculo inspeccion = new InspeccionVehiculo();
        inspeccion.setTipo(request.tipo());
        inspeccion.setIdVehiculo(idVehiculo);
        inspeccion.setIdReserva(request.idReserva());
        inspeccion.setIdEmpleado(idEmpleado);
        inspeccion.setCombustible(request.combustible());
        inspeccion.setKilometraje(request.kilometraje());
        inspeccion.setObservacionesGenerales(request.observacionesGenerales());
        inspeccion.setDanos(aDanos(request.danos()));
        inspeccion.setPiezasCambiadas(aPiezas(request.piezasCambiadas()));
        return inspeccion;
    }

    private InspeccionResponse aRespuesta(InspeccionVehiculo i) {
        List<PiezaDTO> piezas = i.getPiezasCambiadas().stream()
                .map(p -> new PiezaDTO(p.getNombre(), p.getCantidad(), p.getCosto()))
                .toList();
        List<DanoDTO> danos = i.getDanos().stream()
                .map(d -> new DanoDTO(d.getZona(), d.getDescripcion(), d.getSeveridad()))
                .toList();
        return new InspeccionResponse(i.getIdInspeccion(), i.getTipo(), i.getIdVehiculo(), i.getIdReserva(),
                i.getIdEmpleado(), i.getCombustible(), i.getKilometraje(), i.getObservacionesGenerales(),
                danos, piezas, List.copyOf(i.getFotos()), i.getFechaInspeccion());
    }
}