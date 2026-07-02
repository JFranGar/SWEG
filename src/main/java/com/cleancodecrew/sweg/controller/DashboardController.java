package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.AccesoResponse;
import com.cleancodecrew.sweg.dto.DashboardResponse;
import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DashboardController — Panel de indicadores del Administrador (HU Dashboard).
 *
 * Rutas bajo /api/admin/** (protegidas por AuthInterceptor: solo ADMIN).
 * Todas las métricas se calculan con datos reales del sistema (sin datos quemados)
 * usando la fecha/hora real del servidor.
 */
@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final UsuarioRepository usuarioRepository;
    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;

    public DashboardController(UsuarioRepository usuarioRepository,
                              SalaRepository salaRepository,
                              ReservaRepository reservaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
    }

    /** CA-HU10-01, CA-HU10-02, CA-HU10-03: indicadores y series del dashboard. */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = reservaRepository.findAllDetallado();
        List<Sala> salas = salaRepository.findAll().stream()
                .filter(s -> s.getEstado() != EstadoSala.ELIMINADA)
                .toList();

        // ── Tarjetas: usuarios ──
        long totalUsuarios = usuarioRepository.count();
        long clientes = usuarioRepository.countByRol(Rol.CLIENTE);
        long recepcionistas = usuarioRepository.countByRol(Rol.RECEPCIONISTA);
        long admins = usuarioRepository.countByRol(Rol.ADMIN);

        // ── Tarjetas: reservas ──
        long creadasHoy = reservas.stream()
                .filter(r -> r.getCreadaEn() != null && r.getCreadaEn().toLocalDate().equals(hoy)).count();
        long pendientes = contarEstado(reservas, EstadoReserva.PENDIENTE);
        long confirmadas = contarEstado(reservas, EstadoReserva.CONFIRMADA);
        long enUso = contarEstado(reservas, EstadoReserva.EN_USO);
        long canceladas = contarEstado(reservas, EstadoReserva.CANCELADA);
        long finalizadas = contarEstado(reservas, EstadoReserva.FINALIZADA);

        // ── Tarjetas: accesos del día ──
        long checkinsHoy = reservas.stream()
                .filter(r -> r.getFechaIngreso() != null && r.getFechaIngreso().toLocalDate().equals(hoy)).count();
        long checkoutsHoy = reservas.stream()
                .filter(r -> r.getFechaSalida() != null && r.getFechaSalida().toLocalDate().equals(hoy)).count();

        // ── Tarjetas: espacios ──
        Set<Long> salasReservadasHoy = reservas.stream()
                .filter(r -> r.getFecha().equals(hoy)
                        && (r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.PENDIENTE))
                .map(r -> r.getSala().getId())
                .collect(Collectors.toSet());
        long ocupados = salas.stream().filter(s -> s.getEstado() == EstadoSala.OCUPADA).count();
        long reservados = salas.stream()
                .filter(s -> s.getEstado() == EstadoSala.DISPONIBLE && salasReservadasHoy.contains(s.getId())).count();
        long disponibles = salas.stream()
                .filter(s -> s.getEstado() == EstadoSala.DISPONIBLE && !salasReservadasHoy.contains(s.getId())).count();

        // ── Gráfica: reservas por día (últimos 7 días) ──
        List<DashboardResponse.Punto> reservasPorDia = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = hoy.minusDays(i);
            long cnt = reservas.stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA && d.equals(r.getFecha())).count();
            reservasPorDia.add(new DashboardResponse.Punto(d.format(DIA_FMT), cnt));
        }

        // ── Gráfica: reservas por estado ──
        List<DashboardResponse.Punto> reservasPorEstado = List.of(
                new DashboardResponse.Punto("Pendientes", pendientes),
                new DashboardResponse.Punto("Confirmadas", confirmadas),
                new DashboardResponse.Punto("En uso", enUso),
                new DashboardResponse.Punto("Finalizadas", finalizadas),
                new DashboardResponse.Punto("Canceladas", canceladas));

        // ── Gráfica: uso por tipo de sala ──
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (Reserva r : reservas) {
            if (r.getEstado() == EstadoReserva.CANCELADA) continue;
            String tipo = r.getSala().getTipo().name();
            porTipo.merge(tipo, 1L, Long::sum);
        }
        List<DashboardResponse.Punto> usoPorTipo = porTipo.entrySet().stream()
                .map(e -> new DashboardResponse.Punto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(DashboardResponse.Punto::valor).reversed())
                .toList();

        // ── Gráfica: check-ins/check-outs por día (últimos 7 días) ──
        List<DashboardResponse.PuntoAcceso> accesosPorDia = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = hoy.minusDays(i);
            long ci = reservas.stream()
                    .filter(r -> r.getFechaIngreso() != null && d.equals(r.getFechaIngreso().toLocalDate())).count();
            long co = reservas.stream()
                    .filter(r -> r.getFechaSalida() != null && d.equals(r.getFechaSalida().toLocalDate())).count();
            accesosPorDia.add(new DashboardResponse.PuntoAcceso(d.format(DIA_FMT), ci, co));
        }

        // ── Gráfica: clientes con mayor actividad (top 5) ──
        Map<String, Long> porCliente = new LinkedHashMap<>();
        for (Reserva r : reservas) {
            if (r.getCliente() == null) continue;
            porCliente.merge(r.getCliente().getNombre(), 1L, Long::sum);
        }
        List<DashboardResponse.Punto> topClientes = porCliente.entrySet().stream()
                .map(e -> new DashboardResponse.Punto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(DashboardResponse.Punto::valor).reversed())
                .limit(5)
                .toList();

        // ── Gráfica: ocupación por horario (07:00–21:00) ──
        List<DashboardResponse.Punto> ocupacionPorHorario = new ArrayList<>();
        for (int h = 7; h <= 21; h++) {
            final int hora = h;
            long cnt = reservas.stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA
                            && r.getHoraInicio() != null && r.getHoraInicio().getHour() == hora).count();
            ocupacionPorHorario.add(new DashboardResponse.Punto(String.format("%02d:00", hora), cnt));
        }

        // ── Actividad reciente ──
        List<DashboardResponse.Actividad> actividad = construirActividad(reservas);

        DashboardResponse resp = new DashboardResponse(
                totalUsuarios, clientes, recepcionistas, admins,
                creadasHoy, pendientes, confirmadas, enUso, canceladas, finalizadas,
                checkinsHoy, checkoutsHoy,
                salas.size(), disponibles, ocupados, reservados,
                reservasPorDia, reservasPorEstado, usoPorTipo, accesosPorDia,
                topClientes, ocupacionPorHorario, actividad);
        return ResponseEntity.ok(resp);
    }

    /** CA-HU11-01: Historial de check-in / check-out consultable por el administrador. */
    @GetMapping("/accesos")
    public ResponseEntity<List<AccesoResponse>> accesos() {
        List<AccesoResponse> eventos = new ArrayList<>();
        for (Reserva r : reservaRepository.findConAccesos()) {
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

    // ── Helpers ──

    private long contarEstado(List<Reserva> reservas, EstadoReserva estado) {
        return reservas.stream().filter(r -> r.getEstado() == estado).count();
    }

    private List<DashboardResponse.Actividad> construirActividad(List<Reserva> reservas) {
        List<DashboardResponse.Actividad> eventos = new ArrayList<>();
        for (Reserva r : reservas) {
            String sala = r.getSala().getNombre();
            String cliente = r.getCliente() != null ? r.getCliente().getNombre() : "—";
            if (r.getCreadaEn() != null) {
                eventos.add(new DashboardResponse.Actividad("RESERVA",
                        "Nueva reserva · " + sala + " · " + cliente, r.getCreadaEn()));
            }
            if (r.getFechaIngreso() != null) {
                eventos.add(new DashboardResponse.Actividad("CHECK_IN",
                        "Check-in · " + sala + " · " + cliente, r.getFechaIngreso()));
            }
            if (r.getFechaSalida() != null) {
                eventos.add(new DashboardResponse.Actividad("CHECK_OUT",
                        "Check-out · " + sala + " · " + cliente, r.getFechaSalida()));
            }
            if (r.getFechaCancelacion() != null) {
                eventos.add(new DashboardResponse.Actividad("CANCELACION",
                        "Reserva cancelada · " + sala + " · " + cliente, r.getFechaCancelacion()));
            }
        }
        return eventos.stream()
                .sorted(Comparator.comparing(DashboardResponse.Actividad::fecha).reversed())
                .limit(12)
                .toList();
    }
}
