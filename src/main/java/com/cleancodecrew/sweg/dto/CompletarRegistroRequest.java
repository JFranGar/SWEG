package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para completar el registro de una cuenta interna mediante invitación.
 * El rol proviene EXCLUSIVAMENTE de la invitación (validada en backend),
 * nunca del formulario; así se impide la manipulación de roles desde el frontend.
 */
@Data
public class CompletarRegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
    private String contrasena;
}
