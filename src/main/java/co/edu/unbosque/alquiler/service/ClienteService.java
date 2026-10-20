package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.ClienteRequest;
import co.edu.unbosque.alquiler.dto.ClienteResponse;
import co.edu.unbosque.alquiler.entity.Cliente;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.alquiler.dto.ClienteUpdateRequest;

/**
 * Casos de uso de cliente. Las escrituras con reglas de negocio se delegan
 * a la base de datos; las lecturas usan el repositorio.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponse registrar(ClienteRequest request) {
        clienteRepository.registrarClienteTransaccional(
                request.numeroDocumento(),
                request.primerNombre(),
                request.segundoNombre(),
                request.primerApellido(),
                request.segundoApellido(),
                request.correo(),
                request.telefono());

        // El SP no devuelve el id: se consulta por el documento, que es único.
        return buscarPorDocumento(request.numeroDocumento());
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        return aRespuesta(clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id)));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String numeroDocumento) {
        return aRespuesta(clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un cliente con documento " + numeroDocumento)));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(this::aRespuesta);
    }

    @Transactional
    public ClienteResponse cambiarEstado(Long id, String nuevoEstado) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id));
        cliente.setEstado(nuevoEstado);
        return aRespuesta(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id));
        cliente.setPrimerNombre(request.primerNombre());
        cliente.setSegundoNombre(request.segundoNombre());
        cliente.setPrimerApellido(request.primerApellido());
        cliente.setSegundoApellido(request.segundoApellido());
        cliente.setCorreo(request.correo());
        cliente.setTelefono(request.telefono());
        return aRespuesta(clienteRepository.saveAndFlush(cliente));
    }

    private ClienteResponse aRespuesta(Cliente c) {
        return new ClienteResponse(
                c.getIdCliente(),
                c.getNumeroDocumento(),
                c.getPrimerNombre(),
                c.getSegundoNombre(),
                c.getPrimerApellido(),
                c.getSegundoApellido(),
                c.getCorreo(),
                c.getTelefono(),
                c.getEstado());
    }
}
