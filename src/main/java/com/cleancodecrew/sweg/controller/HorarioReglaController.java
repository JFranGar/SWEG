package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.HorarioReglaRequest;
import com.cleancodecrew.sweg.dto.HorarioReglaResponse;
import com.cleancodecrew.sweg.model.HorarioRegla;
import com.cleancodecrew.sweg.repository.HorarioReglaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/horario-reglas")
public class HorarioReglaController {

    private final HorarioReglaRepository repo;

    public HorarioReglaController(HorarioReglaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<HorarioReglaResponse>> listar() {
        return ResponseEntity.ok(repo.findAll().stream().map(HorarioReglaResponse::de).toList());
    }

    @PostMapping
    public ResponseEntity<HorarioReglaResponse> crear(@Valid @RequestBody HorarioReglaRequest req) {
        HorarioRegla r = HorarioRegla.builder()
                .tipo(req.getTipo())
                .horaInicio(req.getHoraInicio())
                .horaFin(req.getHoraFin())
                .diaSemana(req.getDiaSemana())
                .descripcion(req.getDescripcion())
                .activo(req.isActivo())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(HorarioReglaResponse.de(repo.save(r)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody HorarioReglaRequest req) {
        return repo.findById(id).map(r -> {
            r.setTipo(req.getTipo());
            r.setHoraInicio(req.getHoraInicio());
            r.setHoraFin(req.getHoraFin());
            r.setDiaSemana(req.getDiaSemana());
            r.setDescripcion(req.getDescripcion());
            r.setActivo(req.isActivo());
            return ResponseEntity.ok(HorarioReglaResponse.de(repo.save(r)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Regla eliminada"));
    }
}
