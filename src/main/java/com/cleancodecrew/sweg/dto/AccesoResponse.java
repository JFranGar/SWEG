package com.cleancodecrew.sweg.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Evento de acceso (check-in o check-out) para el historial consultable por el administrador.
 * Se deriva de las marcas fechaIngreso/fechaSalida de cada reserva.
 */
public record AccesoResponse(
        Long reservaId,
        String tipo,              // CHECK_IN | CHECK_OUT
        LocalDateTime fecha,      // momento real del evento
        String salaNombre,
        String clienteNombre,
        String clienteCorreo,
        String operador,          // recepcionista/admin que registró la acción
        LocalTime horaInicio,
        LocalTime horaFin,
        String estadoReserva
) {}
