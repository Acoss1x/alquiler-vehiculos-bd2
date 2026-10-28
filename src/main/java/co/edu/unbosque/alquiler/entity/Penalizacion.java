package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cargo por daños sobre una reserva. Solo se lee con JPA: crearlo y cambiar su
 * estado se hace con la función y el procedimiento de la base de datos.
 */
@Entity
@Table(name = "PENALIZACION")
public class Penalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_penalizacion")
    private Long idPenalizacion;

    @Column(name = "fecha_cargo", nullable = false)
    private LocalDate fechaCargo;

    @Column(name = "concepto", nullable = false, length = 200)
    private String concepto;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** PENDIENTE, PAGADA o ANULADA. */
    @Column(name = "estado_cargo", nullable = false, length = 20)
    private String estadoCargo;

    /** Referencia lógica a la inspección en MongoDB que documenta el daño. */
    @Column(name = "id_inspeccion_mongo", length = 24)
    private String idInspeccionMongo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id_reserva", nullable = false)
    private Reserva reserva;

    public Penalizacion() {}

    public Long getIdPenalizacion() { return idPenalizacion; }
    public LocalDate getFechaCargo() { return fechaCargo; }
    public String getConcepto() { return concepto; }
    public BigDecimal getMonto() { return monto; }
    public String getEstadoCargo() { return estadoCargo; }
    public String getIdInspeccionMongo() { return idInspeccionMongo; }
    public Reserva getReserva() { return reserva; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Penalizacion p)) return false;
        return Objects.equals(idPenalizacion, p.idPenalizacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPenalizacion);
    }
}