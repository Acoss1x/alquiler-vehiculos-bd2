package co.edu.unbosque.alquiler.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "TECNICO")
public class Tecnico extends Empleado {

    @Column(name = "especialidad", nullable = false, length = 50)
    private String especialidad;

    public Tecnico() {}

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
}
