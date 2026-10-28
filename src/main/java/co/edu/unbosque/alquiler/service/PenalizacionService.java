package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.dto.PenalizacionRequest;
import co.edu.unbosque.alquiler.dto.PenalizacionResponse;
import co.edu.unbosque.alquiler.entity.Penalizacion;
import co.edu.unbosque.alquiler.entity.Reserva;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import co.edu.unbosque.alquiler.repository.InspeccionRepository;
import co.edu.unbosque.alquiler.repository.PenalizacionRepository;
import co.edu.unbosque.alquiler.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cargos por daños. Las reglas (estado de la reserva, monto, cambios de estado)
 * viven en la base de datos; aquí solo se comprueba lo que está en MongoDB.
 */
@Service
public class PenalizacionService {

    private final PenalizacionRepository penalizacionRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final InspeccionRepository inspeccionRepository;

    public PenalizacionService(PenalizacionRepository penalizacionRepository,
                               ReservaRepository reservaRepository,
                               ClienteRepository clienteRepository,
                               InspeccionRepository inspeccionRepository) {
        this.penalizacionRepository = penalizacionRepository;
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.inspeccionRepository = inspeccionRepository;
    }

    @Transactional
    public PenalizacionResponse registrar(Long idReserva, PenalizacionRequest request) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con id " + idReserva));

        String idInspeccion = null;
        if (request.idInspeccion() != null && !request.idInspeccion().isBlank()) {
            idInspeccion = validarInspeccion(request.idInspeccion(), reserva);
        }

        Long id = penalizacionRepository.registrar(
                idReserva, request.concepto().trim(), request.monto(), idInspeccion);
        return aRespuesta(penalizacionRepository.findById(id).orElseThrow());
    }

    /** Comprueba en Mongo que la inspección exista y sea del mismo vehículo de la reserva. */
    private String validarInspeccion(String idInspeccion, Reserva reserva) {
        InspeccionVehiculo inspeccion = inspeccionRepository.findById(idInspeccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inspección " + idInspeccion));
        if (!inspeccion.getIdVehiculo().equals(reserva.getVehiculo().getIdVehiculo())) {
            throw new SolicitudInvalidaException(
                    "La inspección " + idInspeccion + " no corresponde al vehículo de la reserva");
        }
        if (inspeccion.getIdReserva() != null && !inspeccion.getIdReserva().equals(reserva.getIdReserva())) {
            throw new SolicitudInvalidaException(
                    "La inspección " + idInspeccion + " pertenece a otra reserva");
        }
        return idInspeccion;
    }

    @Transactional(readOnly = true)
    public List<PenalizacionResponse> listarPorReserva(Long idReserva) {
        if (!reservaRepository.existsById(idReserva)) {
            throw new RecursoNoEncontradoException("No existe una reserva con id " + idReserva);
        }
        return penalizacionRepository.findByReservaIdReservaOrderByIdPenalizacionAsc(idReserva)
                .stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<PenalizacionResponse> listarPorCliente(Long idCliente) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new RecursoNoEncontradoException("No existe un cliente con id " + idCliente);
        }
        return penalizacionRepository.findByReservaClienteIdClienteOrderByIdPenalizacionDesc(idCliente)
                .stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public PenalizacionResponse cambiarEstado(Long idPenalizacion, String estado) {
        if (!penalizacionRepository.existsById(idPenalizacion)) {
            throw new RecursoNoEncontradoException("No existe una penalización con id " + idPenalizacion);
        }
        penalizacionRepository.cambiarEstado(idPenalizacion, estado);
        return aRespuesta(penalizacionRepository.findById(idPenalizacion).orElseThrow());
    }

    private PenalizacionResponse aRespuesta(Penalizacion p) {
        return new PenalizacionResponse(p.getIdPenalizacion(), p.getReserva().getIdReserva(),
                p.getFechaCargo(), p.getConcepto(), p.getMonto(), p.getEstadoCargo(),
                p.getIdInspeccionMongo());
    }
}