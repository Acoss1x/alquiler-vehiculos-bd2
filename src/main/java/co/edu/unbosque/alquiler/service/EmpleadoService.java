package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.EmpleadoRequest;
import co.edu.unbosque.alquiler.dto.EmpleadoResponse;
import co.edu.unbosque.alquiler.entity.Administrador;
import co.edu.unbosque.alquiler.entity.Asesor;
import co.edu.unbosque.alquiler.entity.Empleado;
import co.edu.unbosque.alquiler.entity.Sede;
import co.edu.unbosque.alquiler.entity.Tecnico;
import co.edu.unbosque.alquiler.entity.TipoEmpleado;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.AdministradorRepository;
import co.edu.unbosque.alquiler.repository.AsesorRepository;
import co.edu.unbosque.alquiler.repository.EmpleadoRepository;
import co.edu.unbosque.alquiler.repository.SedeRepository;
import co.edu.unbosque.alquiler.repository.TecnicoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hibernate inserta la fila de EMPLEADO y la del subtipo en la misma transacción,
 * por eso el trigger diferido de "especialización total" se cumple al COMMIT.
 */
@Service
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final AsesorRepository asesorRepository;
    private final TecnicoRepository tecnicoRepository;
    private final AdministradorRepository administradorRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           AsesorRepository asesorRepository,
                           TecnicoRepository tecnicoRepository,
                           AdministradorRepository administradorRepository,
                           SedeRepository sedeRepository,
                           PasswordEncoder passwordEncoder) {
        this.empleadoRepository = empleadoRepository;
        this.asesorRepository = asesorRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.administradorRepository = administradorRepository;
        this.sedeRepository = sedeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest request) {
        Sede sede = sedeRepository.findById(request.idSede())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una sede con id " + request.idSede()));

        Empleado empleado = switch (request.tipo()) {
            case ASESOR -> {
                exigir(request.mostradorAsignado() != null, "El mostrador asignado es obligatorio para un asesor");
                Asesor asesor = new Asesor();
                asesor.setMostradorAsignado(request.mostradorAsignado());
                yield asesor;
            }
            case TECNICO -> {
                exigir(hayTexto(request.especialidad()), "La especialidad es obligatoria para un técnico");
                Tecnico tecnico = new Tecnico();
                tecnico.setEspecialidad(request.especialidad());
                yield tecnico;
            }
            case ADMINISTRADOR -> {
                exigir(hayTexto(request.nivelAcceso()), "El nivel de acceso es obligatorio para un administrador");
                Administrador admin = new Administrador();
                admin.setNivelAcceso(request.nivelAcceso());
                admin.setBonoGerencial(request.bonoGerencial());
                yield admin;
            }
        };

        empleado.setNumeroDocumento(request.numeroDocumento());
        empleado.setPrimerNombre(request.primerNombre());
        empleado.setPrimerApellido(request.primerApellido());
        empleado.setFechaContratacion(request.fechaContratacion());
        empleado.setEstado("ACTIVO");
        empleado.setCorreo(request.correo().trim().toLowerCase());
        empleado.setPasswordHash(passwordEncoder.encode(request.clave()));
        empleado.setDebeCambiarClave(true);   // la clave la define el administrador: se cambia al primer ingreso
        empleado.setSede(sede);
        return aRespuesta(empleadoRepository.saveAndFlush(empleado));
    }

    @Transactional(readOnly = true)
    public EmpleadoResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    /** Filtros opcionales: tipo y sede. Sirve para llenar listas de asesores o técnicos. */
    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listar(TipoEmpleado tipo, Long idSede) {
        List<? extends Empleado> empleados = tipo == null ? empleadoRepository.findAll() : porTipo(tipo);
        return empleados.stream()
                .filter(e -> idSede == null || e.getSede().getIdSede().equals(idSede))
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public EmpleadoResponse cambiarEstado(Long id, String nuevoEstado) {
        Empleado empleado = buscar(id);
        empleado.setEstado(nuevoEstado);
        return aRespuesta(empleadoRepository.save(empleado));
    }

    private List<? extends Empleado> porTipo(TipoEmpleado tipo) {
        return switch (tipo) {
            case ASESOR -> asesorRepository.findAll();
            case TECNICO -> tecnicoRepository.findAll();
            case ADMINISTRADOR -> administradorRepository.findAll();
        };
    }

    private Empleado buscar(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un empleado con id " + id));
    }

    private void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new SolicitudInvalidaException(mensaje);
        }
    }

    private boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private EmpleadoResponse aRespuesta(Empleado e) {
        TipoEmpleado tipo;
        Integer mostrador = null;
        String especialidad = null;
        String nivelAcceso = null;
        BigDecimal bono = null;

        if (e instanceof Asesor a) {
            tipo = TipoEmpleado.ASESOR;
            mostrador = a.getMostradorAsignado();
        } else if (e instanceof Tecnico t) {
            tipo = TipoEmpleado.TECNICO;
            especialidad = t.getEspecialidad();
        } else if (e instanceof Administrador ad) {
            tipo = TipoEmpleado.ADMINISTRADOR;
            nivelAcceso = ad.getNivelAcceso();
            bono = ad.getBonoGerencial();
        } else {
            throw new IllegalStateException("El empleado " + e.getIdEmpleado() + " no tiene subtipo");
        }

        return new EmpleadoResponse(
                e.getIdEmpleado(), e.getNumeroDocumento(), e.getPrimerNombre(), e.getPrimerApellido(), e.getCorreo(),
                e.getFechaContratacion(), e.getEstado(),
                e.getSede().getIdSede(), e.getSede().getNombreSede(),
                tipo, mostrador, especialidad, nivelAcceso, bono);
    }
}