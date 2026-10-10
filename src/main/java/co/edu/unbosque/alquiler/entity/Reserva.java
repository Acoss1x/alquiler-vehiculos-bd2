package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad transaccional central del sistema.
 * Conecta al cliente, el vehículo, el asesor que la registra, las sedes de
 * entrega/recibo y los pagos.
 */
@Entity
@Table(name = "RESERVA")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "estado_reserva", nullable = false, length = 20)
    private String estadoReserva;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Precio pactado al momento de crear la reserva (se conserva como histórico).
     */
    @Column(name = "costo_total", precision = 10, scale = 2)
    private BigDecimal costoTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VEHICULO_id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    /**
     * Asesor que registra y atiende la reserva (obligatorio).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASESOR_id_empleado", nullable = false)
    private Asesor asesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_entrega", nullable = false)
    private Sede sedeEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_recibe", nullable = false)
    private Sede sedeRecibe;

    /**
     * Pagos de la reserva. Se registran vía procedimiento almacenado, por eso
     * no hay cascada desde aquí.
     */
    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    public Reserva() {}

    public Long getIdReserva() { return idReserva; }
    public void setIdReserva(Long idReserva) { this.idReserva = idReserva; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public Asesor getAsesor() { return asesor; }
    public void setAsesor(Asesor asesor) { this.asesor = asesor; }

    public Sede getSedeEntrega() { return sedeEntrega; }
    public void setSedeEntrega(Sede sedeEntrega) { this.sedeEntrega = sedeEntrega; }

    public Sede getSedeRecibe() { return sedeRecibe; }
    public void setSedeRecibe(Sede sedeRecibe) { this.sedeRecibe = sedeRecibe; }

    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva reserva)) return false;
        return Objects.equals(idReserva, reserva.idReserva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva);
    }
}