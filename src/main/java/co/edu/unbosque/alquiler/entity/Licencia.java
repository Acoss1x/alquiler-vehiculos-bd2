package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "LICENCIA")
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_licencia")
    private Long idLicencia;

    @Column(name = "numero_licencia", nullable = false, unique = true, length = 50)
    private String numeroLicencia;

    @Column(name = "categoria_licencia", nullable = false, length = 5)
    private String categoriaLicencia;

    @Column(name = "fecha_expedicion", nullable = false)
    private LocalDate fechaExpedicion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    public Licencia() {}

    public Long getIdLicencia() { return idLicencia; }
    public void setIdLicencia(Long idLicencia) { this.idLicencia = idLicencia; }

    public String getNumeroLicencia() { return numeroLicencia; }
    public void setNumeroLicencia(String numeroLicencia) { this.numeroLicencia = numeroLicencia; }

    public String getCategoriaLicencia() { return categoriaLicencia; }
    public void setCategoriaLicencia(String categoriaLicencia) { this.categoriaLicencia = categoriaLicencia; }

    public LocalDate getFechaExpedicion() { return fechaExpedicion; }
    public void setFechaExpedicion(LocalDate fechaExpedicion) { this.fechaExpedicion = fechaExpedicion; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
