package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "VEHICULO")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Long idVehiculo;

    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "modelo_anio", nullable = false)
    private Integer modeloAnio;

    @Column(name = "kilometraje", nullable = false)
    private Integer kilometraje;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "color", length = 30)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORIA_id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SEDE_id_sede", nullable = false)
    private Sede sede;

    public Vehiculo() {}

    public Long getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(Long idVehiculo) { this.idVehiculo = idVehiculo; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Integer getModeloAnio() { return modeloAnio; }
    public void setModeloAnio(Integer modeloAnio) { this.modeloAnio = modeloAnio; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehiculo vehiculo)) return false;
        return Objects.equals(idVehiculo, vehiculo.idVehiculo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVehiculo);
    }
}