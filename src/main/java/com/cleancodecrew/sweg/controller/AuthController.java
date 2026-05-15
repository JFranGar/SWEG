package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.dto.LoginRequest;
import com.cleancodecrew.sweg.dto.SessionResponse;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import com.cleancodecrew.sweg.config.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UsuarioRepository usuarioRepository;
	private final PasswordHasher passwordHasher;

	public AuthController(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
		this.usuarioRepository = usuarioRepository;
		this.passwordHasher = passwordHasher;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
		var maybe = usuarioRepository.findByCorreoIgnoreCase(req.getCorreo());
		if (maybe.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales no validas"));
		}
		Usuario u = maybe.get();

		if (u.estaBloqueado()) {
			long minutos = u.minutosRestantesDeBloqueo();
			String msg = "Cuenta bloqueada. Intentelo en " + minutos + " minutos.";
			return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of("error", msg));
		}

		if (!passwordHasher.matches(req.getContrasena(), u.getContrasenaHash())) {
			u.registrarIntentoFallido();
			usuarioRepository.save(u);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales no validas"));
		}

		u.registrarLoginExitoso();
		usuarioRepository.save(u);

		HttpSession session = request.getSession(true);
		session.setAttribute(AuthInterceptor.SESSION_USER_ID, u.getId());
		session.setAttribute(AuthInterceptor.SESSION_ROL, u.getRol().name());
		session.setAttribute(AuthInterceptor.SESSION_NOMBRE, u.getNombre());
		session.setAttribute(AuthInterceptor.SESSION_CORREO, u.getCorreo());

		SessionResponse resp = new SessionResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name());
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) session.invalidate();
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<?> me(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Long id = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);
		var maybe = usuarioRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Usuario u = maybe.get();
		SessionResponse resp = new SessionResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name());
		return ResponseEntity.ok(resp);
	}
}
