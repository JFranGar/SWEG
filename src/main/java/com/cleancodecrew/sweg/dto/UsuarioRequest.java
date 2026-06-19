package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsuarioRequest {
    @NotBlank(message = "Nombre obligatorio")
    private String nombre;

    @NotBlank(message = "Correo obligatorio")
    @Email(message = "Correo inválido")
    private String correo;

    private String contrasena;

    @NotNull(message = "Rol obligatorio")
    private Rol rol;
}
