package co.edu.unbosque.alquiler.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ADMINISTRADOR")
public class Administrador extends Empleado {

    @Column(name = "nivel_acceso", nullable = false, length = 30)
    private String nivelAcceso;

    @Column(name = "bono_gerencial", precision = 10, scale = 2)
    private BigDecimal bonoGerencial;

    public Administrador() {}

    public String getNivelAcceso() { return nivelAcceso; }
    public void setNivelAcceso(String nivelAcceso) { this.nivelAcceso = nivelAcceso; }

    public BigDecimal getBonoGerencial() { return bonoGerencial; }
    public void setBonoGerencial(BigDecimal bonoGerencial) { this.bonoGerencial = bonoGerencial; }
}
