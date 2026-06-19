package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.Usuario;
import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String nombre, String correo, String rol, LocalDateTime ultimoLogin) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(), u.getUltimoLogin());
    }
}
