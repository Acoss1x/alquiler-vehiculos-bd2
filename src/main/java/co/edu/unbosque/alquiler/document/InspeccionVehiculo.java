package co.edu.unbosque.alquiler.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "inspecciones")
@CompoundIndexes({
        @CompoundIndex(name = "ix_vehiculo_fecha", def = "{'idVehiculo': 1, 'fechaInspeccion': -1}"),
        @CompoundIndex(name = "ux_reserva_tipo", def = "{'idReserva': 1, 'tipo': 1}", unique = true,
                partialFilter = "{ 'idReserva': { '$exists': true } }")
})
public class InspeccionVehiculo {

    @Id
    private String idInspeccion;

    private TipoInspeccion tipo;

    private Long idVehiculo;

    private Long idReserva;

    private Long idEmpleado;

    private NivelCombustible combustible;

    private Integer kilometraje;

    private String observacionesGenerales;

    private List<DanoVehiculo> danos = new ArrayList<>();

    private List<PiezaCambiada> piezasCambiadas = new ArrayList<>();

    private List<String> fotos = new ArrayList<>();

    private LocalDateTime fechaInspeccion = LocalDateTime.now();

    public InspeccionVehiculo() {
    }

    public String getIdInspeccion() { return idInspeccion; }
    public void setIdInspeccion(String idInspeccion) { this.idInspeccion = idInspeccion; }

    public TipoInspeccion getTipo() { return tipo; }
    public void setTipo(TipoInspeccion tipo) { this.tipo = tipo; }

    public Long getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(Long idVehiculo) { this.idVehiculo = idVehiculo; }

    public Long getIdReserva() { return idReserva; }
    public void setIdReserva(Long idReserva) { this.idReserva = idReserva; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public NivelCombustible getCombustible() { return combustible; }
    public void setCombustible(NivelCombustible combustible) { this.combustible = combustible; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public String getObservacionesGenerales() { return observacionesGenerales; }
    public void setObservacionesGenerales(String observacionesGenerales) { this.observacionesGenerales = observacionesGenerales; }

    public List<DanoVehiculo> getDanos() { return danos; }
    public void setDanos(List<DanoVehiculo> danos) { this.danos = danos; }

    public List<PiezaCambiada> getPiezasCambiadas() { return piezasCambiadas; }
    public void setPiezasCambiadas(List<PiezaCambiada> piezasCambiadas) { this.piezasCambiadas = piezasCambiadas; }

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }

    public LocalDateTime getFechaInspeccion() { return fechaInspeccion; }
    public void setFechaInspeccion(LocalDateTime fechaInspeccion) { this.fechaInspeccion = fechaInspeccion; }
}