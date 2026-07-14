package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.AccesoResponse;
import com.cleancodecrew.sweg.dto.PanelSalaResponse;
import com.cleancodecrew.sweg.model.*;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas del RecepcionController (HU7 check-in, HU8 check-out, HU9 panel, HU11 accesos).
 * Repositorios y request mockeados; sin contexto Spring ni base de datos.
 */
class RecepcionControllerTest {

    private ReservaRepository reservaRepo;
    private SalaRepository salaRepository;
    private UsuarioRepository usuarioRepository;
    private RecepcionController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        reservaRepo = mock(ReservaRepository.class);
        salaRepository = mock(SalaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        controller = new RecepcionController(reservaRepo, salaRepository, usuarioRepository);
        request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null); // operador anónimo (sin sesión)
    }

    private Sala sala(EstadoSala estado) {
        return Sala.builder().id(1L).nombre("Sala A").tipo(TipoSala.REUNION)
                .capacidadMaxima(4).estado(estado).build();
    }

    private Usuario cliente() {
        return Usuario.builder().id(7L).nombre("Cli").correo("cli@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.CLIENTE).build();
    }

    // ---------- buscar ----------
    @Test
    @DisplayName("buscar sin correo responde 400")
    void buscar_sinCorreo() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.buscar(null, null, request).getStatusCode());
    }

    @Test
    @DisplayName("buscar encuentra reservas del día")
    void buscar_hoy() {
        Reserva r = Reserva.builder().id(1L).sala(sala(EstadoSala.OCUPADA)).cliente(cliente())
                .fecha(LocalDate.now()).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepo.findReservasDelDiaPorCorreoCliente(eq("cli@swgec.ec"), any(LocalDate.class)))
                .thenReturn(List.of(r));

        ResponseEntity<Object> resp = controller.buscar("CLI@swgec.ec", null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("buscar sin resultados devuelve lista vacía 200")
    void buscar_vacio() {
        when(reservaRepo.findReservasDelDiaPorCorreoCliente(anyString(), any(LocalDate.class)))
                .thenReturn(List.of());
        ResponseEntity<Object> resp = controller.buscar("nadie@swgec.ec", null, request);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(((List<?>) resp.getBody()).isEmpty());
    }

    // ---------- check-in ----------
    @Test
    @DisplayName("registrarIngreso de reserva inexistente responde 404")
    void ingreso_notFound() {
        when(reservaRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.registrarIngreso(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("registrarIngreso fuera de reglas (finalizada) responde 409")
    void ingreso_conflicto() {
        Reserva r = Reserva.builder().id(1L).sala(sala(EstadoSala.DISPONIBLE)).cliente(cliente())
                .fecha(LocalDate.now()).horaInicio(LocalTime.MIN).horaFin(LocalTime.MAX)
                .estado(EstadoReserva.FINALIZADA).build();
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(r));
        assertEquals(HttpStatus.CONFLICT, controller.registrarIngreso(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("registrarIngreso válido responde 200 y deja la sala OCUPADA")
    void ingreso_ok() {
        Sala s = sala(EstadoSala.DISPONIBLE);
        Reserva r = Reserva.builder().id(1L).sala(s).cliente(cliente())
                .fecha(LocalDate.now()).horaInicio(LocalTime.MIN).horaFin(LocalTime.MAX)
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(r));

        ResponseEntity<Object> resp = controller.registrarIngreso(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(EstadoReserva.EN_USO, r.getEstado());
        assertEquals(EstadoSala.OCUPADA, s.getEstado());
        verify(reservaRepo).save(r);
    }

    // ---------- check-out ----------
    @Test
    @DisplayName("registrarSalida de reserva inexistente responde 404")
    void salida_notFound() {
        when(reservaRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.registrarSalida(1L, false, request).getStatusCode());
    }

    @Test
    @DisplayName("registrarSalida sin check-in previo responde 409")
    void salida_conflicto() {
        Reserva r = Reserva.builder().id(1L).sala(sala(EstadoSala.DISPONIBLE)).cliente(cliente())
                .fecha(LocalDate.now().minusDays(1)).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(r));
        assertEquals(HttpStatus.CONFLICT, controller.registrarSalida(1L, false, request).getStatusCode());
    }

    @Test
    @DisplayName("registrarSalida válido finaliza y libera la sala")
    void salida_ok() {
        Sala s = sala(EstadoSala.OCUPADA);
        Reserva r = Reserva.builder().id(1L).sala(s).cliente(cliente())
                .fecha(LocalDate.now().minusDays(1)).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.EN_USO).build();
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(r));

        ResponseEntity<Object> resp = controller.registrarSalida(1L, false, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
        assertEquals(EstadoSala.DISPONIBLE, s.getEstado());
    }

    @Test
    @DisplayName("registrarSalida con limpieza deja la sala EN_LIMPIEZA")
    void salida_conLimpieza() {
        Sala s = sala(EstadoSala.OCUPADA);
        Reserva r = Reserva.builder().id(1L).sala(s).cliente(cliente())
                .fecha(LocalDate.now().minusDays(1)).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.EN_USO).build();
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(r));

        controller.registrarSalida(1L, true, request);

        assertEquals(EstadoSala.EN_LIMPIEZA, s.getEstado());
    }

    // ---------- listados ----------
    @Test
    @DisplayName("salasEnUso responde 200")
    void salasEnUso() {
        when(reservaRepo.findTodasEnUso()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.salasEnUso().getStatusCode());
    }

    @Test
    @DisplayName("accesos deriva eventos CHECK_IN y CHECK_OUT de las reservas")
    void accesos() {
        Reserva r = Reserva.builder().id(1L).sala(sala(EstadoSala.DISPONIBLE)).cliente(cliente())
                .fecha(LocalDate.now()).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.FINALIZADA)
                .fechaIngreso(LocalDateTime.now().minusHours(2))
                .fechaSalida(LocalDateTime.now().minusHours(1))
                .build();
        when(reservaRepo.findConAccesos()).thenReturn(List.of(r));

        List<AccesoResponse> body = controller.accesos().getBody();

        assertEquals(2, body.size(), "Una reserva con ingreso y salida produce 2 eventos");
    }

    @Test
    @DisplayName("panelSalas responde 200 con el estado efectivo")
    void panelSalas() {
        when(reservaRepo.findActivasHoy(any(LocalDate.class))).thenReturn(List.of());
        when(salaRepository.findAll()).thenReturn(List.of(sala(EstadoSala.DISPONIBLE)));
        List<PanelSalaResponse> body = controller.panelSalas().getBody();
        assertEquals(1, body.size());
        assertEquals("DISPONIBLE", body.get(0).estadoPanel());
    }

    // ---------- reservasSalaHoy / marcarDisponible ----------
    @Test
    @DisplayName("reservasSalaHoy con sala inexistente responde 404")
    void reservasSalaHoy_notFound() {
        when(salaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.reservasSalaHoy(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("reservasSalaHoy con sala existente responde 200")
    void reservasSalaHoy_ok() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(EstadoSala.DISPONIBLE)));
        when(reservaRepo.findReservasDetalladasPorSalaYFecha(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.reservasSalaHoy(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("marcarDisponible sobre sala inexistente responde 404")
    void marcarDisponible_notFound() {
        when(salaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.marcarDisponible(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("marcarDisponible sobre sala que no está en limpieza responde 409")
    void marcarDisponible_conflicto() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(EstadoSala.DISPONIBLE)));
        assertEquals(HttpStatus.CONFLICT, controller.marcarDisponible(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("marcarDisponible libera una sala EN_LIMPIEZA y responde 200")
    void marcarDisponible_ok() {
        Sala s = sala(EstadoSala.EN_LIMPIEZA);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));

        ResponseEntity<Object> resp = controller.marcarDisponible(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(EstadoSala.DISPONIBLE, s.getEstado());
        verify(salaRepository).save(s);
    }
}
