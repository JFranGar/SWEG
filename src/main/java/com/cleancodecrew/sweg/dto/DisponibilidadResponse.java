package com.cleancodecrew.sweg.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DisponibilidadResponse(
    Long salaId,
    String nombreSala,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    boolean disponible,
    String mensaje
) {}
