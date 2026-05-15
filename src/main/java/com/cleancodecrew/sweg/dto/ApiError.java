package com.cleancodecrew.sweg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Respuesta de error uniforme. ISO 25010 - Usabilidad (mensajes consistentes).
 */
@Data
@AllArgsConstructor
public class ApiError {
	private int status;
	private String error;
	private String path;
	private LocalDateTime timestamp;

	public static ApiError of(int status, String error, String path) {
		return new ApiError(status, error, path, LocalDateTime.now());
	}
}

