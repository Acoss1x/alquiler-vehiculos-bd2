package co.edu.unbosque.alquiler.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "SEDE")
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Long idSede;

    @Column(name = "nombre_sede", nullable = false, length = 100)
    private String nombreSede;

    @Column(name = "ciudad", nullable = false, length = 50)
    private String ciudad;

    @Column(name = "direccion", nullable = false, length = 150)
    private String direccion;

    @Column(name = "capacidad_estacionamiento", nullable = false)
    private Integer capacidadEstacionamiento;

    /** ACTIVO o INACTIVO. Una sede inactiva no se ofrece para reservar. */
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ACTIVO";

    public Sede() {}

    public Long getIdSede() { return idSede; }
    public void setIdSede(Long idSede) { this.idSede = idSede; }

    public String getNombreSede() { return nombreSede; }
    public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Integer getCapacidadEstacionamiento() { return capacidadEstacionamiento; }
    public void setCapacidadEstacionamiento(Integer capacidadEstacionamiento) { this.capacidadEstacionamiento = capacidadEstacionamiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sede sede)) return false;
        return Objects.equals(idSede, sede.idSede);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSede);
    }
}