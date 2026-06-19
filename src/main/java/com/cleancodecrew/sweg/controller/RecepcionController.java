package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ApiError;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionController.class);

    private final ReservaRepository reservaRepo;
    private final SalaRepository salaRepository;

    public RecepcionController(ReservaRepository reservaRepo, SalaRepository salaRepository) {
        this.reservaRepo = reservaRepo;
        this.salaRepository = salaRepository;
    }

    /** CA-HU07-01, CA-HU07-04: busca reservas activas del dia por cliente. */
    @GetMapping("/buscar-reserva")
    public ResponseEntity<?> buscar(
            @RequestParam(name = "correoCliente", required = false) String correoCliente,
            @RequestParam(name = "correo", required = false) String correoAlias,
            HttpServletRequest req) {

        String correo = correoCliente != null ? correoCliente : correoAlias;
        if (correo == null || correo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(400, "Parámetro 'correoCliente' es obligatorio", req.getRequestURI()));
        }

        correo = correo.trim().toLowerCase();
        log.info("Recepcion: buscar reservas para correo={}", correo);

        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = reservaRepo.findReservasDelDiaPorCorreoCliente(correo, hoy);

        if (reservas.isEmpty()) {
            for (int i = 1; i <= 7; i++) {
                LocalDate d = hoy.plusDays(i);
                List<Reserva> siguientes = reservaRepo.findReservasDelDiaPorCorreoCliente(correo, d);
                if (!siguientes.isEmpty()) {
                    reservas = siguientes;
                    log.info("Recepcion: {} reservas encontradas para {} el dia {}", reservas.size(), correo, d);
                    break;
                }
            }
        }

        if (reservas.isEmpty()) {
            log.info("Recepcion: no se encontraron reservas para {}", correo);
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(reservas.stream().map(ReservaResponse::de).toList());
    }

    /** CA-HU07-02, CA-HU07-03: registra ingreso y persiste el estado de la sala. */
    @Transactional
    @PatchMapping("/reservas/{id}/ingreso")
    public ResponseEntity<?> registrarIngreso(@PathVariable Long id) {
        Reserva r = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no existe"));
        r.registrarIngreso(LocalDateTime.now());
        reservaRepo.save(r);
        salaRepository.save(r.getSala());
        return ResponseEntity.ok(ReservaResponse.de(r));
    }

    /**
     * CA-HU08-01,03,04: Registra la salida del cliente.
     * @param limpieza true = sala pasa a EN_LIMPIEZA; false = sala pasa a DISPONIBLE
     */
    @Transactional
    @PatchMapping("/reservas/{id}/salida")
    public ResponseEntity<?> registrarSalida(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean limpieza,
            HttpServletRequest req) {
        Reserva r = reservaRepo.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Reserva no encontrada", req.getRequestURI()));
        }
        try {
            r.registrarSalida(limpieza, LocalDateTime.now());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, ex.getMessage(), req.getRequestURI()));
        }
        reservaRepo.save(r);
        salaRepository.save(r.getSala());
        log.info("Recepcion: salida registrada reserva={} limpieza={} sala='{}'",
                id, limpieza, r.getSala().getNombre());
        return ResponseEntity.ok(ReservaResponse.de(r));
    }

    /** CA-HU08-01: Lista todas las reservas actualmente EN_USO (para vista rápida del recepcionista). */
    @GetMapping("/salas-en-uso")
    public ResponseEntity<List<ReservaResponse>> salasEnUso() {
        return ResponseEntity.ok(reservaRepo.findTodasEnUso()
                .stream().map(ReservaResponse::de).toList());
    }

    /** CA-HU08-03: Marca una sala EN_LIMPIEZA como DISPONIBLE una vez finalizada la limpieza. */
    @Transactional
    @PatchMapping("/salas/{id}/disponible")
    public ResponseEntity<?> marcarDisponible(@PathVariable Long id, HttpServletRequest req) {
        var maybe = salaRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Sala no encontrada", req.getRequestURI()));
        }
        var sala = maybe.get();
        if (sala.getEstado() != EstadoSala.EN_LIMPIEZA) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, "La sala no está en limpieza", req.getRequestURI()));
        }
        sala.marcarDisponible();
        salaRepository.save(sala);
        log.info("Recepcion: sala '{}' marcada disponible tras limpieza", sala.getNombre());
        return ResponseEntity.ok(Map.of("mensaje", "Sala disponible", "salaId", id, "estado", sala.getEstado().name()));
    }
}
