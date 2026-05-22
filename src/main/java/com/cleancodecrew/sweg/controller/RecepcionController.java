package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    private final ReservaRepository reservaRepo;
    private static final Logger log = LoggerFactory.getLogger(RecepcionController.class);

    public RecepcionController(ReservaRepository reservaRepo) {
        this.reservaRepo = reservaRepo;
    }

    /** CA-HU07-01, CA-HU07-04: busca reservas activas del dia por cliente. */
    @GetMapping("/buscar-reserva")
    public ResponseEntity<?> buscar(@RequestParam(name = "correoCliente", required = false) String correoCliente,
                                    @RequestParam(name = "correo", required = false) String correoAlias,
                                    HttpServletRequest req) {
        String correo = correoCliente != null ? correoCliente : correoAlias;
        if (correo == null || correo.isBlank()) {
            return ResponseEntity.badRequest().body(com.cleancodecrew.sweg.dto.ApiError.of(400, "Parámetro 'correoCliente' es obligatorio", req.getRequestURI()));
        }

        correo = correo.trim().toLowerCase();
        log.info("Recepcion: buscar reservas para correo={} (inicio busqueda) ", correo);

        // First try: today
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = reservaRepo.findReservasDelDiaPorCorreoCliente(correo, hoy);

        // If none for today, try the next 7 days (useful for reception search)
        if (reservas.isEmpty()) {
            log.info("No se encontraron reservas hoy para {}. Buscando próximos días...", correo);
            for (int i = 1; i <= 7 && reservas.isEmpty(); i++) {
                LocalDate d = hoy.plusDays(i);
                List<Reserva> r2 = reservaRepo.findReservasDelDiaPorCorreoCliente(correo, d);
                if (r2 != null && !r2.isEmpty()) {
                    reservas = r2;
                    log.info("Se encontraron {} reservas para {} el día {}", r2.size(), correo, d);
                    break;
                }
            }
        }
        if (reservas == null || reservas.isEmpty()) {
            log.info("Recepcion: no se encontraron reservas para {} en ventana de busqueda", correo);
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        log.info("Recepcion: devolviendo {} reservas para {}", reservas.size(), correo);
        return ResponseEntity.ok(reservas.stream().map(ReservaResponse::de).toList());
    }

    /** CA-HU07-02, CA-HU07-03: registra ingreso. */
    @PatchMapping("/reservas/{id}/ingreso")
    public ResponseEntity<?> registrarIngreso(@PathVariable Long id) {
        Reserva r = reservaRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Reserva no existe"));
        r.registrarIngreso(LocalDateTime.now());
        reservaRepo.save(r);
        return ResponseEntity.ok(ReservaResponse.de(r));
    }

    /** Debug: lista todas las reservas asociadas a un correo (sin filtrar por fecha). */
    @GetMapping("/debug-reservas")
    public ResponseEntity<?> debugReservas(@RequestParam String correo) {
        var list = reservaRepo.findByCliente_CorreoIgnoreCase(correo.trim().toLowerCase());
        return ResponseEntity.ok(list.stream().map(ReservaResponse::de).toList());
    }
}
