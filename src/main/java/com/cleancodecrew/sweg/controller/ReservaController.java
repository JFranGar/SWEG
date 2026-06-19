package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.HorarioReglaResponse;
import com.cleancodecrew.sweg.dto.ReservaRequest;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.model.TipoRegla;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.HorarioReglaRepository;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import com.cleancodecrew.sweg.config.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
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
	private final HorarioReglaRepository horarioReglaRepository;

	public ReservaController(SalaRepository salaRepository, ReservaRepository reservaRepository,
	                         UsuarioRepository usuarioRepository, HorarioReglaRepository horarioReglaRepository) {
		this.salaRepository = salaRepository;
		this.reservaRepository = reservaRepository;
		this.usuarioRepository = usuarioRepository;
		this.horarioReglaRepository = horarioReglaRepository;
	}

	@GetMapping("/salas-disponibles")
	public ResponseEntity<List<Sala>> salasDisponibles() {
		var list = salaRepository.findAll().stream().filter(s -> s.getEstado() == null || s.getEstado().name().equals("DISPONIBLE") || s.getEstado() == com.cleancodecrew.sweg.model.EstadoSala.DISPONIBLE).toList();
		return ResponseEntity.ok(list);
	}

	@GetMapping
	public ResponseEntity<?> misReservas(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long clienteId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);
		var pageable = PageRequest.of(page, Math.min(size, 50));
		var resultado = reservaRepository.findByCliente_IdOrderByFechaDescHoraInicioDesc(clienteId, pageable)
				.map(ReservaResponse::de);
		return ResponseEntity.ok(resultado);
	}

	// CA-HU04-01, CA-HU04-02, CA-HU04-03, CA-HU04-04, CA-HU04-05
	@Transactional
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

		if (req.getCantidadPersonas() > sala.getCapacidadMaxima()) {
			throw new ConflictoException("La cantidad de personas (" + req.getCantidadPersonas()
					+ ") supera la capacidad máxima de la sala (" + sala.getCapacidadMaxima() + ")");
		}

		Reserva reserva = Reserva.builder()
				.sala(sala)
				.cliente(cliente)
				.fecha(req.getFecha())
				.horaInicio(req.getHoraInicio())
				.horaFin(req.getHoraFin())
				.cantidadPersonas(req.getCantidadPersonas())
				.build();

		reserva.validarTodo();

		// Validar horario comercial
		validarReglas(req.getFecha(), req.getHoraInicio(), req.getHoraFin());

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
		if (sala.getEstado() != com.cleancodecrew.sweg.model.EstadoSala.DISPONIBLE) {
			DisponibilidadResponse resp = new DisponibilidadResponse(salaId, sala.getNombre(), fecha, horaInicio, horaFin,
					false, "Sala no disponible (" + sala.getEstado().name() + ")");
			return ResponseEntity.ok(resp);
		}
		Reserva consulta = Reserva.deConsulta(sala, fecha, horaInicio, horaFin);
		var activas = reservaRepository.findActivasPorSalaYFecha(salaId, fecha);
		boolean ocupada = activas.stream().anyMatch(consulta::seSolapaCon);
		DisponibilidadResponse resp = new DisponibilidadResponse(salaId, sala.getNombre(), fecha, horaInicio, horaFin, !ocupada, ocupada ? "No disponible" : "Disponible");
		return ResponseEntity.ok(resp);
	}

	/** Devuelve todas las reservas activas de una sala en un día para renderizar el timeline. */
	@GetMapping("/horario-dia")
	public ResponseEntity<?> horarioDia(@RequestParam Long salaId, @RequestParam LocalDate fecha) {
		var maybeSala = salaRepository.findById(salaId);
		if (maybeSala.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sala no encontrada"));
		var activas = reservaRepository.findOcupadasPorSalaYFecha(salaId, fecha);
		return ResponseEntity.ok(activas.stream().map(ReservaResponse::de).toList());
	}

	/** Reglas de horario activas visibles para el cliente (usadas por el timeline). */
	@GetMapping("/reglas-activas")
	public ResponseEntity<List<HorarioReglaResponse>> reglasActivas() {
		return ResponseEntity.ok(horarioReglaRepository.findByActivoTrue()
				.stream().map(HorarioReglaResponse::de).toList());
	}

	/** Valida que horaInicio-horaFin respete las reglas APERTURA y BLOQUEO activas para la fecha dada. */
	private void validarReglas(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
		var reglas = horarioReglaRepository.findByActivoTrue();
		if (reglas.isEmpty()) return;
		int dia = fecha.getDayOfWeek().getValue(); // ISO: 1=Lun…7=Dom
		var aperturas = reglas.stream()
				.filter(r -> r.getTipo() == TipoRegla.APERTURA)
				.filter(r -> r.getDiaSemana() == null || Integer.valueOf(dia).equals(r.getDiaSemana()))
				.toList();
		if (!aperturas.isEmpty()) {
			boolean dentro = aperturas.stream().anyMatch(r ->
					!horaInicio.isBefore(r.getHoraInicio()) && !horaFin.isAfter(r.getHoraFin()));
			if (!dentro) throw new ConflictoException("La reserva está fuera del horario comercial");
		}
		var bloqueos = reglas.stream()
				.filter(r -> r.getTipo() == TipoRegla.BLOQUEO)
				.filter(r -> r.getDiaSemana() == null || Integer.valueOf(dia).equals(r.getDiaSemana()))
				.toList();
		for (var r : bloqueos) {
			if (horaInicio.isBefore(r.getHoraFin()) && r.getHoraInicio().isBefore(horaFin)) {
				throw new ConflictoException("El horario está bloqueado"
						+ (r.getDescripcion() != null ? ": " + r.getDescripcion() : ""));
			}
		}
	}

	/** Editar una reserva propia (solo PENDIENTE o CONFIRMADA). */
	@Transactional
	@PutMapping("/{id}")
	public ResponseEntity<?> editar(@PathVariable Long id, @Valid @RequestBody ReservaRequest req,
	                                HttpServletRequest request) {
		var session = request.getSession(false);
		if (session == null || session.getAttribute(AuthInterceptor.SESSION_USER_ID) == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long clienteId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_ID);

		var maybe = reservaRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Reserva no encontrada"));
		Reserva r = maybe.get();

		if (!r.getCliente().getId().equals(clienteId))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No autorizado"));

		if (r.getEstado() != EstadoReserva.PENDIENTE && r.getEstado() != EstadoReserva.CONFIRMADA)
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("error", "Solo se pueden editar reservas PENDIENTE o CONFIRMADA"));

		var maybeSala = salaRepository.findById(req.getSalaId());
		if (maybeSala.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sala no encontrada"));

		Sala salaNueva = maybeSala.get();

		if (req.getCantidadPersonas() > salaNueva.getCapacidadMaxima()) {
			throw new ConflictoException("La cantidad de personas (" + req.getCantidadPersonas()
					+ ") supera la capacidad máxima de la sala (" + salaNueva.getCapacidadMaxima() + ")");
		}

		r.setSala(salaNueva);
		r.setFecha(req.getFecha());
		r.setHoraInicio(req.getHoraInicio());
		r.setHoraFin(req.getHoraFin());
		r.setCantidadPersonas(req.getCantidadPersonas());
		r.validarTodo();

		validarReglas(req.getFecha(), req.getHoraInicio(), req.getHoraFin());

		var candidatas = reservaRepository.findBySala_IdAndFechaAndEstadoNot(req.getSalaId(), req.getFecha(), EstadoReserva.CANCELADA);
		for (Reserva candidata : candidatas) {
			if (candidata.getId().equals(id)) continue;
			if (r.seSolapaCon(candidata)) throw new ConflictoException("La sala ya está reservada en ese horario");
		}

		return ResponseEntity.ok(ReservaResponse.de(reservaRepository.save(r)));
	}

	/** CA-HU06-01 */
	@Transactional
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
