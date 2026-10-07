package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SEGURO")
public class Seguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguro")
    private Long idSeguro;

    @Column(name = "nombre_seguro", nullable = false, length = 50)
    private String nombreSeguro;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "costo_diario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoDiario;

    public Seguro() {}

    public Long getIdSeguro() { return idSeguro; }
    public void setIdSeguro(Long idSeguro) { this.idSeguro = idSeguro; }

    public String getNombreSeguro() { return nombreSeguro; }
    public void setNombreSeguro(String nombreSeguro) { this.nombreSeguro = nombreSeguro; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getCostoDiario() { return costoDiario; }
    public void setCostoDiario(BigDecimal costoDiario) { this.costoDiario = costoDiario; }
}
