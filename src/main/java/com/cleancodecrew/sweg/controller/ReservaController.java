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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import com.cleancodecrew.sweg.dto.DisponibilidadResponse;

@RestController
@RequestMapping({"/api/cliente/reservas", "/api/reservas"})
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

	// CA-HU04-01, CA-HU04-02, CA-HU04-03, CA-HU04-04, CA-HU04-05
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
		ReservaResponse resp = ReservaResponse.de(saved);
		return ResponseEntity.status(HttpStatus.CREATED).body(resp);
	}

	/** CA-HU05-01, CA-HU05-04, CA-HU05-05 */
	@GetMapping("/disponibilidad")
	public ResponseEntity<?> disponibilidad(@RequestParam(required = false) Long salaId,
	                                       @RequestParam(required = false) LocalDate fecha,
	                                       @RequestParam(required = false) LocalTime horaInicio,
	                                       @RequestParam(required = false) LocalTime horaFin,
	                                       HttpServletRequest request) {

		// Precondiciones: validar campos y devolver ApiError.fields cuando falten
		Map<String, String> campos = new LinkedHashMap<>();
		if (salaId == null) campos.put("salaId", "salaId es obligatorio");
		if (fecha == null) campos.put("fecha", "fecha es obligatoria");
		if (horaInicio == null) campos.put("horaInicio", "horaInicio es obligatorio");
		if (horaFin == null) campos.put("horaFin", "horaFin es obligatorio");
		if (!campos.isEmpty()) {
			return ResponseEntity.badRequest().body(com.cleancodecrew.sweg.dto.ApiError.of(400, "Campos obligatorios ausentes", request.getRequestURI(), campos));
		}

		// validar rango horario
		Reserva consultaTemp = Reserva.deConsulta(null, fecha, horaInicio, horaFin);
		try { consultaTemp.validarRangoHorario(); } catch (IllegalArgumentException ex) {
			campos.put("horaInicio/horaFin", ex.getMessage());
			return ResponseEntity.badRequest().body(com.cleancodecrew.sweg.dto.ApiError.of(400, "Rango horario invalido", request.getRequestURI(), campos));
		}

		// validar fecha no pasada
		try { consultaTemp.validarFechaNoPasada(); } catch (IllegalArgumentException ex) {
			campos.put("fecha", ex.getMessage());
			return ResponseEntity.badRequest().body(com.cleancodecrew.sweg.dto.ApiError.of(400, "Fecha invalida", request.getRequestURI(), campos));
		}

		var maybeSala = salaRepository.findById(salaId);
		if (maybeSala.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Sala no encontrada"));

		Sala sala = maybeSala.get();
		Reserva consulta = Reserva.deConsulta(sala, fecha, horaInicio, horaFin);
		var activas = reservaRepository.findActivasPorSalaYFecha(salaId, fecha);
		boolean ocupada = activas.stream().anyMatch(consulta::seSolapaCon);
		DisponibilidadResponse resp = new DisponibilidadResponse(salaId, sala.getNombre(), fecha, horaInicio, horaFin, !ocupada, ocupada ? "No disponible" : "Disponible");
		return ResponseEntity.ok(resp);
	}

	/** CA-HU06-01 */
	@PatchMapping("/{id}/cancelar")
	public ResponseEntity<?> cancelar(@PathVariable Long id, HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long clienteId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);
		var maybeCliente = usuarioRepository.findById(clienteId);
		if (maybeCliente.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Cliente no encontrado"));
		Usuario cliente = maybeCliente.get();
		var maybe = reservaRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Reserva no encontrada"));
		Reserva r = maybe.get();
		r.cancelar(cliente);
		reservaRepository.save(r);
		return ResponseEntity.ok(Map.of("mensaje","Reserva cancelada correctamente","id", r.getId(), "estado", r.getEstado().name()));
	}
}
