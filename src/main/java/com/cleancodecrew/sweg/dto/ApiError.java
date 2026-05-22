package com.cleancodecrew.sweg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Respuesta de error uniforme.
 * ISO 25010 - Usabilidad / Prevención de errores.
 * Soporta errores granulares por campo (CA-HU02-04, CA-HU04-04).
 */
@Data
@AllArgsConstructor
public class ApiError {
	private int status;
	private String error;
	private String path;
	private LocalDateTime timestamp;
	private Map<String, String> fields;   // NUEVO

	public static ApiError of(int status, String error, String path) {
		return new ApiError(status, error, path, LocalDateTime.now(), Collections.emptyMap());
	}

	public static ApiError of(int status, String error, String path, Map<String, String> fields) {
		return new ApiError(status, error, path, LocalDateTime.now(), fields);
	}
}

