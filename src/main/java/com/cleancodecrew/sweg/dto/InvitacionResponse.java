package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.Invitacion;

import java.time.LocalDateTime;

/**
 * Respuesta con los datos de una invitación para el panel de administración.
 * Incluye el estado legible y la ruta relativa de registro para compartir.
 */
public record InvitacionResponse(
        Long id,
        String token,
        String rol,
        String correo,
        String estado,
        String creadaPor,
        LocalDateTime creadaEn,
        LocalDateTime expiraEn,
        boolean usada,
        LocalDateTime usadaEn,
        String usuarioCreado,
        String rutaRegistro
) {
    public static InvitacionResponse de(Invitacion inv) {
        LocalDateTime ahora = LocalDateTime.now();
        return new InvitacionResponse(
                inv.getId(),
                inv.getToken(),
                inv.getRol() != null ? inv.getRol().name() : null,
                inv.getCorreo(),
                inv.estadoLegible(ahora),
                inv.getCreadaPor() != null ? inv.getCreadaPor().getNombre() : null,
                inv.getCreadaEn(),
                inv.getExpiraEn(),
                inv.isUsada(),
                inv.getUsadaEn(),
                inv.getUsuarioCreado() != null ? inv.getUsuarioCreado().getNombre() : null,
                "/html/registro-invitacion.html?token=" + inv.getToken()
        );
    }
}
