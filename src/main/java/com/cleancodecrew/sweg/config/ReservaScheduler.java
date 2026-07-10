package com.cleancodecrew.sweg.config;

import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Finaliza reservas vencidas y libera salas ocupadas cuando el horario ya pasó.
 * Se ejecuta cada 2 minutos.
 */
@Component
public class ReservaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaScheduler.class);

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;

    public ReservaScheduler(ReservaRepository reservaRepository, SalaRepository salaRepository) {
        this.reservaRepository = reservaRepository;
        this.salaRepository    = salaRepository;
    }

    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void finalizarReservasVencidas() {
        LocalDateTime ahora       = LocalDateTime.now();
        LocalDate     hoy         = ahora.toLocalDate();
        LocalTime     horaActual  = ahora.toLocalTime();

        List<Reserva> vencidas = reservaRepository.findReservasVencidas(hoy, horaActual);
        if (vencidas.isEmpty()) return;

        log.info("Scheduler: finalizando {} reservas vencidas", vencidas.size());

        for (Reserva r : vencidas) {
            // Red de seguridad: una reserva EN_USO rezagada se cierra registrando como
            // hora de salida el fin de su rango horario (check-out automático del sistema).
            if (r.getEstado() == EstadoReserva.EN_USO && r.getFechaSalida() == null) {
                r.setFechaSalida(LocalDateTime.of(r.getFecha(), r.getHoraFin()));
            }
            r.setEstado(EstadoReserva.FINALIZADA);
            reservaRepository.save(r);

            // Liberar sala si ya no hay ninguna otra reserva EN_USO para ella
            var sala = r.getSala();
            if (sala.getEstado() == EstadoSala.OCUPADA) {
                boolean hayOtraEnUso = reservaRepository.existsBySalaIdAndEstado(sala.getId(), EstadoReserva.EN_USO);
                if (!hayOtraEnUso) {
                    sala.setEstado(EstadoSala.DISPONIBLE);
                    salaRepository.save(sala);
                    log.info("Scheduler: sala '{}' liberada", sala.getNombre());
                }
            }
        }
    }
}
