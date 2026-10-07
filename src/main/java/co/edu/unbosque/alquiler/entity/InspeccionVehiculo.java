package co.edu.unbosque.alquiler.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa el historial de inspecciones de un vehículo.
 * Se almacena en MongoDB debido a la naturaleza flexible y no estructurada
 * de las observaciones mecánicas y la lista variable de piezas cambiadas.
 */
@Document(collection = "inspecciones")
public class InspeccionVehiculo {

    /**
     * Identificador alfanumérico generado automáticamente por MongoDB.
     * Nota: Se importa de org.springframework.data.annotation.Id, no de JPA.
     */
    @Id
    private String idInspeccion;

    /**
     * Referencia lógica al vehículo en PostgreSQL.
     * No es una llave foránea estricta, solo un valor de enlace.
     */
    private Long idVehiculo;

    private String nombreMecanico;

    private String observacionesGenerales;

    /**
     * Almacenar listas directamente es una ventaja clave de NoSQL.
     */
    private List<String> piezasCambiadas;

    private LocalDateTime fechaInspeccion = LocalDateTime.now();

    public InspeccionVehiculo() {
    }


    public String getIdInspeccion() {
        return idInspeccion;
    }

    public void setIdInspeccion(String idInspeccion) {
        this.idInspeccion = idInspeccion;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getNombreMecanico() {
        return nombreMecanico;
    }

    public void setNombreMecanico(String nombreMecanico) {
        this.nombreMecanico = nombreMecanico;
    }

    public String getObservacionesGenerales() {
        return observacionesGenerales;
    }

    public void setObservacionesGenerales(String observacionesGenerales) {
        this.observacionesGenerales = observacionesGenerales;
    }

    public List<String> getPiezasCambiadas() {
        return piezasCambiadas;
    }

    public void setPiezasCambiadas(List<String> piezasCambiadas) {
        this.piezasCambiadas = piezasCambiadas;
    }

    public LocalDateTime getFechaInspeccion() {
        return fechaInspeccion;
    }

    public void setFechaInspeccion(LocalDateTime fechaInspeccion) {
        this.fechaInspeccion = fechaInspeccion;
    }
}
