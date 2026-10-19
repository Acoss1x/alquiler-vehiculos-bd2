package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.SeguroResponse;
import co.edu.unbosque.alquiler.repository.SeguroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta del catálogo de seguros disponibles para contratar en una reserva.
 */
@Service
public class SeguroService {

    private final SeguroRepository seguroRepository;

    public SeguroService(SeguroRepository seguroRepository) {
        this.seguroRepository = seguroRepository;
    }

    @Transactional(readOnly = true)
    public List<SeguroResponse> listar() {
        return seguroRepository.findAll().stream()
                .map(s -> new SeguroResponse(s.getIdSeguro(), s.getNombreSeguro(),
                        s.getDescripcion(), s.getCostoDiario()))
                .toList();
    }
}
