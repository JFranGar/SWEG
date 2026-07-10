package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.PasswordHasher;
import com.cleancodecrew.sweg.dto.ApiError;
import com.cleancodecrew.sweg.dto.CompletarRegistroRequest;
import com.cleancodecrew.sweg.dto.RegistroClienteRequest;
import com.cleancodecrew.sweg.model.Invitacion;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.InvitacionRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * RegistroController — Modelo híbrido RBAC (HU Registro de cuentas).
 *
 * Rutas bajo /api/auth/** (públicas: excluidas del AuthInterceptor):
 *  - Auto-registro público de CLIENTE.
 *  - Validación y consumo de invitaciones para cuentas internas (RECEPCIONISTA/ADMIN).
 *
 * Regla de seguridad clave: el rol NUNCA se toma del cliente.
 *  - En el auto-registro el backend fuerza CLIENTE.
 *  - Al completar una invitación, el rol proviene de la invitación validada en servidor.
 */
@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);

    private final UsuarioRepository usuarioRepository;
    private final InvitacionRepository invitacionRepository;
    private final PasswordHasher passwordHasher;

    public RegistroController(UsuarioRepository usuarioRepository,
                              InvitacionRepository invitacionRepository,
                              PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.invitacionRepository = invitacionRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * CA-HU12-03: Auto-registro público de clientes.
     * El backend fuerza el rol CLIENTE; ignora cualquier intento de escalar privilegios.
     */
    @PostMapping("/registro")
    public ResponseEntity<Object> registrarCliente(@Valid @RequestBody RegistroClienteRequest req,
                                              HttpServletRequest http) {
        String correo = req.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, "Ya existe una cuenta con ese correo", http.getRequestURI()));
        }
        // Nombre y apellido llegan separados y validados; se almacenan como nombre completo.
        String nombreCompleto = (req.getNombre().trim() + " " + req.getApellido().trim()).trim();
        Usuario nuevo = Usuario.builder()
                .nombre(nombreCompleto)
                .correo(correo)
                .contrasenaHash(passwordHasher.hash(req.getContrasena()))
                .rol(Rol.CLIENTE)  // CA-HU12-03: rol forzado en backend, jamás desde el frontend
                .build();
        usuarioRepository.save(nuevo);
        log.info("Registro: nueva cuenta CLIENTE '{}'", correo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cuenta creada correctamente", "correo", correo));
    }

    /**
     * CA-HU12-01: Consulta pública de una invitación por token.
     * Devuelve el rol y correo sugerido solo si la invitación sigue siendo válida.
     */
    @GetMapping("/invitacion/{token}")
    public ResponseEntity<Object> verInvitacion(@PathVariable String token, HttpServletRequest http) {
        Invitacion inv = invitacionRepository.findByToken(token).orElse(null);
        if (inv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Invitación no encontrada", http.getRequestURI()));
        }
        if (!inv.esValida(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiError.of(410, "La invitación ya no es válida (usada, revocada o expirada)", http.getRequestURI()));
        }
        return ResponseEntity.ok(Map.of(
                "rol", inv.getRol().name(),
                "correo", inv.getCorreo() != null ? inv.getCorreo() : "",
                "expiraEn", inv.getExpiraEn()));
    }

    /**
     * CA-HU12-01, CA-HU12-02: Completa el registro de una cuenta interna mediante invitación.
     * El rol se toma de la invitación (no del cliente); la invitación se marca como usada (un solo uso).
     */
    @Transactional
    @PostMapping("/invitacion/{token}/completar")
    public ResponseEntity<Object> completarInvitacion(@PathVariable String token,
                                                @Valid @RequestBody CompletarRegistroRequest req,
                                                HttpServletRequest http) {
        Invitacion inv = invitacionRepository.findByToken(token).orElse(null);
        if (inv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Invitación no encontrada", http.getRequestURI()));
        }
        if (!inv.esValida(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiError.of(410, "La invitación ya no es válida (usada, revocada o expirada)", http.getRequestURI()));
        }

        // El correo definitivo: el de la invitación si existe, o el nombre no basta -> exigimos correo de invitación.
        String correo = inv.getCorreo();
        if (correo == null || correo.isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, "La invitación no tiene un correo asociado; solicite una nueva", http.getRequestURI()));
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, "Ya existe una cuenta con el correo de la invitación", http.getRequestURI()));
        }

        Usuario nuevo = Usuario.builder()
                .nombre(req.getNombre().trim())
                .correo(correo)
                .contrasenaHash(passwordHasher.hash(req.getContrasena()))
                .rol(inv.getRol())  // CA-HU12-01: rol tomado SIEMPRE de la invitación validada en backend
                .build();
        usuarioRepository.save(nuevo);

        inv.marcarUsada(nuevo, LocalDateTime.now());  // CA-HU12-02: un solo uso + trazabilidad
        invitacionRepository.save(inv);

        log.info("Registro: cuenta interna '{}' rol={} creada vía invitación de '{}'",
                correo, inv.getRol(), inv.getCreadaPor() != null ? inv.getCreadaPor().getCorreo() : "?");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cuenta creada correctamente", "correo", correo, "rol", inv.getRol().name()));
    }
}
