package co.edu.unbosque.alquiler.document;

/**
 * Subdocumento embebido en una inspección: un daño encontrado en el vehículo.
 */
public class DanoVehiculo {

    private String zona;
    private String descripcion;
    private SeveridadDano severidad;

    public DanoVehiculo() {
    }

    public DanoVehiculo(String zona, String descripcion, SeveridadDano severidad) {
        this.zona = zona;
        this.descripcion = descripcion;
        this.severidad = severidad;
    }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public SeveridadDano getSeveridad() { return severidad; }
    public void setSeveridad(SeveridadDano severidad) { this.severidad = severidad; }
}
