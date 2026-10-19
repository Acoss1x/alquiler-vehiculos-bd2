package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.VehiculoRequest;
import co.edu.unbosque.alquiler.dto.VehiculoResponse;
import co.edu.unbosque.alquiler.entity.EstadoVehiculo;
import co.edu.unbosque.alquiler.entity.Vehiculo;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.CategoriaRepository;
import co.edu.unbosque.alquiler.repository.SedeRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso de vehículo (PostgreSQL). Las escrituras con reglas de negocio
 * se delegan a procedimientos almacenados; las lecturas usan el repositorio.
 */
@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final CategoriaRepository categoriaRepository;
    private final SedeRepository sedeRepository;



    public VehiculoService(VehiculoRepository vehiculoRepository,
                           CategoriaRepository categoriaRepository,
                           SedeRepository sedeRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.categoriaRepository = categoriaRepository;
        this.sedeRepository = sedeRepository;
    }

    @Transactional
    public VehiculoResponse registrar(VehiculoRequest request) {

        if (!categoriaRepository.existsById(request.idCategoria())) {
            throw new RecursoNoEncontradoException("No existe una categoría con id " + request.idCategoria());
        }
        if (!sedeRepository.existsById(request.idSede())) {
            throw new RecursoNoEncontradoException("No existe una sede con id " + request.idSede());
        }

        vehiculoRepository.registrarVehiculo(
                request.placa(),
                request.marca(),
                request.modeloAnio(),
                request.kilometraje(),
                request.color(),
                request.idCategoria(),
                request.idSede());


        return aRespuesta(vehiculoRepository.findByPlaca(request.placa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el vehículo recién registrado con placa " + request.placa())));
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtenerPorId(Long id) {
        return aRespuesta(vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehículo con id " + id)));
    }

    @Transactional(readOnly = true)
    public Page<VehiculoResponse> listar(Long idSede, EstadoVehiculo estado, Pageable pageable) {
        String nombreEstado = estado == null ? null : estado.name();

        Page<Vehiculo> pagina;
        if (idSede != null && nombreEstado != null) {
            pagina = vehiculoRepository.findBySedeIdSedeAndEstado(idSede, nombreEstado, pageable);
        } else if (idSede != null) {
            pagina = vehiculoRepository.findBySedeIdSede(idSede, pageable);
        } else if (nombreEstado != null) {
            pagina = vehiculoRepository.findByEstado(nombreEstado, pageable);
        } else {
            pagina = vehiculoRepository.findAll(pageable);
        }
        return pagina.map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> disponibles(LocalDate fechaInicio, LocalDate fechaFin,
                                              Long idCategoria, Long idSede) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new SolicitudInvalidaException("La fecha de fin no puede ser anterior a la de inicio");
        }
        return vehiculoRepository.buscarDisponibles(fechaInicio, fechaFin, idCategoria, idSede)
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public VehiculoResponse cambiarEstado(Long id, EstadoVehiculo nuevoEstado) {

        if (!vehiculoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe un vehículo con id " + id);
        }
        vehiculoRepository.actualizarEstadoVehiculo(id, nuevoEstado.name());
        return obtenerPorId(id);
    }

    private VehiculoResponse aRespuesta(Vehiculo v) {
        return new VehiculoResponse(
                v.getIdVehiculo(),
                v.getPlaca(),
                v.getMarca(),
                v.getModeloAnio(),
                v.getKilometraje(),
                v.getEstado(),
                v.getColor(),
                v.getCategoria().getIdCategoria(),
                v.getCategoria().getNombreCategoria(),
                v.getSede().getIdSede(),
                v.getSede().getNombreSede());
    }
}