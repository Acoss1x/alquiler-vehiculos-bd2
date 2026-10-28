package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.CotizacionResponse;
import co.edu.unbosque.alquiler.dto.PagoRequest;
import co.edu.unbosque.alquiler.dto.PagoResponse;
import co.edu.unbosque.alquiler.dto.ReservaRequest;
import co.edu.unbosque.alquiler.dto.ReservaResponse;
import co.edu.unbosque.alquiler.dto.ReservaResumenResponse;
import co.edu.unbosque.alquiler.dto.SeguroContratadoResponse;
import co.edu.unbosque.alquiler.entity.EstadoReserva;
import co.edu.unbosque.alquiler.entity.Pago;
import co.edu.unbosque.alquiler.entity.Reserva;
import co.edu.unbosque.alquiler.entity.SeguroReserva;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.entity.EstadoPago;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.AsesorRepository;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import co.edu.unbosque.alquiler.repository.PagoRepository;
import co.edu.unbosque.alquiler.repository.ReservaRepository;
import co.edu.unbosque.alquiler.repository.SedeRepository;
import co.edu.unbosque.alquiler.repository.SeguroReservaRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Casos de uso de reservas y pagos. Las reglas de negocio (disponibilidad,
 * solapamientos, licencia vigente, transiciones de estado, suma de pagos) viven
 * en la base de datos; aquí solo se validan existencias para devolver un 404
 * claro y se arman las respuestas.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final SeguroReservaRepository seguroReservaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final AsesorRepository asesorRepository;
    private final SedeRepository sedeRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          PagoRepository pagoRepository,
                          SeguroReservaRepository seguroReservaRepository,
                          ClienteRepository clienteRepository,
                          VehiculoRepository vehiculoRepository,
                          AsesorRepository asesorRepository,
                          SedeRepository sedeRepository) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.seguroReservaRepository = seguroReservaRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.asesorRepository = asesorRepository;
        this.sedeRepository = sedeRepository;
    }

    /**
     * Crea la reserva en estado PENDIENTE. La función de BD devuelve el id.
     * El asesor es opcional: una reserva hecha por el propio cliente nace sin asesor.
     */
    @Transactional
    public ReservaResponse crear(ReservaRequest request) {
        validarExiste(clienteRepository.existsById(request.idCliente()), "cliente", request.idCliente());
        validarExiste(vehiculoRepository.existsById(request.idVehiculo()), "vehículo", request.idVehiculo());
        if (request.idAsesor() != null) {
            validarExiste(asesorRepository.existsById(request.idAsesor()), "asesor", request.idAsesor());
        }
        validarExiste(sedeRepository.existsById(request.idSedeEntrega()), "sede", request.idSedeEntrega());
        validarExiste(sedeRepository.existsById(request.idSedeRecibe()), "sede", request.idSedeRecibe());

        Long idReserva = reservaRepository.crearReserva(
                request.idCliente(),
                request.idVehiculo(),
                request.idAsesor(),
                request.idSedeEntrega(),
                request.idSedeRecibe(),
                request.fechaInicio(),
                request.fechaFin(),
                aArregloPostgres(request.idsSeguros()));

        return obtenerPorId(idReserva);
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long id) {
        Reserva r = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con id " + id));

        List<SeguroContratadoResponse> seguros = seguroReservaRepository.findByReservaIdReserva(id).stream()
                .map(this::aSeguroContratado)
                .toList();
        List<PagoResponse> pagos = listarPagos(id);
        BigDecimal saldo = reservaRepository.saldoPendiente(id);

        return new ReservaResponse(
                r.getIdReserva(),
                r.getEstadoReserva(),
                r.getFechaCreacion(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getCostoTotal(),
                saldo,
                r.getCliente().getIdCliente(),
                nombreCompleto(r.getCliente().getPrimerNombre(), r.getCliente().getPrimerApellido()),
                r.getVehiculo().getIdVehiculo(),
                r.getVehiculo().getPlaca(),
                r.getAsesor() == null ? null : r.getAsesor().getIdEmpleado(),
                r.getAsesor() == null ? null
                        : nombreCompleto(r.getAsesor().getPrimerNombre(), r.getAsesor().getPrimerApellido()),
                r.getSedeEntrega().getIdSede(),
                r.getSedeEntrega().getNombreSede(),
                r.getSedeRecibe().getIdSede(),
                r.getSedeRecibe().getNombreSede(),
                seguros,
                pagos);
    }

    /** Listado paginado con filtros opcionales por cliente y estado. */
    @Transactional(readOnly = true)
    public Page<ReservaResumenResponse> listar(Long idCliente, EstadoReserva estado, Pageable pageable) {
        String nombreEstado = estado == null ? null : estado.name();

        Page<Reserva> pagina;
        if (idCliente != null && nombreEstado != null) {
            pagina = reservaRepository.findByClienteIdClienteAndEstadoReserva(idCliente, nombreEstado, pageable);
        } else if (idCliente != null) {
            pagina = reservaRepository.findByClienteIdCliente(idCliente, pageable);
        } else if (nombreEstado != null) {
            pagina = reservaRepository.findByEstadoReserva(nombreEstado, pageable);
        } else {
            pagina = reservaRepository.findAll(pageable);
        }
        return pagina.map(this::aResumen);
    }

    /** RF-07: cotización previa a la reserva, calculada por la función de BD. */
    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(Long idVehiculo, LocalDate fechaInicio, LocalDate fechaFin,
                                      List<Long> idsSeguros) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new SolicitudInvalidaException("La fecha de fin no puede ser anterior a la de inicio");
        }
        validarExiste(vehiculoRepository.existsById(idVehiculo), "vehículo", idVehiculo);

        BigDecimal costo = reservaRepository.calcularCosto(
                idVehiculo, fechaInicio, fechaFin, aArregloPostgres(idsSeguros));
        int dias = (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        return new CotizacionResponse(idVehiculo, dias, costo);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos(Long idReserva) {
        validarExiste(reservaRepository.existsById(idReserva), "reserva", idReserva);
        return pagoRepository.findByReservaIdReservaOrderByIdPagoAsc(idReserva).stream()
                .map(this::aPago)
                .toList();
    }

    /**
     * Registra un pago. Si se aprueba y la reserva está PENDIENTE, queda CONFIRMADA;
     * si se rechaza, la reserva se cancela para liberar el vehículo.
     */
    @Transactional
    public ReservaResponse registrarPago(Long idReserva, PagoRequest request) {
        validarExiste(reservaRepository.existsById(idReserva), "reserva", idReserva);
        if (request.estado() == EstadoPago.REEMBOLSADO) {
            throw new SolicitudInvalidaException(
                    "Solo se pueden registrar pagos APROBADO o RECHAZADO; los reembolsos los genera la cancelación de la reserva");
        }
        reservaRepository.registrarPago(idReserva, request.monto(), request.metodoPago(), request.estado().name());
        return obtenerPorId(idReserva);
    }

    /** CONFIRMADA -> EN_CURSO: entrega del vehículo al cliente por un asesor activo. */
    @Transactional
    public ReservaResponse iniciar(Long idReserva, Long idAsesor) {
        validarExiste(reservaRepository.existsById(idReserva), "reserva", idReserva);
        validarExiste(asesorRepository.existsById(idAsesor), "asesor", idAsesor);
        reservaRepository.iniciarReserva(idReserva, idAsesor);
        return obtenerPorId(idReserva);
    }

    /** EN_CURSO -> FINALIZADA: devolución del vehículo; exige saldo en cero. */
    @Transactional
    public ReservaResponse finalizar(Long idReserva) {
        validarExiste(reservaRepository.existsById(idReserva), "reserva", idReserva);
        reservaRepository.finalizarReserva(idReserva);
        return obtenerPorId(idReserva);
    }

    /** Cancela una reserva PENDIENTE o CONFIRMADA y reembolsa los pagos aprobados. */
    @Transactional
    public ReservaResponse cancelar(Long idReserva) {
        validarExiste(reservaRepository.existsById(idReserva), "reserva", idReserva);
        reservaRepository.cancelarReserva(idReserva);
        return obtenerPorId(idReserva);
    }
    /** ¿La reserva pertenece a ese cliente? Se usa para que un cliente solo opere sobre las suyas. */
    @Transactional(readOnly = true)
    public boolean perteneceACliente(Long idReserva, Long idCliente) {
        return reservaRepository.existsByIdReservaAndClienteIdCliente(idReserva, idCliente);
    }

    private void validarExiste(boolean existe, String recurso, Long id) {
        if (!existe) {
            throw new RecursoNoEncontradoException("No existe un " + recurso + " con id " + id);
        }
    }

    /** Convierte [1, 2] en el literal de arreglo de PostgreSQL "{1,2}"; vacío o null -> null. */
    private String aArregloPostgres(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}"));
    }

    private String nombreCompleto(String nombre, String apellido) {
        return nombre + " " + apellido;
    }

    private PagoResponse aPago(Pago p) {
        return new PagoResponse(p.getIdPago(), p.getFechaPago(), p.getMonto(),
                p.getMetodoPago(), p.getEstadoPago());
    }

    private SeguroContratadoResponse aSeguroContratado(SeguroReserva sr) {
        return new SeguroContratadoResponse(sr.getSeguro().getIdSeguro(),
                sr.getSeguro().getNombreSeguro(), sr.getCostoContratado());
    }

    private ReservaResumenResponse aResumen(Reserva r) {
        return new ReservaResumenResponse(
                r.getIdReserva(),
                r.getEstadoReserva(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getCostoTotal(),
                r.getCliente().getIdCliente(),
                nombreCompleto(r.getCliente().getPrimerNombre(), r.getCliente().getPrimerApellido()),
                r.getVehiculo().getIdVehiculo(),
                r.getVehiculo().getPlaca());
    }
}