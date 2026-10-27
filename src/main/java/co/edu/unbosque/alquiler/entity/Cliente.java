package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entidad que representa a un cliente en el sistema de alquiler de vehículos.
 * <p>
 * Se mapea a la tabla {@code CLIENTE} en la base de datos relacional.
 * Su función principal en la aplicación web es facilitar las consultas (SELECT),
 * mientras que las transacciones críticas se delegan a procedimientos almacenados.
 * </p>
 */
@Entity
@Table(name = "CLIENTE")
public class Cliente {

    /**
     * Identificador único del cliente, generado automáticamente por PostgreSQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    /**
     * Número de documento de identidad. No puede ser nulo y debe ser único.
     */
    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;

    @Column(name = "primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 50)
    private String segundoApellido;

    /**
     * Correo electrónico de contacto. Único por cliente.
     */
    @Column(name = "correo", nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    /**
     * Estado del cliente (por ejemplo, ACTIVO / INACTIVO).
     */
    @Column(name = "estado", nullable = false, length = 15)
    private String estado;


    /**
     * Hash BCrypt de la contraseña. Nunca se devuelve en las respuestas.
     */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * TRUE si la clave es temporal y debe cambiarse al iniciar sesión.
     */
    @Column(name = "debe_cambiar_clave", nullable = false)
    private boolean debeCambiarClave;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public Cliente() {
    }

    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isDebeCambiarClave() { return debeCambiarClave; }
    public void setDebeCambiarClave(boolean debeCambiarClave) { this.debeCambiarClave = debeCambiarClave; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente cliente)) return false;
        return Objects.equals(idCliente, cliente.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }
}