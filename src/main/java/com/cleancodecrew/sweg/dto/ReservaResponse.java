package com.cleancodecrew.sweg.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.cleancodecrew.sweg.model.Reserva;

public record ReservaResponse(
        Long id,
        String mensaje,
        SalaMini sala,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaCancelacion,
        Integer cantidadPersonas
) {
    public record SalaMini(Long id, String nombre, String tipo, int capacidadMaxima) {}

    public static ReservaResponse de(Reserva r) {
        SalaMini sm = new SalaMini(
                r.getSala().getId(), r.getSala().getNombre(),
                r.getSala().getTipo().name(), r.getSala().getCapacidadMaxima());
        return new ReservaResponse(
                r.getId(), "", sm,
                r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
                r.getEstado().name(),
                r.getFechaIngreso(), r.getFechaCancelacion(),
                r.getCantidadPersonas());
    }
}
