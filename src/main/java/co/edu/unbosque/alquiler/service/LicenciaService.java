package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.LicenciaRequest;
import co.edu.unbosque.alquiler.dto.LicenciaResponse;
import co.edu.unbosque.alquiler.entity.Licencia;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import co.edu.unbosque.alquiler.repository.LicenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la licencia de conducción de un cliente (relación 1:1).
 */
@Service
public class LicenciaService {

    private final LicenciaRepository licenciaRepository;
    private final ClienteRepository clienteRepository;

    public LicenciaService(LicenciaRepository licenciaRepository, ClienteRepository clienteRepository) {
        this.licenciaRepository = licenciaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public LicenciaResponse registrar(Long idCliente, LicenciaRequest request) {
        // Se comprueba la existencia aquí solo para devolver un 404 claro.
        if (!clienteRepository.existsById(idCliente)) {
            throw new RecursoNoEncontradoException("No existe un cliente con id " + idCliente);
        }

        licenciaRepository.registrarLicencia(
                idCliente,
                request.numeroLicencia(),
                request.categoriaLicencia(),
                request.fechaExpedicion(),
                request.fechaVencimiento());

        return obtenerPorCliente(idCliente);
    }

    @Transactional(readOnly = true)
    public LicenciaResponse obtenerPorCliente(Long idCliente) {
        return aRespuesta(licenciaRepository.findByClienteIdCliente(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El cliente " + idCliente + " no tiene licencia registrada")));
    }

    private LicenciaResponse aRespuesta(Licencia l) {
        return new LicenciaResponse(
                l.getIdLicencia(),
                l.getCliente().getIdCliente(),
                l.getNumeroLicencia(),
                l.getCategoriaLicencia(),
                l.getFechaExpedicion(),
                l.getFechaVencimiento());
    }
}
