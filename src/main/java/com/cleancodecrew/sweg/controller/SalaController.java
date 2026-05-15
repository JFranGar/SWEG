package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.SalaRequest;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/salas")
public class SalaController {

	private final SalaRepository salaRepository;

	public SalaController(SalaRepository salaRepository) {
		this.salaRepository = salaRepository;
	}

	@GetMapping
	public ResponseEntity<List<Sala>> listAll() {
		var list = salaRepository.findAll();
		list.sort((a,b)->a.getNombre().compareToIgnoreCase(b.getNombre()));
		return ResponseEntity.ok(list);
	}

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

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		var maybe = salaRepository.findById(id);
		if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		salaRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
