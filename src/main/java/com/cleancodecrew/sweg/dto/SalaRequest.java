package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.TipoSala;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de entrada para HU2 Gestion de Salas.
 * HU2 CA4: campos obligatorios via Bean Validation.
 */
@Data
public class SalaRequest {
	@NotBlank(message = "El nombre es obligatorio")
	private String nombre;

	@NotNull(message = "El tipo es obligatorio")
	private TipoSala tipo;

	@Min(value = 1, message = "La capacidad debe ser mayor a 0")
	private int capacidadMaxima;
}
