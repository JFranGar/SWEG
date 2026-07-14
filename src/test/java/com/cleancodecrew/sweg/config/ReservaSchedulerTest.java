package com.cleancodecrew.sweg.config;

import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.model.TipoSala;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas del ReservaScheduler (cierre automático de reservas vencidas y liberación
 * de salas). Repositorios mockeados; sin planificador real ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReservaSchedulerTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private SalaRepository salaRepository;
    @InjectMocks private ReservaScheduler scheduler;

    private Sala sala(EstadoSala estado) {
        return Sala.builder().id(1L).nombre("Sala A").tipo(TipoSala.REUNION)
                .capacidadMaxima(4).estado(estado).build();
    }

    private Reserva reservaVencida(Sala sala, EstadoReserva estado) {
        return Reserva.builder().id(10L).sala(sala)
                .fecha(LocalDate.now().minusDays(1))
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(estado).build();
    }

    @Test
    @DisplayName("Sin reservas vencidas no persiste nada")
    void sinVencidas_noHaceNada() {
        when(reservaRepository.findReservasVencidas(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of());

        scheduler.finalizarReservasVencidas();

        verify(reservaRepository, never()).save(any());
        verify(salaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Finaliza una CONFIRMADA vencida sin tocar la sala si no está OCUPADA")
    void finalizaConfirmada_sinLiberarSala() {
        Sala sala = sala(EstadoSala.DISPONIBLE);
        Reserva r = reservaVencida(sala, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findReservasVencidas(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(r));

        scheduler.finalizarReservasVencidas();

        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
        verify(reservaRepository).save(r);
        verify(salaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cierra una EN_USO rezagada, registra la salida y libera la sala OCUPADA")
    void finalizaEnUso_liberaSala() {
        Sala sala = sala(EstadoSala.OCUPADA);
        Reserva r = reservaVencida(sala, EstadoReserva.EN_USO);
        when(reservaRepository.findReservasVencidas(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(r));
        when(reservaRepository.existsBySalaIdAndEstado(1L, EstadoReserva.EN_USO)).thenReturn(false);

        scheduler.finalizarReservasVencidas();

        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
        assertNotNull(r.getFechaSalida(), "El check-out automático registra la hora de salida");
        assertEquals(EstadoSala.DISPONIBLE, sala.getEstado());
        verify(salaRepository).save(sala);
    }

    @Test
    @DisplayName("No libera la sala si aún hay otra reserva EN_USO para ella")
    void enUso_conOtraActiva_noLibera() {
        Sala sala = sala(EstadoSala.OCUPADA);
        Reserva r = reservaVencida(sala, EstadoReserva.EN_USO);
        when(reservaRepository.findReservasVencidas(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(r));
        when(reservaRepository.existsBySalaIdAndEstado(eq(1L), eq(EstadoReserva.EN_USO))).thenReturn(true);

        scheduler.finalizarReservasVencidas();

        assertEquals(EstadoSala.OCUPADA, sala.getEstado());
        verify(salaRepository, never()).save(any());
    }
}
