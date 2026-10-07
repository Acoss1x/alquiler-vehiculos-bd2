package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad que representa a un cliente en el sistema de alquiler de vehículos.
 * <p>
 * Se mapea a la tabla {@code CLIENTE} en la base de datos relacional.
 * Su función principal en la aplicación web es facilitar las consultas (SELECT),
 * mientras que las transacciones críticas se delegarán a procedimientos almacenados.
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

    /**
     * Primer nombre del cliente.
     */
    @Column(name = "primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    /**
     * Primer apellido del cliente.
     */
    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    /**
     * Dirección de correo electrónico de contacto.
     */
    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    /**
     * Número de teléfono de contacto.
     */
    @Column(name = "telefono", length = 15)
    private String telefono;

    /**
     * Fecha en la que el cliente fue registrado en el sistema.
     */
    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro = LocalDate.now();

    /**
     * Constructor por defecto requerido por JPA.
     */
    public Cliente() {
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

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
