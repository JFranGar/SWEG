package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.PanelSalaResponse;
import com.cleancodecrew.sweg.dto.SalaRequest;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.model.EstadoSala;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/admin/salas", "/api/salas"})
public class SalaController {

	private final SalaRepository salaRepository;
	private final ReservaRepository reservaRepository;

	public SalaController(SalaRepository salaRepository, ReservaRepository reservaRepository) {
		this.salaRepository = salaRepository;
		this.reservaRepository = reservaRepository;
	}

	// CA-HU02-07
	@GetMapping
	public ResponseEntity<List<Sala>> listAll() {
		var list = salaRepository.findAll();
		list = new ArrayList<>(list.stream()
				.filter(s -> s.getEstado() != EstadoSala.ELIMINADA)
				.toList());
		list.sort((a,b)->a.getNombre().compareToIgnoreCase(b.getNombre()));
		return ResponseEntity.ok(list);
	}

	// CA-HU02-01, CA-HU02-02, CA-HU02-04
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody SalaRequest req) {
		if (salaRepository.findByNombreIgnoreCase(req.getNombre()).isPresent()) {
			throw new DuplicadoException("Ya existe una sala con ese nombre");
		}
		Sala s = Sala.builder()
				.nombre(req.getNombre())
				.tipo(req.getTipo())
				.capacidadMaxima(req.getCapacidadMaxima())
				.build();
		s.validarCamposObligatorios();
		Sala saved = salaRepository.save(s);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	// CA-HU02-03, CA-HU02-02, CA-HU02-04
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SalaRequest req) {
		var maybe = salaRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		if (salaRepository.existsByNombreIgnoreCaseAndIdNot(req.getNombre(), id)) {
			throw new DuplicadoException("Ya existe otra sala con ese nombre");
		}
		Sala s = maybe.get();
		s.actualizarDatos(req.getNombre(), req.getTipo(), req.getCapacidadMaxima());
		salaRepository.save(s);
		return ResponseEntity.ok(s);
	}

	/**
	 * CA-HU09-01,02,03: Panel de control con estado efectivo de todas las salas.
	 * Estado efectivo: si hay reserva CONFIRMADA/PENDIENTE hoy -> RESERVADA;
	 * si sala.estado = OCUPADA -> EN_USO; otros estados propios de la sala se usan tal cual.
	 */
	@GetMapping("/panel")
	public ResponseEntity<List<PanelSalaResponse>> panel() {
		LocalDate hoy = LocalDate.now();
		Set<Long> salasConReservaHoy = reservaRepository.findActivasHoy(hoy)
				.stream().map(r -> r.getSala().getId()).collect(Collectors.toSet());

		List<PanelSalaResponse> panelList = salaRepository.findAll().stream()
				.filter(s -> s.getEstado() != EstadoSala.ELIMINADA)
				.map(s -> {
					String estadoPanel = switch (s.getEstado()) {
						case OCUPADA       -> "EN_USO";
						case EN_LIMPIEZA   -> "EN_LIMPIEZA";
						case MANTENIMIENTO -> "MANTENIMIENTO";
						default -> salasConReservaHoy.contains(s.getId()) ? "RESERVADA" : "DISPONIBLE";
					};
					return new PanelSalaResponse(s.getId(), s.getNombre(), s.getTipo().name(), s.getCapacidadMaxima(), estadoPanel);
				})
				.sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
				.toList();
		return ResponseEntity.ok(panelList);
	}

	// CA-HU02-06
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest req) {
		var maybe = salaRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

		Sala sala = maybe.get();
		sala.eliminar();
		salaRepository.save(sala);
		return ResponseEntity.noContent().build();
	}
}
