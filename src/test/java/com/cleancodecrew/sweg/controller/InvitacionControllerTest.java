package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.dto.InvitacionRequest;
import com.cleancodecrew.sweg.model.Invitacion;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.InvitacionRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas del InvitacionController (HU12 - emisión y gestión de invitaciones por ADMIN).
 */
class InvitacionControllerTest {

    private InvitacionRepository invitacionRepository;
    private UsuarioRepository usuarioRepository;
    private InvitacionController controller;
    private HttpServletRequest http;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        invitacionRepository = mock(InvitacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        controller = new InvitacionController(invitacionRepository, usuarioRepository);
        http = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
    }

    private void comoAdmin() {
        Usuario admin = Usuario.builder().id(1L).nombre("Admin").correo("admin@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.ADMIN).build();
        when(http.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
    }

    private InvitacionRequest req(Rol rol, String correo) {
        InvitacionRequest r = new InvitacionRequest();
        r.setRol(rol);
        r.setCorreo(correo);
        return r;
    }

    @Test
    @DisplayName("listar responde 200")
    void listar() {
        when(invitacionRepository.findAllByOrderByCreadaEnDesc()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.listar().getStatusCode());
    }

    @Test
    @DisplayName("crear sin sesión de admin responde 403")
    void crear_noAdmin() {
        when(http.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.FORBIDDEN,
                controller.crear(req(Rol.RECEPCIONISTA, "a@a.ec"), http).getStatusCode());
    }

    @Test
    @DisplayName("crear con rol CLIENTE responde 400")
    void crear_rolCliente() {
        comoAdmin();
        assertEquals(HttpStatus.BAD_REQUEST, controller.crear(req(Rol.CLIENTE, "a@a.ec"), http).getStatusCode());
    }

    @Test
    @DisplayName("crear sin correo responde 400")
    void crear_sinCorreo() {
        comoAdmin();
        assertEquals(HttpStatus.BAD_REQUEST, controller.crear(req(Rol.RECEPCIONISTA, null), http).getStatusCode());
    }

    @Test
    @DisplayName("crear con correo ya registrado responde 409")
    void crear_correoExistente() {
        comoAdmin();
        when(usuarioRepository.existsByCorreoIgnoreCase("a@a.ec")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, controller.crear(req(Rol.RECEPCIONISTA, "a@a.ec"), http).getStatusCode());
    }

    @Test
    @DisplayName("crear válido responde 201 y persiste la invitación")
    void crear_ok() {
        comoAdmin();
        when(usuarioRepository.existsByCorreoIgnoreCase("a@a.ec")).thenReturn(false);

        ResponseEntity<Object> resp = controller.crear(req(Rol.RECEPCIONISTA, "a@a.ec"), http);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(invitacionRepository).save(any(Invitacion.class));
    }

    @Test
    @DisplayName("revocar invitación inexistente responde 404")
    void revocar_notFound() {
        when(invitacionRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.revocar(1L, http).getStatusCode());
    }

    @Test
    @DisplayName("revocar invitación pendiente responde 200")
    void revocar_ok() {
        Usuario admin = Usuario.builder().id(1L).nombre("Admin").correo("admin@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.ADMIN).build();
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "a@a.ec", admin, 24);
        when(invitacionRepository.findById(1L)).thenReturn(Optional.of(inv));

        ResponseEntity<Object> resp = controller.revocar(1L, http);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(inv.isRevocada());
        verify(invitacionRepository).save(inv);
    }
}
