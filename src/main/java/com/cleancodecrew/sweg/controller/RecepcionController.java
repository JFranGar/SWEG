package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ApiError;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
}
