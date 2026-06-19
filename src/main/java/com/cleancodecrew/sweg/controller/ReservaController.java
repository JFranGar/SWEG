package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.DisponibilidadResponse;
import com.cleancodecrew.sweg.dto.HorarioReglaResponse;
import com.cleancodecrew.sweg.dto.ReservaRequest;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.model.TipoSala;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;

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
		var list = salaRepository.findAll().stream()
				.filter(s -> s.getEstado() == EstadoSala.DISPONIBLE)
				.toList();
		return ResponseEntity.ok(list);
	}

	/**
	 * CA-HU03-03: Salas reservables (DISPONIBLE u OCUPADA) para poblar los desplegables de búsqueda.
	 * La disponibilidad real por horario se consulta luego con /horario-dia sobre la sala elegida.
	 */
	@GetMapping("/salas-reservables")
	public ResponseEntity<List<Sala>> salasReservables(@RequestParam(required = false) String tipo) {
		TipoSala tipoFiltro = parseTipo(tipo);
		var list = salaRepository.findAll().stream()
				.filter(s -> s.getEstado() == EstadoSala.DISPONIBLE || s.getEstado() == EstadoSala.OCUPADA)
				.filter(s -> tipoFiltro == null || s.getTipo() == tipoFiltro)
				.sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
				.toList();
		return ResponseEntity.ok(list);
	}

	private TipoSala parseTipo(String tipo) {
		if (tipo == null || tipo.isBlank()) return null;
		try {
			return TipoSala.valueOf(tipo.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * CA-HU03-01,02,03,04: Busca salas disponibles filtrando por tipo y fecha.
	 * El rango de horas es OPCIONAL: si se envía, además filtra salas con solapamiento.
	 * Rechaza fechas pasadas y rangos inválidos.
	 */
	@GetMapping("/buscar-salas")
	public ResponseEntity<?> buscarSalas(
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) LocalDate fecha,
			@RequestParam(required = false) LocalTime horaInicio,
			@RequestParam(required = false) LocalTime horaFin,
			HttpServletRequest request) {

		Map<String, String> errores = new LinkedHashMap<>();

		// fecha sigue siendo obligatoria
		if (fecha == null) {
			errores.put("fecha", "La fecha es obligatoria");
			return ResponseEntity.badRequest().body(
					com.cleancodecrew.sweg.dto.ApiError.of(400, "Campos obligatorios ausentes", request.getRequestURI(), errores));
		}

		// CA-HU03-04: rechazar fechas pasadas
		if (fecha.isBefore(LocalDate.now())) {
			errores.put("fecha", "No se permiten consultas en fechas pasadas");
			return ResponseEntity.badRequest().body(
					com.cleancodecrew.sweg.dto.ApiError.of(400, "Fecha inválida", request.getRequestURI(), errores));
		}

		// El rango horario es opcional, pero si se envía uno debe venir completo y ser válido
		boolean filtrarPorHora = horaInicio != null || horaFin != null;
		if (filtrarPorHora) {
			if (horaInicio == null || horaFin == null) {
				errores.put("horaInicio", "Debe indicar hora de inicio y fin, o ninguna");
				return ResponseEntity.badRequest().body(
						com.cleancodecrew.sweg.dto.ApiError.of(400, "Rango horario incompleto", request.getRequestURI(), errores));
			}
			if (!horaInicio.isBefore(horaFin)) {
				errores.put("horaInicio", "La hora de inicio debe ser menor a la hora de fin");
				return ResponseEntity.badRequest().body(
						com.cleancodecrew.sweg.dto.ApiError.of(400, "Rango horario inválido", request.getRequestURI(), errores));
			}
		}

		TipoSala tipoFiltro = parseTipo(tipo);

		// Si hay rango horario, excluir salas con solapamiento en ese intervalo
		final Set<Long> ocupadas = filtrarPorHora
				? new java.util.HashSet<>(reservaRepository.findSalaIdsConReservaEnHorario(fecha, horaInicio, horaFin))
				: java.util.Collections.emptySet();

		final TipoSala tipoFinal = tipoFiltro;
		List<Sala> disponibles = salaRepository.findAll().stream()
				.filter(s -> s.getEstado() == EstadoSala.DISPONIBLE || s.getEstado() == EstadoSala.OCUPADA)
				.filter(s -> tipoFinal == null || s.getTipo() == tipoFinal)
				.filter(s -> !ocupadas.contains(s.getId()))
				.sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
				.toList();

		return ResponseEntity.ok(disponibles);
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

		if (sala.getEstado() == EstadoSala.EN_LIMPIEZA) {
			throw new ConflictoException("La sala está en limpieza y no puede ser reservada");
		}
		if (sala.getEstado() == EstadoSala.MANTENIMIENTO) {
			throw new ConflictoException("La sala está en mantenimiento y no puede ser reservada");
		}

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

	private static final LocalTime HORA_APERTURA = LocalTime.of(7, 0);
	private static final LocalTime HORA_CIERRE   = LocalTime.of(22, 0);

	/** Valida que el horario esté dentro del rango comercial fijo 07:00–22:00. */
	private void validarReglas(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
		if (horaInicio.isBefore(HORA_APERTURA) || horaFin.isAfter(HORA_CIERRE)) {
			throw new ConflictoException("El horario debe estar entre 07:00 y 22:00");
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

		// Validar el nuevo rango antes de modificar la entidad
		Reserva check = Reserva.deConsulta(salaNueva, req.getFecha(), req.getHoraInicio(), req.getHoraFin());
		check.validarRangoHorario();

		validarReglas(req.getFecha(), req.getHoraInicio(), req.getHoraFin());

		// Buscar conflictos excluyendo a nivel SQL la propia reserva (evita falsos positivos por caché JPA)
		var candidatas = reservaRepository.findOtrasActivasPorSalaYFecha(req.getSalaId(), req.getFecha(), id);
		for (Reserva candidata : candidatas) {
			if (check.seSolapaCon(candidata)) throw new ConflictoException("La sala ya está reservada en ese horario");
		}

		// Aplicar los cambios y persistir
		r.setSala(salaNueva);
		r.setFecha(req.getFecha());
		r.setHoraInicio(req.getHoraInicio());
		r.setHoraFin(req.getHoraFin());
		r.setCantidadPersonas(req.getCantidadPersonas());
		r.validarTodo();

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
