package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.SedeRequest;
import co.edu.unbosque.alquiler.dto.SedeResponse;
import co.edu.unbosque.alquiler.entity.Sede;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.SedeRepository;
import co.edu.unbosque.alquiler.repository.VehiculoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SedeService {

    private final SedeRepository sedeRepository;
    private final VehiculoRepository vehiculoRepository;

    public SedeService(SedeRepository sedeRepository, VehiculoRepository vehiculoRepository) {
        this.sedeRepository = sedeRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional
    public SedeResponse crear(SedeRequest request) {
        Sede sede = new Sede();
        aplicar(sede, request);
        return aRespuesta(sedeRepository.save(sede));
    }

    @Transactional(readOnly = true)
    public SedeResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<SedeResponse> listar(boolean incluirInactivos) {
        Sort orden = Sort.by("nombreSede");
        List<Sede> sedes = incluirInactivos
                ? sedeRepository.findAll(orden)
                : sedeRepository.findByEstado("ACTIVO", orden);
        return sedes.stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public SedeResponse cambiarEstado(Long id, String estado) {
        Sede sede = buscar(id);
        sede.setEstado(estado);
        return aRespuesta(sedeRepository.saveAndFlush(sede));   // flush: si el trigger rechaza, falla aquí
    }

    @Transactional
    public SedeResponse actualizar(Long id, SedeRequest request) {
        Sede sede = buscar(id);
        long ocupados = vehiculoRepository.countBySedeIdSede(id);
        if (request.capacidadEstacionamiento() < ocupados) {
            throw new SolicitudInvalidaException(
                    "La capacidad no puede ser menor que los vehículos que hay hoy en la sede (" + ocupados + ")");
        }
        aplicar(sede, request);
        return aRespuesta(sedeRepository.save(sede));
    }

    private Sede buscar(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una sede con id " + id));
    }

    private void aplicar(Sede sede, SedeRequest r) {
        sede.setNombreSede(r.nombreSede());
        sede.setCiudad(r.ciudad());
        sede.setDireccion(r.direccion());
        sede.setCapacidadEstacionamiento(r.capacidadEstacionamiento());
    }

    private SedeResponse aRespuesta(Sede s) {
        return new SedeResponse(s.getIdSede(), s.getNombreSede(), s.getCiudad(),
                s.getDireccion(), s.getCapacidadEstacionamiento(), s.getEstado());
    }
}
