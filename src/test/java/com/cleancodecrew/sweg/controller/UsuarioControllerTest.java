package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.PasswordHasher;
import com.cleancodecrew.sweg.dto.UsuarioRequest;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
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
 * Pruebas del UsuarioController (gestión de usuarios por ADMIN).
 */
class UsuarioControllerTest {

    private UsuarioRepository usuarioRepository;
    private ReservaRepository reservaRepository;
    private PasswordHasher passwordHasher;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        controller = new UsuarioController(usuarioRepository, reservaRepository, passwordHasher);
    }

    private Usuario usuario(long id, String correo) {
        return Usuario.builder().id(id).nombre("Nombre").correo(correo)
                .contrasenaHash("s:d").rol(Rol.CLIENTE).build();
    }

    private UsuarioRequest req(String nombre, String correo, String pass, Rol rol) {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombre(nombre);
        r.setCorreo(correo);
        r.setContrasena(pass);
        r.setRol(rol);
        return r;
    }

    @Test
    @DisplayName("listAll responde 200")
    void listAll() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario(1, "a@a.ec")));
        assertEquals(HttpStatus.OK, controller.listAll().getStatusCode());
    }

    @Test
    @DisplayName("create sin contraseña responde 400")
    void create_sinContrasena() {
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.create(req("N", "a@a.ec", "  ", Rol.CLIENTE)).getStatusCode());
    }

    @Test
    @DisplayName("create con correo duplicado lanza DuplicadoException")
    void create_duplicado() {
        when(usuarioRepository.findByCorreoIgnoreCase("a@a.ec")).thenReturn(Optional.of(usuario(1, "a@a.ec")));
        assertThrows(DuplicadoException.class, () -> controller.create(req("N", "a@a.ec", "clave", Rol.CLIENTE)));
    }

    @Test
    @DisplayName("create válido hashea la contraseña y responde 201")
    void create_ok() {
        when(usuarioRepository.findByCorreoIgnoreCase("a@a.ec")).thenReturn(Optional.empty());
        when(passwordHasher.hash("clave")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.create(req("N", "A@A.ec", "clave", Rol.RECEPCIONISTA));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(passwordHasher).hash("clave");
    }

    @Test
    @DisplayName("update inexistente responde 404")
    void update_notFound() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(9L, req("N", "a@a.ec", null, Rol.CLIENTE)).getStatusCode());
    }

    @Test
    @DisplayName("update con correo de otro usuario lanza DuplicadoException")
    void update_correoDeOtro() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "a@a.ec")));
        when(usuarioRepository.findByCorreoIgnoreCase("b@b.ec")).thenReturn(Optional.of(usuario(2, "b@b.ec")));
        assertThrows(DuplicadoException.class,
                () -> controller.update(1L, req("N", "b@b.ec", null, Rol.CLIENTE)));
    }

    @Test
    @DisplayName("update válido sin contraseña conserva el hash y responde 200")
    void update_ok() {
        Usuario u = usuario(1, "a@a.ec");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByCorreoIgnoreCase("a@a.ec")).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.update(1L, req("Nuevo", "a@a.ec", null, Rol.ADMIN));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Nuevo", u.getNombre());
        assertEquals(Rol.ADMIN, u.getRol());
        assertEquals("s:d", u.getContrasenaHash(), "Sin nueva contraseña se conserva el hash previo");
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    @DisplayName("delete inexistente responde 404")
    void delete_notFound() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(9L).getStatusCode());
    }

    @Test
    @DisplayName("delete de usuario con reservas responde 409")
    void delete_conReservas() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "a@a.ec")));
        when(reservaRepository.existsByClienteId(1L)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, controller.delete(1L).getStatusCode());
    }

    @Test
    @DisplayName("delete válido responde 204")
    void delete_ok() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "a@a.ec")));
        when(reservaRepository.existsByClienteId(1L)).thenReturn(false);

        ResponseEntity<Object> resp = controller.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(usuarioRepository).deleteById(1L);
    }
}
