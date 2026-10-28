package co.edu.unbosque.alquiler.dto;

import java.math.BigDecimal;

public record CategoriaResponse(
        Long idCategoria,
        String nombreCategoria,
        Integer capacidadPasajeros,
        BigDecimal tarifaDiaria,
        String tipoTransmision,
        String estado
) {}
