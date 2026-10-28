package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.CambiarClaveRequest;
import co.edu.unbosque.alquiler.dto.ClaveTemporalResponse;
import co.edu.unbosque.alquiler.dto.ClienteResponse;
import co.edu.unbosque.alquiler.dto.LoginRequest;
import co.edu.unbosque.alquiler.dto.LoginResponse;
import co.edu.unbosque.alquiler.dto.PerfilResponse;
import co.edu.unbosque.alquiler.dto.RegistroRequest;
import co.edu.unbosque.alquiler.dto.RestablecerClaveRequest;
import co.edu.unbosque.alquiler.entity.Administrador;
import co.edu.unbosque.alquiler.entity.Asesor;
import co.edu.unbosque.alquiler.entity.Cliente;
import co.edu.unbosque.alquiler.entity.Empleado;
import co.edu.unbosque.alquiler.entity.Tecnico;
import co.edu.unbosque.alquiler.exception.CredencialesInvalidasException;
import co.edu.unbosque.alquiler.exception.CuentaNoActivaException;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import co.edu.unbosque.alquiler.repository.EmpleadoRepository;
import co.edu.unbosque.alquiler.security.JwtService;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Inicio de sesión, registro, perfil y manejo de contraseñas. Las credenciales viven
 * en las tablas cliente y empleado; aquí solo se comparan hashes BCrypt y se emite el JWT.
 */
@Service
public class AuthService {

    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String TIPO_EMPLEADO = "EMPLEADO";

    /**
     * Hash de relleno: si el correo no existe se compara igual contra un hash, para
     * que el tiempo de respuesta no delate qué correos están registrados.
     */
    private static final String HASH_RELLENO = "$2a$10$W/iz47lH7LTk5APDAynlh.lHB/IjVv/VVryT5fgjGc7n293X7RYtu";

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final int LONGITUD_CLAVE_TEMPORAL = 12;

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteService clienteService;
    private final LicenciaService licenciaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityManager entityManager;
    private final SecureRandom aleatorio = new SecureRandom();

    public AuthService(ClienteRepository clienteRepository,
                       EmpleadoRepository empleadoRepository,
                       ClienteService clienteService,
                       LicenciaService licenciaService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EntityManager entityManager) {
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.clienteService = clienteService;
        this.licenciaService = licenciaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }

