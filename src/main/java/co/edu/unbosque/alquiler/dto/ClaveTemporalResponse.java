package co.edu.unbosque.alquiler.dto;

/**
 * Clave temporal generada al restablecer. Se muestra una sola vez: el sistema
 * solo guarda su hash.
 */
public record ClaveTemporalResponse(String claveTemporal) {
}