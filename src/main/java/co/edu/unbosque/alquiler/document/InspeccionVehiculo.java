package co.edu.unbosque.alquiler.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de inspecciones mecánicas de un vehículo.
 * Se almacena en MongoDB por la naturaleza flexible de las observaciones y la
 * lista variable de piezas cambiadas (subdocumentos).
 * <p>
 * Nota: {@code @Id} es de Spring Data, no de JPA.
 * </p>
 */
@Document(collection = "inspecciones")
public class InspeccionVehiculo {

    @Id
    private String idInspeccion;

    /**
     * Referencia lógica al vehículo en PostgreSQL (no hay llave foránea).
     */
    private Long idVehiculo;

    /**
     * Referencia lógica al técnico (empleado) en PostgreSQL.
     */
    private Long idTecnico;

    private String observacionesGenerales;

    private List<PiezaCambiada> piezasCambiadas = new ArrayList<>();

    private LocalDateTime fechaInspeccion = LocalDateTime.now();

    public InspeccionVehiculo() {
    }

    public String getIdInspeccion() { return idInspeccion; }
    public void setIdInspeccion(String idInspeccion) { this.idInspeccion = idInspeccion; }

    public Long getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(Long idVehiculo) { this.idVehiculo = idVehiculo; }

    public Long getIdTecnico() { return idTecnico; }
    public void setIdTecnico(Long idTecnico) { this.idTecnico = idTecnico; }

    public String getObservacionesGenerales() { return observacionesGenerales; }
    public void setObservacionesGenerales(String observacionesGenerales) { this.observacionesGenerales = observacionesGenerales; }

    public List<PiezaCambiada> getPiezasCambiadas() { return piezasCambiadas; }
    public void setPiezasCambiadas(List<PiezaCambiada> piezasCambiadas) { this.piezasCambiadas = piezasCambiadas; }

    public LocalDateTime getFechaInspeccion() { return fechaInspeccion; }
    public void setFechaInspeccion(LocalDateTime fechaInspeccion) { this.fechaInspeccion = fechaInspeccion; }
}

