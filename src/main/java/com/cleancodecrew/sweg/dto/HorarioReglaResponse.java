package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.HorarioRegla;
import java.time.LocalTime;

public record HorarioReglaResponse(
        Long id,
        String tipo,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer diaSemana,
        String descripcion,
        boolean activo) {

    public static HorarioReglaResponse de(HorarioRegla r) {
        return new HorarioReglaResponse(
                r.getId(), r.getTipo().name(),
                r.getHoraInicio(), r.getHoraFin(),
                r.getDiaSemana(), r.getDescripcion(), r.isActivo());
    }
}
