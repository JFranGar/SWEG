package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada para HU4 Reserva de Sala.
 * HU4 CA4: campos obligatorios via Bean Validation.
 */
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
}
