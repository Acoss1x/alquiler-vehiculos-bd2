package co.edu.unbosque.alquiler.document;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;


public class PiezaCambiada {

    private String nombre;

    private Integer cantidad;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal costo;

    public PiezaCambiada() {
    }

    public PiezaCambiada(String nombre, Integer cantidad, BigDecimal costo) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.costo = costo;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
}

