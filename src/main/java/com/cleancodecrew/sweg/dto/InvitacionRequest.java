package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de entrada para que un administrador emita una invitación de cuenta interna.
 * Solo admite roles RECEPCIONISTA o ADMIN (validado en dominio y controller).
 */
@Data
public class InvitacionRequest {

    @NotNull(message = "Debe seleccionar el rol de la invitación")
    private Rol rol;

    /** Correo sugerido del invitado (opcional, informativo). */
    @Email(message = "Formato de correo inválido")
    private String correo;

    /** Horas de validez de la invitación; si es nulo o <= 0 se usa el valor por defecto. */
    private Integer horasValidez;
}
