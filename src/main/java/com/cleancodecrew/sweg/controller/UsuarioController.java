package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.PasswordHasher;
import com.cleancodecrew.sweg.dto.UsuarioRequest;
import com.cleancodecrew.sweg.dto.UsuarioResponse;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordHasher passwordHasher;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             ReservaRepository reservaRepository,
                             PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.passwordHasher = passwordHasher;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listAll() {
        return ResponseEntity.ok(usuarioRepository.findAll().stream()
                .map(UsuarioResponse::de).toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UsuarioRequest req) {
        if (req.getContrasena() == null || req.getContrasena().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña es obligatoria al crear un usuario"));
        }
        if (usuarioRepository.findByCorreoIgnoreCase(req.getCorreo()).isPresent()) {
            throw new DuplicadoException("Ya existe un usuario con ese correo");
        }
        Usuario u = Usuario.builder()
                .nombre(req.getNombre().trim())
                .correo(req.getCorreo().trim().toLowerCase())
                .contrasenaHash(passwordHasher.hash(req.getContrasena()))
                .rol(req.getRol())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuarioRepository.save(u)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        var maybe = usuarioRepository.findById(id);
        if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Usuario u = maybe.get();
        var byCorreo = usuarioRepository.findByCorreoIgnoreCase(req.getCorreo());
        if (byCorreo.isPresent() && !byCorreo.get().getId().equals(id)) {
            throw new DuplicadoException("Ya existe otro usuario con ese correo");
        }
        u.setNombre(req.getNombre().trim());
        u.setCorreo(req.getCorreo().trim().toLowerCase());
        u.setRol(req.getRol());
        if (req.getContrasena() != null && !req.getContrasena().isBlank()) {
            u.setContrasenaHash(passwordHasher.hash(req.getContrasena()));
        }
        return ResponseEntity.ok(UsuarioResponse.de(usuarioRepository.save(u)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (usuarioRepository.findById(id).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (reservaRepository.existsByCliente_Id(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "No se puede eliminar un usuario con reservas registradas"));
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