    /** Cuenta de acceso (cliente o empleado) con lo necesario para autenticar. */
    private record Cuenta(String tipo, String rol, Long id, String nombre, String correo,
                          String estado, String hash, boolean debeCambiarClave) {

        PerfilResponse aPerfil() {
            return new PerfilResponse(id, tipo, rol, nombre, correo, estado, debeCambiarClave);
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Optional<Cuenta> encontrada = buscarPorCorreo(correo);

        String hash = encontrada.map(Cuenta::hash).orElse(HASH_RELLENO);
        boolean coincide = passwordEncoder.matches(request.clave(), hash);
        if (encontrada.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException();
        }

        Cuenta cuenta = encontrada.get();
        if (!"ACTIVO".equals(cuenta.estado())) {
            throw new CuentaNoActivaException(cuenta.estado());
        }
        return emitir(cuenta);
    }

    /** El cliente se registra por sí mismo (con licencia opcional) y queda con sesión iniciada. */
    @Transactional
    public LoginResponse registro(RegistroRequest request) {
        ClienteResponse creado = clienteService.registrar(request.cliente(), false);
        if (request.licencia() != null) {
            licenciaService.registrar(creado.idCliente(), request.licencia());
        }
        return emitir(buscarCuenta(TIPO_CLIENTE, creado.idCliente()));
    }

    @Transactional(readOnly = true)
    public PerfilResponse perfil(String tipo, Long id) {
        return buscarCuenta(tipo, id).aPerfil();
    }

    /** Cambia la clave de quien está autenticado y devuelve un token nuevo (sin la marca de clave temporal). */
    @Transactional
    public LoginResponse cambiarClave(String tipo, Long id, CambiarClaveRequest request) {
        Cuenta cuenta = buscarCuenta(tipo, id);
        if (!passwordEncoder.matches(request.claveActual(), cuenta.hash())) {
            throw new SolicitudInvalidaException("La contraseña actual no es correcta");
        }
        if (request.claveActual().equals(request.claveNueva())) {
            throw new SolicitudInvalidaException("La contraseña nueva debe ser distinta de la actual");
        }
        clienteRepository.cambiarClave(tipo, id, passwordEncoder.encode(request.claveNueva()), false);
        // El procedimiento cambió la fila por SQL: se vacía la caché de JPA para releerla actualizada
        // (si no, el token nuevo saldría con la marca de clave temporal anterior).
        entityManager.clear();
        return emitir(buscarCuenta(tipo, id));
    }

    /**
     * Restablece la contraseña de otra persona con una clave temporal que debe cambiar al entrar.
     * Un administrador puede restablecer a cualquiera; un asesor, solo a clientes.
     */
    @Transactional
    public ClaveTemporalResponse restablecerClave(String rolActor, RestablecerClaveRequest request) {
        boolean permitido = "ADMINISTRADOR".equals(rolActor)
                || ("ASESOR".equals(rolActor) && TIPO_CLIENTE.equals(request.tipo()));
        if (!permitido) {
            throw new AccessDeniedException("No tiene permisos para restablecer esta contraseña");
        }
        buscarCuenta(request.tipo(), request.id());   // 404 si no existe

        String temporal = generarClaveTemporal();
        clienteRepository.cambiarClave(request.tipo(), request.id(), passwordEncoder.encode(temporal), true);
        return new ClaveTemporalResponse(temporal);
    }

    // ---------- Apoyo ----------

    private LoginResponse emitir(Cuenta cuenta) {
        PerfilResponse perfil = cuenta.aPerfil();
        return new LoginResponse(jwtService.generar(perfil), "Bearer", jwtService.segundosDeVida(), perfil);
    }

    private Optional<Cuenta> buscarPorCorreo(String correo) {
        Optional<Cliente> cliente = clienteRepository.findByCorreoIgnoreCase(correo);
        if (cliente.isPresent()) {
            return Optional.of(deCliente(cliente.get()));
        }
        return empleadoRepository.findByCorreoIgnoreCase(correo).map(this::deEmpleado);
    }

    private Cuenta buscarCuenta(String tipo, Long id) {
        if (TIPO_CLIENTE.equals(tipo)) {
            return clienteRepository.findById(id).map(this::deCliente)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id));
        }
        if (TIPO_EMPLEADO.equals(tipo)) {
            return empleadoRepository.findById(id).map(this::deEmpleado)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un empleado con id " + id));
        }
        throw new SolicitudInvalidaException("Tipo de cuenta no válido: " + tipo);
    }

    private Cuenta deCliente(Cliente c) {
        return new Cuenta(TIPO_CLIENTE, "CLIENTE", c.getIdCliente(),
                c.getPrimerNombre() + " " + c.getPrimerApellido(), c.getCorreo(),
                c.getEstado(), c.getPasswordHash(), c.isDebeCambiarClave());
    }

    private Cuenta deEmpleado(Empleado e) {
        String rol;
        if (e instanceof Administrador) {
            rol = "ADMINISTRADOR";
        } else if (e instanceof Asesor) {
            rol = "ASESOR";
        } else if (e instanceof Tecnico) {
            rol = "TECNICO";
        } else {
            throw new IllegalStateException("El empleado " + e.getIdEmpleado() + " no tiene subtipo");
        }
        return new Cuenta(TIPO_EMPLEADO, rol, e.getIdEmpleado(),
                e.getPrimerNombre() + " " + e.getPrimerApellido(), e.getCorreo(),
                e.getEstado(), e.getPasswordHash(), e.isDebeCambiarClave());
    }

    /** 12 caracteres con al menos una mayúscula, una minúscula y un número (sin caracteres ambiguos). */
    private String generarClaveTemporal() {
        String todos = MAYUSCULAS + MINUSCULAS + DIGITOS;
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(MAYUSCULAS.charAt(aleatorio.nextInt(MAYUSCULAS.length())));
        caracteres.add(MINUSCULAS.charAt(aleatorio.nextInt(MINUSCULAS.length())));
        caracteres.add(DIGITOS.charAt(aleatorio.nextInt(DIGITOS.length())));
        while (caracteres.size() < LONGITUD_CLAVE_TEMPORAL) {
            caracteres.add(todos.charAt(aleatorio.nextInt(todos.length())));
        }
        Collections.shuffle(caracteres, aleatorio);
        StringBuilder clave = new StringBuilder();
        for (char c : caracteres) {
            clave.append(c);
        }
        return clave.toString();
    }
}