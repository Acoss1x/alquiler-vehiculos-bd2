package co.edu.unbosque.alquiler.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "ASESOR")
public class Asesor extends Empleado {

    @Column(name = "mostrador_asignado", nullable = false)
    private Integer mostradorAsignado;

    public Asesor() {}

    public Integer getMostradorAsignado() { return mostradorAsignado; }
    public void setMostradorAsignado(Integer mostradorAsignado) { this.mostradorAsignado = mostradorAsignado; }
}
