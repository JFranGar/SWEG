package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.PasswordHasher;
import com.cleancodecrew.sweg.dto.CompletarRegistroRequest;
import com.cleancodecrew.sweg.dto.RegistroClienteRequest;
import com.cleancodecrew.sweg.model.Invitacion;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.InvitacionRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas del RegistroController (HU12 - auto-registro y registro por invitación).
 * Se verifica en especial que el rol nunca provenga del cliente.
 */
class RegistroControllerTest {

    private UsuarioRepository usuarioRepository;
    private InvitacionRepository invitacionRepository;
    private PasswordHasher passwordHasher;
    private RegistroController controller;
    private HttpServletRequest http;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        invitacionRepository = mock(InvitacionRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        controller = new RegistroController(usuarioRepository, invitacionRepository, passwordHasher);
        http = mock(HttpServletRequest.class);
    }

    private Usuario admin() {
        return Usuario.builder().id(1L).nombre("Admin").correo("admin@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.ADMIN).build();
    }

    private RegistroClienteRequest registro() {
        RegistroClienteRequest r = new RegistroClienteRequest();
        r.setNombre("Juan");
        r.setApellido("Perez");
        r.setCorreo("Juan@Correo.EC");
        r.setContrasena("Segura123");
        return r;
    }

    @Test
    @DisplayName("Auto-registro con correo existente responde 409")
    void registro_duplicado() {
        when(usuarioRepository.existsByCorreoIgnoreCase("juan@correo.ec")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, controller.registrarCliente(registro(), http).getStatusCode());
    }

    @Test
    @DisplayName("Auto-registro fuerza el rol CLIENTE y guarda nombre completo")
    void registro_ok() {
        when(usuarioRepository.existsByCorreoIgnoreCase("juan@correo.ec")).thenReturn(false);
        when(passwordHasher.hash("Segura123")).thenReturn("hash");

        ResponseEntity<Object> resp = controller.registrarCliente(registro(), http);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Rol.CLIENTE, captor.getValue().getRol(), "El rol SIEMPRE es CLIENTE");
        assertEquals("Juan Perez", captor.getValue().getNombre());
        assertEquals("juan@correo.ec", captor.getValue().getCorreo());
    }

    @Test
    @DisplayName("verInvitacion con token inexistente responde 404")
    void verInvitacion_notFound() {
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.verInvitacion("t", http).getStatusCode());
    }

    @Test
    @DisplayName("verInvitacion con invitación no válida responde 410")
    void verInvitacion_invalida() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "a@a.ec", admin(), 24);
        inv.revocar();
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.of(inv));
        assertEquals(HttpStatus.GONE, controller.verInvitacion("t", http).getStatusCode());
    }

    @Test
    @DisplayName("verInvitacion válida responde 200 con rol y correo")
    void verInvitacion_ok() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "invitado@swgec.ec", admin(), 24);
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.of(inv));
        assertEquals(HttpStatus.OK, controller.verInvitacion("t", http).getStatusCode());
    }

    @Test
    @DisplayName("completarInvitacion con token inexistente responde 404")
    void completar_notFound() {
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.empty());
        CompletarRegistroRequest req = new CompletarRegistroRequest();
        req.setNombre("Ana"); req.setContrasena("1234");
        assertEquals(HttpStatus.NOT_FOUND, controller.completarInvitacion("t", req, http).getStatusCode());
    }

    @Test
    @DisplayName("completarInvitacion con correo ya usado responde 409")
    void completar_correoExistente() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "invitado@swgec.ec", admin(), 24);
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.of(inv));
        when(usuarioRepository.existsByCorreoIgnoreCase("invitado@swgec.ec")).thenReturn(true);

        CompletarRegistroRequest req = new CompletarRegistroRequest();
        req.setNombre("Ana"); req.setContrasena("1234");

        assertEquals(HttpStatus.CONFLICT, controller.completarInvitacion("t", req, http).getStatusCode());
    }

    @Test
    @DisplayName("completarInvitacion válida crea la cuenta con el rol de la invitación y la consume")
    void completar_ok() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "invitado@swgec.ec", admin(), 24);
        when(invitacionRepository.findByToken("t")).thenReturn(Optional.of(inv));
        when(usuarioRepository.existsByCorreoIgnoreCase("invitado@swgec.ec")).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn("hash");

        CompletarRegistroRequest req = new CompletarRegistroRequest();
        req.setNombre("Ana"); req.setContrasena("1234");

        ResponseEntity<Object> resp = controller.completarInvitacion("t", req, http);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Rol.RECEPCIONISTA, captor.getValue().getRol(), "El rol proviene de la invitación");
        assertTrue(inv.isUsada(), "La invitación queda consumida (un solo uso)");
        verify(invitacionRepository).save(inv);
    }
}
