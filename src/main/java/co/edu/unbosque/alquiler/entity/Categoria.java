package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "CATEGORIA")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(name = "nombre_categoria", nullable = false, length = 50)
    private String nombreCategoria;

    @Column(name = "capacidad_pasajeros", nullable = false)
    private Integer capacidadPasajeros;

    @Column(name = "tarifa_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaDiaria;

    @Column(name = "tipo_transmision", nullable = false, length = 20)
    private String tipoTransmision;

    /** ACTIVO o INACTIVO. Una categoría inactiva no se ofrece para reservar. */
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ACTIVO";

    public Categoria() {}

    public Long getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Long idCategoria) { this.idCategoria = idCategoria; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public Integer getCapacidadPasajeros() { return capacidadPasajeros; }
    public void setCapacidadPasajeros(Integer capacidadPasajeros) { this.capacidadPasajeros = capacidadPasajeros; }

    public BigDecimal getTarifaDiaria() { return tarifaDiaria; }
    public void setTarifaDiaria(BigDecimal tarifaDiaria) { this.tarifaDiaria = tarifaDiaria; }

    public String getTipoTransmision() { return tipoTransmision; }
    public void setTipoTransmision(String tipoTransmision) { this.tipoTransmision = tipoTransmision; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria categoria)) return false;
        return Objects.equals(idCategoria, categoria.idCategoria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCategoria);
    }
}