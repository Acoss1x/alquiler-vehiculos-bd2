package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.CategoriaRequest;
import co.edu.unbosque.alquiler.dto.CategoriaResponse;
import co.edu.unbosque.alquiler.entity.Categoria;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.repository.CategoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cambiar la tarifa NO afecta reservas existentes: su costo_total ya está congelado.
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        aplicar(categoria, request);
        return aRespuesta(categoriaRepository.saveAndFlush(categoria));
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombreCategoria")).stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        aplicar(categoria, request);
        return aRespuesta(categoriaRepository.saveAndFlush(categoria));
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una categoría con id " + id));
    }

    private void aplicar(Categoria c, CategoriaRequest r) {
        c.setNombreCategoria(r.nombreCategoria());
        c.setCapacidadPasajeros(r.capacidadPasajeros());
        c.setTarifaDiaria(r.tarifaDiaria());
        c.setTipoTransmision(r.tipoTransmision());
    }

    private CategoriaResponse aRespuesta(Categoria c) {
        return new CategoriaResponse(c.getIdCategoria(), c.getNombreCategoria(),
                c.getCapacidadPasajeros(), c.getTarifaDiaria(), c.getTipoTransmision());
    }
}
