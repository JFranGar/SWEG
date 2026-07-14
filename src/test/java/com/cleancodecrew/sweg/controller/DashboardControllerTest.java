package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.DashboardResponse;
import com.cleancodecrew.sweg.model.*;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del DashboardController (HU10 - indicadores). Un caso completo ejercita
 * las tarjetas y todas las series con reservas de estados y fechas variados.
 */
class DashboardControllerTest {

    private UsuarioRepository usuarioRepository;
    private SalaRepository salaRepository;
    private ReservaRepository reservaRepository;
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        salaRepository = mock(SalaRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        controller = new DashboardController(usuarioRepository, salaRepository, reservaRepository);
    }

    private Sala sala(long id, String nombre, EstadoSala estado) {
        return Sala.builder().id(id).nombre(nombre).tipo(TipoSala.REUNION)
                .capacidadMaxima(4).estado(estado).build();
    }

    private Usuario cliente(long id) {
        return Usuario.builder().id(id).nombre("Cli" + id).correo("c" + id + "@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.CLIENTE).build();
    }

    @Test
    @DisplayName("dashboard vacío responde 200 con series inicializadas")
    void dashboard_vacio() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(reservaRepository.findAllDetallado()).thenReturn(List.of());
        when(salaRepository.findAll()).thenReturn(List.of());

        ResponseEntity<DashboardResponse> resp = controller.dashboard();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        DashboardResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(7, body.reservasPorDia().size(), "Siempre 7 días");
        assertEquals(15, body.ocupacionPorHorario().size(), "07:00 a 21:00 -> 15 franjas");
        assertEquals(5, body.reservasPorEstado().size());
    }

    @Test
    @DisplayName("dashboard con datos calcula tarjetas, actividad y series")
    void dashboard_conDatos() {
        LocalDate hoy = LocalDate.now();
        Sala s1 = sala(1, "Alfa", EstadoSala.OCUPADA);
        Sala s2 = sala(2, "Beta", EstadoSala.DISPONIBLE);

        Reserva confirmada = Reserva.builder().id(1L).sala(s2).cliente(cliente(1))
                .fecha(hoy).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).creadaEn(LocalDateTime.now()).build();
        Reserva enUso = Reserva.builder().id(2L).sala(s1).cliente(cliente(1))
                .fecha(hoy).horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(11, 0))
                .estado(EstadoReserva.EN_USO).creadaEn(LocalDateTime.now())
                .fechaIngreso(LocalDateTime.now()).build();
        Reserva finalizada = Reserva.builder().id(3L).sala(s1).cliente(cliente(2))
                .fecha(hoy).horaInicio(LocalTime.of(11, 0)).horaFin(LocalTime.of(12, 0))
                .estado(EstadoReserva.FINALIZADA).creadaEn(LocalDateTime.now())
                .fechaSalida(LocalDateTime.now()).build();
        Reserva cancelada = Reserva.builder().id(4L).sala(s2).cliente(cliente(2))
                .fecha(hoy).horaInicio(LocalTime.of(13, 0)).horaFin(LocalTime.of(14, 0))
                .estado(EstadoReserva.CANCELADA).creadaEn(LocalDateTime.now())
                .fechaCancelacion(LocalDateTime.now()).build();

        when(usuarioRepository.count()).thenReturn(10L);
        when(usuarioRepository.countByRol(Rol.CLIENTE)).thenReturn(7L);
        when(usuarioRepository.countByRol(Rol.RECEPCIONISTA)).thenReturn(2L);
        when(usuarioRepository.countByRol(Rol.ADMIN)).thenReturn(1L);
        when(reservaRepository.findAllDetallado())
                .thenReturn(List.of(confirmada, enUso, finalizada, cancelada));
        when(salaRepository.findAll()).thenReturn(List.of(s1, s2));

        DashboardResponse body = controller.dashboard().getBody();

        assertNotNull(body);
        assertEquals(10L, body.totalUsuarios());
        assertEquals(7L, body.clientesActivos());
        assertEquals(1L, body.reservasConfirmadas());
        assertEquals(1L, body.reservasEnUso());
        assertEquals(1L, body.reservasFinalizadas());
        assertEquals(1L, body.reservasCanceladas());
        assertEquals(1L, body.checkinsHoy());
        assertEquals(1L, body.checkoutsHoy());
        assertEquals(1L, body.espaciosOcupados(), "s1 está OCUPADA");
        assertFalse(body.actividadReciente().isEmpty());
        assertFalse(body.usoPorTipoSala().isEmpty());
    }
}
