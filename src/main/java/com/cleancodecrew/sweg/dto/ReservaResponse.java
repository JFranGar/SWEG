package com.cleancodecrew.sweg.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ReservaResponse
 *
 * Respuesta simplificada de reserva.
 */
public record ReservaResponse(
        Long id,
        String mensaje,
        SalaMini sala,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado
) {
    public record SalaMini(Long id, String nombre, String tipo, int capacidadMaxima) {}
}
