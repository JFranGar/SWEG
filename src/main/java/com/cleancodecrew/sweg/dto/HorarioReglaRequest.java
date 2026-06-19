package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.TipoRegla;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioReglaRequest {
    @NotNull
    private TipoRegla tipo;
    @NotNull
    private LocalTime horaInicio;
    @NotNull
    private LocalTime horaFin;
    private Integer diaSemana;
    private String descripcion;
    private boolean activo = true;
}
