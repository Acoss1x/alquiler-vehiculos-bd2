package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.RestablecerClaveRequest;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.ClienteRepository;
import co.edu.unbosque.alquiler.repository.EmpleadoRepository;
import co.edu.unbosque.alquiler.repository.TokenRecuperacionRepository;
import co.edu.unbosque.alquiler.repository.TokenRecuperacionRepository.CuentaRef;
import co.edu.unbosque.alquiler.security.LimitadorSolicitudes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Recuperación de contraseña por correo: la persona pide un enlace, le llega a su correo
 * registrado y con él define una contraseña nueva. El enlace lleva un token aleatorio que
 * vence y solo se puede usar una vez (en la base solo se guarda su hash).
 */
@Service
public class RecuperacionClaveService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionClaveService.class);

    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String TIPO_EMPLEADO = "EMPLEADO";

    /** Cuenta a la que se le envía el enlace. */
    private record Destino(String tipo, Long id, String nombre, String correo) {
    }

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender correo;
    private final LimitadorSolicitudes limitador;
    private final String urlFront;
    private final int minutosDeVida;
    private final String remitente;
    private final boolean mostrarEnlaceEnLog;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionClaveService(ClienteRepository clienteRepository,
                                    EmpleadoRepository empleadoRepository,
                                    TokenRecuperacionRepository tokens,
                                    PasswordEncoder passwordEncoder,
                                    JavaMailSender correo,
                                    LimitadorSolicitudes limitador,
                                    @Value("${app.frontend-url:http://localhost:4200}") String urlFront,
                                    @Value("${app.recuperacion.minutos:30}") int minutosDeVida,
                                    @Value("${spring.mail.username:}") String remitente,
                                    @Value("${app.recuperacion.mostrar-enlace-en-log:false}") boolean mostrarEnlaceEnLog) {
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.limitador = limitador;
        this.urlFront = urlFront.endsWith("/") ? urlFront.substring(0, urlFront.length() - 1) : urlFront;
        this.minutosDeVida = minutosDeVida;
        this.remitente = remitente;
        this.mostrarEnlaceEnLog = mostrarEnlaceEnLog;
    }

    /**
     * Envía el enlace al correo si pertenece a una cuenta activa. No dice si el correo existe
     * (la respuesta es siempre la misma) y limita las solicitudes por correo.
     */
    public void olvideClave(String correoIngresado) {
        String direccion = correoIngresado.trim().toLowerCase();
        if (!limitador.permitir(direccion)) {
            return;
        }
        Optional<Destino> destino = buscarActivo(direccion);
        if (destino.isEmpty()) {
            return;
        }
        Destino d = destino.get();
        String token = nuevoToken();
        tokens.crear(d.tipo(), d.id(), hash(token), minutosDeVida);

        String enlace = urlFront + "/restablecer?token=" + token;
        if (mostrarEnlaceEnLog) {
            log.warn("[DESARROLLO] Enlace de recuperación para {}: {}", d.correo(), enlace);
        }
        // En segundo plano: así la respuesta no tarda más cuando el correo sí existe.
        CompletableFuture.runAsync(() -> enviar(d, enlace));
    }

    /** Define la contraseña nueva con el token del enlace; el token queda inutilizado. */
    @Transactional
    public void restablecer(RestablecerClaveRequest request) {
        CuentaRef cuenta = tokens.consumir(hash(request.token().trim()))
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "El enlace no es válido o ya venció. Solicita uno nuevo"));
        clienteRepository.cambiarClave(cuenta.tipo(), cuenta.idCuenta(),
                passwordEncoder.encode(request.claveNueva()), false);
    }

    // ---------- Apoyo ----------

    private Optional<Destino> buscarActivo(String direccion) {
        Optional<Destino> cliente = clienteRepository.findByCorreoIgnoreCase(direccion)
                .filter(c -> "ACTIVO".equals(c.getEstado()))
                .map(c -> new Destino(TIPO_CLIENTE, c.getIdCliente(), c.getPrimerNombre(), c.getCorreo()));
        if (cliente.isPresent()) {
            return cliente;
        }
        return empleadoRepository.findByCorreoIgnoreCase(direccion)
                .filter(e -> "ACTIVO".equals(e.getEstado()))
                .map(e -> new Destino(TIPO_EMPLEADO, e.getIdEmpleado(), e.getPrimerNombre(), e.getCorreo()));
    }

    private void enviar(Destino destino, String enlace) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (!remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(destino.correo());
            mensaje.setSubject("RutaCar: restablece tu contraseña");
            mensaje.setText("Hola " + destino.nombre() + ",\n\n"
                    + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de RutaCar. "
                    + "Abre este enlace para crear una nueva (vence en " + minutosDeVida
                    + " minutos y solo se puede usar una vez):\n\n"
                    + enlace + "\n\n"
                    + "Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.\n\n"
                    + "RutaCar");
            correo.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de recuperación a {}: {}", destino.correo(), e.getMessage());
        }
    }

    /** 256 bits aleatorios, en Base64 apto para URL. */
    private String nuevoToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
