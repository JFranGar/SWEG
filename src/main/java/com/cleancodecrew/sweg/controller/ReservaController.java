package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ReservaRequest;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import com.cleancodecrew.sweg.config.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/cliente/reservas")
public class ReservaController {

	private final SalaRepository salaRepository;
	private final ReservaRepository reservaRepository;
	private final UsuarioRepository usuarioRepository;

	public ReservaController(SalaRepository salaRepository, ReservaRepository reservaRepository, UsuarioRepository usuarioRepository) {
		this.salaRepository = salaRepository;
		this.reservaRepository = reservaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@GetMapping("/salas-disponibles")
	public ResponseEntity<List<Sala>> salasDisponibles() {
		var list = salaRepository.findAll().stream().filter(s -> s.getEstado() == null || s.getEstado().name().equals("DISPONIBLE") || s.getEstado() == com.cleancodecrew.sweg.model.EstadoSala.DISPONIBLE).toList();
		return ResponseEntity.ok(list);
	}

	@GetMapping
	public ResponseEntity<?> misReservas(HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long clienteId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);
		var list = reservaRepository.findByCliente_IdOrderByFechaDescHoraInicioDesc(clienteId);
		return ResponseEntity.ok(list);
	}

	@PostMapping
	public ResponseEntity<?> crear(@Valid @RequestBody ReservaRequest req, HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long clienteId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);

		var maybeSala = salaRepository.findById(req.getSalaId());
		if (maybeSala.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Sala no encontrada"));
		var maybeCliente = usuarioRepository.findById(clienteId);
		if (maybeCliente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Cliente no encontrado"));

		Sala sala = maybeSala.get();
		Usuario cliente = maybeCliente.get();

		Reserva reserva = Reserva.builder()
				.sala(sala)
				.cliente(cliente)
				.fecha(req.getFecha())
				.horaInicio(req.getHoraInicio())
				.horaFin(req.getHoraFin())
				.build();

		reserva.validarTodo();

		var candidatas = reservaRepository.findBySala_IdAndFechaAndEstadoNot(req.getSalaId(), req.getFecha(), EstadoReserva.CANCELADA);
		for (Reserva r : candidatas) {
			if (reserva.seSolapaCon(r)) {
				throw new ConflictoException("La sala ya esta reservada en ese horario");
			}
		}

		Reserva saved = reservaRepository.save(reserva);
		ReservaResponse.SalaMini sm = new ReservaResponse.SalaMini(sala.getId(), sala.getNombre(), sala.getTipo().name(), sala.getCapacidadMaxima());
		ReservaResponse resp = new ReservaResponse(saved.getId(), "Reserva confirmada", sm, saved.getFecha(), saved.getHoraInicio(), saved.getHoraFin(), saved.getEstado().name());
		return ResponseEntity.status(HttpStatus.CREATED).body(resp);
	}
}
