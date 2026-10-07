package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SeguroReservaId implements Serializable {

    private Long idSeguro;
    private Long idReserva;

    public SeguroReservaId() {}

    public SeguroReservaId(Long idSeguro, Long idReserva) {
        this.idSeguro = idSeguro;
        this.idReserva = idReserva;
    }

    public Long getIdSeguro() { return idSeguro; }
    public void setIdSeguro(Long idSeguro) { this.idSeguro = idSeguro; }

    public Long getIdReserva() { return idReserva; }
    public void setIdReserva(Long idReserva) { this.idReserva = idReserva; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeguroReservaId that)) return false;
        return Objects.equals(idSeguro, that.idSeguro) && Objects.equals(idReserva, that.idReserva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSeguro, idReserva);
    }
}
