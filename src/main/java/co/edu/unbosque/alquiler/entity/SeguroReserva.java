package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SEG_RES")
public class SeguroReserva {

    @EmbeddedId
    private SeguroReservaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idSeguro")
    @JoinColumn(name = "SEGURO_id_seguro")
    private Seguro seguro;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idReserva")
    @JoinColumn(name = "RESERVA_id_reserva")
    private Reserva reserva;

    @Column(name = "costo_contratado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoContratado;

    public SeguroReserva() {}

    public SeguroReservaId getId() { return id; }
    public void setId(SeguroReservaId id) { this.id = id; }

    public Seguro getSeguro() { return seguro; }
    public void setSeguro(Seguro seguro) { this.seguro = seguro; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public BigDecimal getCostoContratado() { return costoContratado; }
    public void setCostoContratado(BigDecimal costoContratado) { this.costoContratado = costoContratado; }
}
