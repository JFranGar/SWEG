package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.config.PasswordHasher;
import com.cleancodecrew.sweg.dto.LoginRequest;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del AuthController (HU1 - login/logout/me). Repositorio, hasher, request
 * y sesión mockeados; sin contexto Spring ni base de datos.
 */
class AuthControllerTest {

    private UsuarioRepository usuarioRepository;
    private PasswordHasher passwordHasher;
    private AuthController controller;
    private HttpServletRequest request;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        controller = new AuthController(usuarioRepository, passwordHasher);
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
    }

    private LoginRequest login(String correo, String pass, Rol rol) {
        LoginRequest r = new LoginRequest();
        r.setCorreo(correo);
        r.setContrasena(pass);
        r.setRolSeleccionado(rol);
        return r;
    }

    private Usuario usuario(Rol rol) {
        return Usuario.builder().id(1L).nombre("Demo").correo("demo@swgec.ec")
                .contrasenaHash("hash").rol(rol).build();
    }

    @Test
    @DisplayName("Login con correo inexistente responde 401")
    void login_correoInexistente() {
        when(usuarioRepository.findByCorreoIgnoreCase("x@x.ec")).thenReturn(Optional.empty());
        ResponseEntity<Object> resp = controller.login(login("x@x.ec", "p", Rol.CLIENTE), request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    @DisplayName("Login con cuenta bloqueada responde 423 LOCKED")
    void login_bloqueado() {
        Usuario u = usuario(Rol.CLIENTE);
        u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByCorreoIgnoreCase("demo@swgec.ec")).thenReturn(Optional.of(u));

        ResponseEntity<Object> resp = controller.login(login("demo@swgec.ec", "p", Rol.CLIENTE), request);

        assertEquals(HttpStatus.LOCKED, resp.getStatusCode());
    }

    @Test
    @DisplayName("Contraseña incorrecta registra intento fallido y responde 401")
    void login_passwordIncorrecta() {
        Usuario u = usuario(Rol.CLIENTE);
        when(usuarioRepository.findByCorreoIgnoreCase("demo@swgec.ec")).thenReturn(Optional.of(u));
        when(passwordHasher.matches("mala", "hash")).thenReturn(false);

        ResponseEntity<Object> resp = controller.login(login("demo@swgec.ec", "mala", Rol.CLIENTE), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals(1, u.getIntentosFallidos());
        verify(usuarioRepository).save(u);
    }

    @Test
    @DisplayName("Rol seleccionado que no coincide responde 403")
    void login_rolNoCoincide() {
        Usuario u = usuario(Rol.CLIENTE);
        when(usuarioRepository.findByCorreoIgnoreCase("demo@swgec.ec")).thenReturn(Optional.of(u));
        when(passwordHasher.matches("demo", "hash")).thenReturn(true);

        ResponseEntity<Object> resp = controller.login(login("demo@swgec.ec", "demo", Rol.ADMIN), request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    @DisplayName("Login correcto crea sesión y responde 200 con datos del usuario")
    void login_exitoso() {
        Usuario u = usuario(Rol.ADMIN);
        when(usuarioRepository.findByCorreoIgnoreCase("demo@swgec.ec")).thenReturn(Optional.of(u));
        when(passwordHasher.matches("demo", "hash")).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);

        ResponseEntity<Object> resp = controller.login(login("demo@swgec.ec", "demo", Rol.ADMIN), request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(session).setAttribute(AuthInterceptor.SESSION_USER_ID, 1L);
        verify(session).setAttribute(AuthInterceptor.SESSION_ROL, "ADMIN");
        verify(usuarioRepository).save(u);
        assertNotNull(u.getUltimoLogin(), "El login exitoso registra la marca de tiempo");
    }

    @Test
    @DisplayName("Logout invalida la sesión existente y responde 204")
    void logout_conSesion() {
        when(request.getSession(false)).thenReturn(session);
        ResponseEntity<Void> resp = controller.logout(request);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(session).invalidate();
    }

    @Test
    @DisplayName("Logout sin sesión responde 204 sin fallar")
    void logout_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.NO_CONTENT, controller.logout(request).getStatusCode());
    }

    @Test
    @DisplayName("/me sin sesión responde 401")
    void me_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.me(request).getStatusCode());
    }

    @Test
    @DisplayName("/me con sesión válida devuelve los datos del usuario")
    void me_conSesion() {
        Usuario u = usuario(Rol.CLIENTE);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        ResponseEntity<Object> resp = controller.me(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("/me con usuario inexistente responde 401")
    void me_usuarioInexistente() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.UNAUTHORIZED, controller.me(request).getStatusCode());
    }
}
