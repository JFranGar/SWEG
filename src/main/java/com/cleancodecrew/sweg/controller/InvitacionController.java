package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.dto.ApiError;
import com.cleancodecrew.sweg.dto.InvitacionRequest;
import com.cleancodecrew.sweg.dto.InvitacionResponse;
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
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * InvitacionController — Emisión y gestión de invitaciones internas (HU RBAC Híbrido).
 *
 * Rutas bajo /api/admin/invitaciones (protegidas por AuthInterceptor: solo ADMIN).
 * Un administrador activo puede crear, listar y revocar invitaciones de cuentas
 * RECEPCIONISTA y ADMIN. El backend valida SIEMPRE el permiso y el rol asignado.
 */
@RestController
@RequestMapping("/api/admin/invitaciones")
public class InvitacionController {

    private static final Logger log = LoggerFactory.getLogger(InvitacionController.class);

    private final InvitacionRepository invitacionRepository;
    private final UsuarioRepository usuarioRepository;

    public InvitacionController(InvitacionRepository invitacionRepository,
                                UsuarioRepository usuarioRepository) {
        this.invitacionRepository = invitacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario adminActual(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object idObj = session.getAttribute(AuthInterceptor.SESSION_USER_ID);
        if (idObj == null) return null;
        return usuarioRepository.findById((Long) idObj).orElse(null);
    }

    /** CA-HU12-02: listado de invitaciones para el panel, más recientes primero. */
    @GetMapping
    public ResponseEntity<List<InvitacionResponse>> listar() {
        return ResponseEntity.ok(invitacionRepository.findAllByOrderByCreadaEnDesc()
                .stream().map(InvitacionResponse::de).toList());
    }

    /**
     * CA-HU12-01: Un administrador activo emite una invitación segura (token + expiración).
     * Solo roles internos (RECEPCIONISTA/ADMIN) y correo obligatorio del invitado.
     */
    @Transactional
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody InvitacionRequest req, HttpServletRequest http) {
        Usuario admin = adminActual(http);
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of(403, "Solo un administrador activo puede emitir invitaciones", http.getRequestURI()));
        }
        if (req.getRol() == Rol.CLIENTE) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(400, "Las cuentas CLIENTE se crean por auto-registro, no por invitación",
                            http.getRequestURI(), Map.of("rol", "Seleccione RECEPCIONISTA o ADMIN")));
        }
        if (req.getCorreo() == null || req.getCorreo().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(400, "El correo del invitado es obligatorio",
                            http.getRequestURI(), Map.of("correo", "El correo del invitado es obligatorio")));
        }
        String correo = req.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, "Ya existe una cuenta con ese correo", http.getRequestURI()));
        }

        int horas = req.getHorasValidez() != null ? req.getHorasValidez() : Invitacion.HORAS_VALIDEZ_DEFECTO;
        Invitacion inv = Invitacion.crear(req.getRol(), correo, admin, horas);
        invitacionRepository.save(inv);
        log.info("Invitación creada: rol={} correo={} por admin='{}' expira={}",
                inv.getRol(), correo, admin.getCorreo(), inv.getExpiraEn());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitacionResponse.de(inv));
    }

    /** Revoca una invitación pendiente para que no pueda utilizarse. */
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> revocar(@PathVariable Long id, HttpServletRequest http) {
        Invitacion inv = invitacionRepository.findById(id).orElse(null);
        if (inv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Invitación no encontrada", http.getRequestURI()));
        }
        inv.revocar();  // lanza IllegalStateException (409) si ya fue usada
        invitacionRepository.save(inv);
        return ResponseEntity.ok(Map.of("mensaje", "Invitación revocada", "id", id));
    }
}
