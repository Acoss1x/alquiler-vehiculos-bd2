package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.SeguroRequest;
import co.edu.unbosque.alquiler.dto.SeguroResponse;
import co.edu.unbosque.alquiler.entity.Seguro;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.repository.SeguroRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de seguros. Uno inactivo no se puede contratar en reservas nuevas
 * (lo hace cumplir la base de datos); las reservas existentes conservan el suyo.
 */
@Service
public class SeguroService {

    private final SeguroRepository seguroRepository;

    public SeguroService(SeguroRepository seguroRepository) {
        this.seguroRepository = seguroRepository;
    }

    @Transactional(readOnly = true)
    public List<SeguroResponse> listar(boolean incluirInactivos) {
        Sort orden = Sort.by("nombreSeguro");
        List<Seguro> seguros = incluirInactivos
                ? seguroRepository.findAll(orden)
                : seguroRepository.findByEstado("ACTIVO", orden);
        return seguros.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public SeguroResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public SeguroResponse crear(SeguroRequest request) {
        Seguro seguro = new Seguro();
        aplicar(seguro, request);
        return aRespuesta(seguroRepository.saveAndFlush(seguro));
    }

    @Transactional
    public SeguroResponse actualizar(Long id, SeguroRequest request) {
        Seguro seguro = buscar(id);
        aplicar(seguro, request);
        return aRespuesta(seguroRepository.saveAndFlush(seguro));
    }

    @Transactional
    public SeguroResponse cambiarEstado(Long id, String estado) {
        Seguro seguro = buscar(id);
        seguro.setEstado(estado);
        return aRespuesta(seguroRepository.saveAndFlush(seguro));
    }

    private Seguro buscar(Long id) {
        return seguroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un seguro con id " + id));
    }

    private void aplicar(Seguro s, SeguroRequest r) {
        s.setNombreSeguro(r.nombreSeguro());
        s.setDescripcion(r.descripcion());
        s.setCostoDiario(r.costoDiario());
    }

    private SeguroResponse aRespuesta(Seguro s) {
        return new SeguroResponse(s.getIdSeguro(), s.getNombreSeguro(),
                s.getDescripcion(), s.getCostoDiario(), s.getEstado());
    }
}