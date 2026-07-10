package com.cleancodecrew.sweg.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de validación de Reserva (HU04) y cancelación (HU06).
 * Cubre rango horario, fecha pasada, campos obligatorios, solapamiento y cancelación,
 * combinando happy paths y edge cases.
 */
class ReservaValidacionTest {

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    private Sala sala(long id) {
        return Sala.builder().id(id).nombre("Sala " + id).tipo(TipoSala.REUNION).capacidadMaxima(4).build();
    }

    private Usuario cliente(long id) {
        return Usuario.builder().id(id).nombre("Cli").correo("c" + id + "@swgec.ec")
                .contrasenaHash("salt:digest").rol(Rol.CLIENTE).build();
    }

    private Reserva reserva(long salaId, LocalDate fecha, LocalTime hi, LocalTime hf) {
        return Reserva.builder()
                .sala(sala(salaId)).cliente(cliente(1))
                .fecha(fecha).horaInicio(hi).horaFin(hf)
                .estado(EstadoReserva.CONFIRMADA).build();
    }

    // --- Rango horario (CA-HU04-03) ---
    @Test
    @DisplayName("Rango válido (inicio < fin) no lanza excepción")
    void rango_valido() {
        Reserva r = reserva(1, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertDoesNotThrow(r::validarRangoHorario);
    }

    @Test
    @DisplayName("Inicio igual o posterior al fin es rechazado (edge case)")
    void rango_invalido() {
        Reserva igual = reserva(1, MANANA, LocalTime.of(9, 0), LocalTime.of(9, 0));
        assertThrows(IllegalArgumentException.class, igual::validarRangoHorario);
        Reserva invertido = reserva(1, MANANA, LocalTime.of(11, 0), LocalTime.of(10, 0));
        assertThrows(IllegalArgumentException.class, invertido::validarRangoHorario);
    }

    // --- Fecha pasada (CA-HU04-05) ---
    @Test
    @DisplayName("Fecha pasada es rechazada; hoy es aceptada")
    void fecha_pasada() {
        Reserva pasada = reserva(1, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThrows(IllegalArgumentException.class, pasada::validarFechaNoPasada);

        Reserva hoy = reserva(1, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertDoesNotThrow(hoy::validarFechaNoPasada);
    }

    // --- Campos obligatorios (CA-HU04-04) ---
    @Test
    @DisplayName("Falta de un campo obligatorio (sala) es rechazada")
    void campos_obligatorios() {
        Reserva sinSala = Reserva.builder().cliente(cliente(1)).fecha(MANANA)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0)).build();
        assertThrows(IllegalArgumentException.class, sinSala::validarCamposObligatorios);
    }

    @Test
    @DisplayName("validarTodo pasa para una reserva completa y futura (happy path)")
    void validarTodo_ok() {
        Reserva r = reserva(1, MANANA, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertDoesNotThrow(r::validarTodo);
    }

    // --- Solapamiento (CA-HU04-02) ---
    @Test
    @DisplayName("Misma sala y fecha con horas superpuestas se solapan")
    void solapamiento_true() {
        Reserva a = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reserva b = reserva(1, MANANA, LocalTime.of(10, 30), LocalTime.of(11, 30));
        assertTrue(a.seSolapaCon(b));
    }

    @Test
    @DisplayName("Reservas contiguas (fin == inicio) no se solapan (edge case)")
    void solapamiento_contiguas() {
        Reserva a = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reserva b = reserva(1, MANANA, LocalTime.of(11, 0), LocalTime.of(12, 0));
        assertFalse(a.seSolapaCon(b));
    }

    @Test
    @DisplayName("Reservas en salas distintas no se solapan")
    void solapamiento_salasDistintas() {
        Reserva a = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reserva b = reserva(2, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertFalse(a.seSolapaCon(b));
    }

    @Test
    @DisplayName("Una reserva cancelada nunca se solapa")
    void solapamiento_cancelada() {
        Reserva a = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reserva b = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        b.setEstado(EstadoReserva.CANCELADA);
        assertFalse(a.seSolapaCon(b));
    }

    @Test
    @DisplayName("seSolapaCon(null) es falso")
    void solapamiento_null() {
        Reserva a = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertFalse(a.seSolapaCon(null));
    }

    // --- Cancelación (CA-HU06-01) ---
    @Test
    @DisplayName("El dueño puede cancelar una reserva confirmada (happy path)")
    void cancelar_ok() {
        Reserva r = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        r.cancelar(cliente(1));
        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertNotNull(r.getFechaCancelacion());
    }

    @Test
    @DisplayName("Un usuario que no es el dueño no puede cancelar (edge case)")
    void cancelar_noDueno() {
        Reserva r = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertThrows(IllegalStateException.class, () -> r.cancelar(cliente(99)));
    }

    @Test
    @DisplayName("No se puede cancelar una reserva ya cancelada (edge case)")
    void cancelar_yaCancelada() {
        Reserva r = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        r.cancelar(cliente(1));
        assertThrows(IllegalStateException.class, () -> r.cancelar(cliente(1)));
    }

    @Test
    @DisplayName("No se puede cancelar una reserva en uso (edge case)")
    void cancelar_enUso() {
        Reserva r = reserva(1, MANANA, LocalTime.of(10, 0), LocalTime.of(11, 0));
        r.setEstado(EstadoReserva.EN_USO);
        assertThrows(IllegalStateException.class, () -> r.cancelar(cliente(1)));
    }
}
