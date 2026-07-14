package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.config.AuthInterceptor;
import com.cleancodecrew.sweg.dto.DisponibilidadResponse;
import com.cleancodecrew.sweg.dto.ReservaRequest;
import com.cleancodecrew.sweg.model.*;
import com.cleancodecrew.sweg.repository.HorarioReglaRepository;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del ReservaController (HU3 búsqueda, HU4 crear, HU5 disponibilidad, HU6 cancelar,
 * edición). Repositorios y sesión mockeados; sin contexto Spring ni base de datos.
 */
class ReservaControllerTest {

    private SalaRepository salaRepository;
    private ReservaRepository reservaRepository;
    private UsuarioRepository usuarioRepository;
    private HorarioReglaRepository horarioReglaRepository;
    private ReservaController controller;
    private HttpServletRequest request;
    private HttpSession session;

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        salaRepository = mock(SalaRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        horarioReglaRepository = mock(HorarioReglaRepository.class);
        controller = new ReservaController(salaRepository, reservaRepository,
                usuarioRepository, horarioReglaRepository);
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
    }

    private void sesion(Long userId) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(userId);
    }

    private Sala sala(long id, EstadoSala estado) {
        return Sala.builder().id(id).nombre("Sala " + id).tipo(TipoSala.REUNION)
                .capacidadMaxima(4).estado(estado).build();
    }

    private Usuario cliente(long id) {
        return Usuario.builder().id(id).nombre("Cliente").correo("c" + id + "@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.CLIENTE).build();
    }

    private ReservaRequest reservaReq(long salaId, LocalTime hi, LocalTime hf, int personas) {
        ReservaRequest r = new ReservaRequest();
        r.setSalaId(salaId);
        r.setFecha(MANANA);
        r.setHoraInicio(hi);
        r.setHoraFin(hf);
        r.setCantidadPersonas(personas);
        return r;
    }

    // ---------- salas disponibles / reservables ----------
    @Test
    @DisplayName("salasDisponibles devuelve solo las DISPONIBLES")
    void salasDisponibles() {
        when(salaRepository.findAll()).thenReturn(List.of(
                sala(1, EstadoSala.DISPONIBLE), sala(2, EstadoSala.OCUPADA)));
        List<Sala> body = controller.salasDisponibles().getBody();
        assertEquals(1, body.size());
    }

    @Test
    @DisplayName("salasReservables filtra por tipo válido e incluye DISPONIBLE y OCUPADA")
    void salasReservables_conTipo() {
        when(salaRepository.findAll()).thenReturn(List.of(
                sala(1, EstadoSala.DISPONIBLE), sala(2, EstadoSala.OCUPADA),
                sala(3, EstadoSala.EN_LIMPIEZA)));
        List<Sala> body = controller.salasReservables("REUNION").getBody();
        assertEquals(2, body.size());
    }

    @Test
    @DisplayName("salasReservables con tipo inválido ignora el filtro")
    void salasReservables_tipoInvalido() {
        when(salaRepository.findAll()).thenReturn(List.of(sala(1, EstadoSala.DISPONIBLE)));
        assertEquals(1, controller.salasReservables("NO_EXISTE").getBody().size());
    }

    // ---------- buscarSalas ----------
    @Test
    @DisplayName("buscarSalas sin fecha responde 400")
    void buscarSalas_sinFecha() {
        ResponseEntity<Object> resp = controller.buscarSalas(null, null, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("buscarSalas con fecha pasada responde 400")
    void buscarSalas_fechaPasada() {
        ResponseEntity<Object> resp = controller.buscarSalas(
                null, LocalDate.now().minusDays(1), null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("buscarSalas con rango horario incompleto responde 400")
    void buscarSalas_rangoIncompleto() {
        ResponseEntity<Object> resp = controller.buscarSalas(
                null, MANANA, LocalTime.of(9, 0), null, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("buscarSalas con rango invertido responde 400")
    void buscarSalas_rangoInvalido() {
        ResponseEntity<Object> resp = controller.buscarSalas(
                null, MANANA, LocalTime.of(11, 0), LocalTime.of(10, 0), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("buscarSalas con horario excluye las salas ocupadas en ese intervalo")
    void buscarSalas_conHorario() {
        when(reservaRepository.findSalaIdsConReservaEnHorario(MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(List.of(2L));
        when(salaRepository.findAll()).thenReturn(List.of(
                sala(1, EstadoSala.DISPONIBLE), sala(2, EstadoSala.DISPONIBLE)));

        ResponseEntity<Object> resp = controller.buscarSalas(
                null, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0), request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Sala> body = (List<Sala>) resp.getBody();
        assertEquals(1, body.size());
        assertEquals(1L, body.get(0).getId());
    }

    @Test
    @DisplayName("buscarSalas sin horario devuelve todas las reservables")
    void buscarSalas_sinHorario() {
        when(salaRepository.findAll()).thenReturn(List.of(sala(1, EstadoSala.DISPONIBLE)));
        ResponseEntity<Object> resp = controller.buscarSalas(null, MANANA, null, null, request);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ---------- misReservas ----------
    @Test
    @DisplayName("misReservas sin sesión responde 401")
    void misReservas_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.misReservas(0, 10, request).getStatusCode());
    }

    @Test
    @DisplayName("misReservas con sesión devuelve la página de reservas")
    void misReservas_ok() {
        sesion(1L);
        Page<Reserva> page = new PageImpl<>(List.of());
        when(reservaRepository.findByClienteIdOrderByFechaDescHoraInicioDesc(eq(1L), any()))
                .thenReturn(page);
        assertEquals(HttpStatus.OK, controller.misReservas(0, 10, request).getStatusCode());
    }

    // ---------- crear ----------
    @Test
    @DisplayName("crear sin sesión responde 401")
    void crear_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        ResponseEntity<Object> resp = controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    @DisplayName("crear con sala inexistente responde 404")
    void crear_salaNoEncontrada() {
        sesion(1L);
        when(salaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("crear con cliente inexistente responde 404")
    void crear_clienteNoEncontrado() {
        sesion(1L);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.DISPONIBLE)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("crear sobre sala EN_LIMPIEZA lanza ConflictoException")
    void crear_salaEnLimpieza() {
        sesion(1L);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.EN_LIMPIEZA)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        assertThrows(ConflictoException.class,
                () -> controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request));
    }

    @Test
    @DisplayName("crear con más personas que la capacidad lanza ConflictoException")
    void crear_excedeCapacidad() {
        sesion(1L);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.DISPONIBLE)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        assertThrows(ConflictoException.class,
                () -> controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 99), request));
    }

    @Test
    @DisplayName("crear fuera del horario comercial (07:00-22:00) lanza ConflictoException")
    void crear_fueraDeHorario() {
        sesion(1L);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.DISPONIBLE)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        assertThrows(ConflictoException.class,
                () -> controller.crear(reservaReq(1, LocalTime.of(6, 0), LocalTime.of(7, 0), 2), request));
    }

    @Test
    @DisplayName("crear con solapamiento lanza ConflictoException")
    void crear_solapamiento() {
        sesion(1L);
        Sala s = sala(1, EstadoSala.DISPONIBLE);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        Reserva existente = Reserva.builder().id(5L).sala(s).cliente(cliente(1))
                .fecha(MANANA).horaInicio(LocalTime.of(9, 30)).horaFin(LocalTime.of(10, 30))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findBySalaIdAndFechaAndEstadoNot(1L, MANANA, EstadoReserva.CANCELADA))
                .thenReturn(List.of(existente));

        assertThrows(ConflictoException.class,
                () -> controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request));
    }

    @Test
    @DisplayName("crear válido persiste y responde 201")
    void crear_ok() {
        sesion(1L);
        Sala s = sala(1, EstadoSala.DISPONIBLE);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        when(reservaRepository.findBySalaIdAndFechaAndEstadoNot(1L, MANANA, EstadoReserva.CANCELADA))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva rr = i.getArgument(0);
            if (rr.getEstado() == null) rr.setEstado(EstadoReserva.CONFIRMADA); // simula @PrePersist
            return rr;
        });

        ResponseEntity<Object> resp = controller.crear(reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(reservaRepository).save(any(Reserva.class));
    }

    // ---------- disponibilidad ----------
    @Test
    @DisplayName("disponibilidad con campos faltantes responde 400")
    void disponibilidad_camposFaltantes() {
        ResponseEntity<Object> resp = controller.disponibilidad(null, null, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("disponibilidad con rango inválido responde 400")
    void disponibilidad_rangoInvalido() {
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, MANANA, LocalTime.of(10, 0), LocalTime.of(9, 0), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("disponibilidad con fecha pasada responde 400")
    void disponibilidad_fechaPasada() {
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("disponibilidad con sala inexistente responde 404")
    void disponibilidad_salaNoEncontrada() {
        when(salaRepository.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0), request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @DisplayName("disponibilidad de sala no DISPONIBLE devuelve disponible=false")
    void disponibilidad_salaNoDisponible() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.MANTENIMIENTO)));
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0), request);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(((DisponibilidadResponse) resp.getBody()).disponible());
    }

    @Test
    @DisplayName("disponibilidad libre devuelve disponible=true")
    void disponibilidad_libre() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.DISPONIBLE)));
        when(reservaRepository.findActivasPorSalaYFecha(1L, MANANA)).thenReturn(List.of());
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0), request);
        assertTrue(((DisponibilidadResponse) resp.getBody()).disponible());
    }

    @Test
    @DisplayName("disponibilidad con solapamiento devuelve disponible=false")
    void disponibilidad_ocupada() {
        Sala s = sala(1, EstadoSala.DISPONIBLE);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));
        Reserva activa = Reserva.builder().sala(s).fecha(MANANA)
                .horaInicio(LocalTime.of(9, 30)).horaFin(LocalTime.of(10, 30))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findActivasPorSalaYFecha(1L, MANANA)).thenReturn(List.of(activa));
        ResponseEntity<Object> resp = controller.disponibilidad(
                1L, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0), request);
        assertFalse(((DisponibilidadResponse) resp.getBody()).disponible());
    }

    // ---------- horarioDia / reglasActivas ----------
    @Test
    @DisplayName("horarioDia con sala inexistente responde 404")
    void horarioDia_salaNoEncontrada() {
        when(salaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.horarioDia(1L, MANANA).getStatusCode());
    }

    @Test
    @DisplayName("horarioDia con sala existente responde 200")
    void horarioDia_ok() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, EstadoSala.DISPONIBLE)));
        when(reservaRepository.findOcupadasPorSalaYFecha(1L, MANANA)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.horarioDia(1L, MANANA).getStatusCode());
    }

    @Test
    @DisplayName("reglasActivas responde 200 con la lista")
    void reglasActivas_ok() {
        when(horarioReglaRepository.findByActivoTrue()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.reglasActivas().getStatusCode());
    }

    // ---------- editar ----------
    @Test
    @DisplayName("editar sin sesión responde 401")
    void editar_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.editar(1L, reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("editar reserva inexistente responde 404")
    void editar_notFound() {
        sesion(1L);
        when(reservaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.editar(1L, reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("editar reserva de otro cliente responde 403")
    void editar_noDueno() {
        sesion(1L);
        Reserva r = Reserva.builder().id(1L).sala(sala(1, EstadoSala.DISPONIBLE)).cliente(cliente(99))
                .fecha(MANANA).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));
        assertEquals(HttpStatus.FORBIDDEN,
                controller.editar(1L, reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("editar reserva en estado no editable responde 409")
    void editar_estadoNoEditable() {
        sesion(1L);
        Reserva r = Reserva.builder().id(1L).sala(sala(1, EstadoSala.DISPONIBLE)).cliente(cliente(1))
                .fecha(MANANA).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.EN_USO).build();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));
        assertEquals(HttpStatus.CONFLICT,
                controller.editar(1L, reservaReq(1, LocalTime.of(9, 0), LocalTime.of(10, 0), 2), request).getStatusCode());
    }

    @Test
    @DisplayName("editar válido actualiza y responde 200")
    void editar_ok() {
        sesion(1L);
        Sala s = sala(1, EstadoSala.DISPONIBLE);
        Reserva r = Reserva.builder().id(1L).sala(s).cliente(cliente(1))
                .fecha(MANANA).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));
        when(reservaRepository.findOtrasActivasPorSalaYFecha(1L, MANANA, 1L)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.editar(1L, reservaReq(1, LocalTime.of(11, 0), LocalTime.of(12, 0), 3), request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(LocalTime.of(11, 0), r.getHoraInicio());
    }

    // ---------- cancelar ----------
    @Test
    @DisplayName("cancelar sin sesión responde 401")
    void cancelar_sinSesion() {
        when(request.getSession(false)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.cancelar(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("cancelar con cliente inexistente responde 404")
    void cancelar_clienteNoEncontrado() {
        sesion(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.cancelar(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("cancelar con reserva inexistente responde 404")
    void cancelar_reservaNoEncontrada() {
        sesion(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        when(reservaRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.cancelar(1L, request).getStatusCode());
    }

    @Test
    @DisplayName("cancelar válido responde 200 y marca la reserva CANCELADA")
    void cancelar_ok() {
        sesion(1L);
        Reserva r = Reserva.builder().id(1L).sala(sala(1, EstadoSala.DISPONIBLE)).cliente(cliente(1))
                .fecha(MANANA).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .estado(EstadoReserva.CONFIRMADA).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1)));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.cancelar(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
    }
}
