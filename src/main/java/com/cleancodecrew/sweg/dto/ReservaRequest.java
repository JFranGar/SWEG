package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaRequest {
	@NotNull(message = "Seleccione una sala")
	private Long salaId;

	@NotNull(message = "La fecha es obligatoria")
	private LocalDate fecha;

	@NotNull(message = "La hora de inicio es obligatoria")
	private LocalTime horaInicio;

	@NotNull(message = "La hora de fin es obligatoria")
	private LocalTime horaFin;

	@NotNull(message = "Indique la cantidad de personas")
	@Min(value = 1, message = "Debe ser al menos 1 persona")
	private Integer cantidadPersonas;
}
