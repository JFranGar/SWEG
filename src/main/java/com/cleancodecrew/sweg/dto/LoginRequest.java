package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de entrada para HU1 Inicio de Sesion.
 * HU1 CA4: validacion de campos obligatorios via Bean Validation.
 */
@Data
public class LoginRequest {
	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Formato de correo invalido")
	private String correo;

	@NotBlank(message = "La contrasena es obligatoria")
	private String contrasena;
}
