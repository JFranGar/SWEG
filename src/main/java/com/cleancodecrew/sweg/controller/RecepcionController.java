package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.dto.AccesoResponse;
import com.cleancodecrew.sweg.dto.ApiError;
import com.cleancodecrew.sweg.dto.PanelSalaResponse;
import com.cleancodecrew.sweg.dto.ReservaResponse;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionController.class);

    private final ReservaRepository reservaRepo;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;

    public RecepcionController(ReservaRepository reservaRepo, SalaRepository salaRepository,
                               UsuarioRepository usuarioRepository) {
        this.reservaRepo = reservaRepo;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Recupera el usuario (recepcionista/admin) que ejecuta la accion, para la trazabilidad. */
    private Usuario operadorActual(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object idObj = session.getAttribute(AuthInterceptor.SESSION_USER_ID);
        if (idObj == null) return null;
        return usuarioRepository.findById((Long) idObj).orElse(null);
    }

    /** CA-HU07-01, CA-HU07-04: busca reservas activas del dia por cliente. */
    @GetMapping("/buscar-reserva")
    public ResponseEntity<Object> buscar(
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

    /** CA-HU07-02, CA-HU07-03, CA-HU11-02: registra ingreso (check-in) y persiste el estado de la sala. */
    @Transactional
    @PatchMapping("/reservas/{id}/ingreso")
    public ResponseEntity<Object> registrarIngreso(@PathVariable Long id, HttpServletRequest req) {
        Reserva r = reservaRepo.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Reserva no encontrada", req.getRequestURI()));
        }
        Usuario operador = operadorActual(req);
        try {
            r.registrarIngreso(LocalDateTime.now(), operador);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, ex.getMessage(), req.getRequestURI()));
        }
        reservaRepo.save(r);
        salaRepository.save(r.getSala());
        log.info("Recepcion: check-in reserva={} operador='{}'",
                id, operador != null ? operador.getCorreo() : "?");
        return ResponseEntity.ok(ReservaResponse.de(r));
    }

    /**
     * CA-HU08-01,03,04: Registra la salida del cliente.
     * @param limpieza true = sala pasa a EN_LIMPIEZA; false = sala pasa a DISPONIBLE
     */
    @Transactional
    @PatchMapping("/reservas/{id}/salida")
    public ResponseEntity<Object> registrarSalida(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean limpieza,
            HttpServletRequest req) {
        Reserva r = reservaRepo.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Reserva no encontrada", req.getRequestURI()));
        }
        Usuario operador = operadorActual(req);
        try {
            r.registrarSalida(limpieza, LocalDateTime.now(), operador);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(409, ex.getMessage(), req.getRequestURI()));
        }
        reservaRepo.save(r);
        salaRepository.save(r.getSala());
        log.info("Recepcion: check-out reserva={} limpieza={} sala='{}' operador='{}'",
                id, limpieza, r.getSala().getNombre(), operador != null ? operador.getCorreo() : "?");
        return ResponseEntity.ok(ReservaResponse.de(r));
    }

    /** CA-HU08-01: Lista todas las reservas actualmente EN_USO (para vista rápida del recepcionista). */
    @GetMapping("/salas-en-uso")
    public ResponseEntity<List<ReservaResponse>> salasEnUso() {
        return ResponseEntity.ok(reservaRepo.findTodasEnUso()
                .stream().map(ReservaResponse::de).toList());
    }

    /**
     * CA-HU11-01, CA-HU11-03: Historial de check-in / check-out consultable desde el módulo
     * de Recepción (el recepcionista, responsable de los accesos, conserva la trazabilidad).
     */
    @GetMapping("/accesos")
    public ResponseEntity<List<AccesoResponse>> accesos() {
        List<AccesoResponse> eventos = new ArrayList<>();
        for (Reserva r : reservaRepo.findConAccesos()) {
            String sala = r.getSala().getNombre();
            String cliente = r.getCliente() != null ? r.getCliente().getNombre() : null;
            String correo = r.getCliente() != null ? r.getCliente().getCorreo() : null;
            if (r.getFechaIngreso() != null) {
                eventos.add(new AccesoResponse(r.getId(), "CHECK_IN", r.getFechaIngreso(), sala, cliente, correo,
                        r.getIngresoPor() != null ? r.getIngresoPor().getNombre() : null,
                        r.getHoraInicio(), r.getHoraFin(), r.getEstado().name()));
            }
            if (r.getFechaSalida() != null) {
                eventos.add(new AccesoResponse(r.getId(), "CHECK_OUT", r.getFechaSalida(), sala, cliente, correo,
                        r.getSalidaPor() != null ? r.getSalidaPor().getNombre() : null,
                        r.getHoraInicio(), r.getHoraFin(), r.getEstado().name()));
            }
        }
        eventos.sort(Comparator.comparing(AccesoResponse::fecha).reversed());
        return ResponseEntity.ok(eventos);
    }

    /**
     * CA-HU09-01: Vista del día para recepcionistas — todas las salas con estado efectivo hoy.
     * Estado efectivo: OCUPADA→EN_USO, DISPONIBLE+reserva hoy→RESERVADA, demás usa sala.estado.
     */
    @GetMapping("/panel-salas")
    public ResponseEntity<List<PanelSalaResponse>> panelSalas() {
        LocalDate hoy = LocalDate.now();
        Set<Long> salasConReservaHoy = reservaRepo.findActivasHoy(hoy)
                .stream().map(r -> r.getSala().getId()).collect(Collectors.toSet());

        List<PanelSalaResponse> panel = salaRepository.findAll().stream()
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
        return ResponseEntity.ok(panel);
    }

    /** CA-HU08-02: Reservas del día para una sala específica (vista recepcionista). */
    @GetMapping("/salas/{id}/reservas-hoy")
    public ResponseEntity<Object> reservasSalaHoy(@PathVariable Long id, HttpServletRequest req) {
        if (salaRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(404, "Sala no encontrada", req.getRequestURI()));
        }
        var reservas = reservaRepo.findReservasDetalladasPorSalaYFecha(id, LocalDate.now());
        return ResponseEntity.ok(reservas.stream().map(ReservaResponse::de).toList());
    }

    /** CA-HU08-03: Marca una sala EN_LIMPIEZA como DISPONIBLE una vez finalizada la limpieza. */
    @Transactional
    @PatchMapping("/salas/{id}/disponible")
    public ResponseEntity<Object> marcarDisponible(@PathVariable Long id, HttpServletRequest req) {
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
