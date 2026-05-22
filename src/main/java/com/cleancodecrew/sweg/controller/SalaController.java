package com.cleancodecrew.sweg.controller;

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
import java.util.ArrayList;
import java.util.List;

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
