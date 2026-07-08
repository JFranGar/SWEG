package com.cleancodecrew.sweg.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de prueba del flujo Check-in / Check-out (HU11).
 *
 * Reglas verificadas:
 *  - El check-in solo se permite dentro del rango [horaInicio, horaFin] y NO finaliza la reunión.
 *  - El check-out solo se permite cuando termina el rango horario (ahora >= horaFin).
 *  - Check-in y check-out no comparten lógica: cada uno cambia un estado distinto.
 *  - No se permite doble check-in ni doble check-out, ni check-out sin check-in previo.
 *
 * Reserva de referencia: 10:00 – 11:00 el 2026-07-07.
 */
class ReservaCheckInOutTest {

    private static final LocalDate DIA   = LocalDate.of(2026, 7, 7);
    private static final LocalTime H_INI = LocalTime.of(10, 0);
    private static final LocalTime H_FIN = LocalTime.of(11, 0);

    private Reserva reserva(EstadoReserva estado) {
        Sala sala = Sala.builder()
                .nombre("Sala A")
                .tipo(TipoSala.REUNION)
                .capacidadMaxima(4)
                .estado(EstadoSala.DISPONIBLE)
                .build();
        return Reserva.builder()
                .sala(sala)
                .fecha(DIA)
                .horaInicio(H_INI)
                .horaFin(H_FIN)
                .estado(estado)
                .build();
    }

    private LocalDateTime a(int hora, int minuto) {
        return LocalDateTime.of(DIA, LocalTime.of(hora, minuto));
    }

    // 1) 09:50 -> no permite check-in ni check-out.
    @Test
    @DisplayName("Antes de la hora de inicio no se permite check-in ni check-out")
    void antesDeInicio_bloquea() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);

        IllegalStateException in = assertThrows(IllegalStateException.class,
                () -> r.registrarIngreso(a(9, 50), null));
        assertTrue(in.getMessage().contains("La reunión inicia a las 10:00"));
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado(), "El check-in fallido no cambia el estado");

        // Sin check-in previo tampoco puede haber check-out.
        assertThrows(IllegalStateException.class, () -> r.registrarSalida(false, a(9, 50), null));
    }

    // 2) 10:00 -> permite check-in y NO finaliza la reunión.
    @Test
    @DisplayName("A la hora de inicio el check-in pasa a EN_USO y no finaliza")
    void enInicio_checkInNoFinaliza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);

        r.registrarIngreso(a(10, 0), null);

        assertEquals(EstadoReserva.EN_USO, r.getEstado());
        assertNotEquals(EstadoReserva.FINALIZADA, r.getEstado(), "El check-in NUNCA finaliza la reunión");
        assertNotNull(r.getFechaIngreso());
        assertNull(r.getFechaSalida(), "El check-in no registra salida");
        assertEquals(EstadoSala.OCUPADA, r.getSala().getEstado());
    }

    // 3) 10:30 -> permite check-in; no permite check-out todavía.
    @Test
    @DisplayName("Durante el rango: check-in válido, pero check-out bloqueado hasta el fin")
    void durante_checkInSiCheckOutNo() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);

        r.registrarIngreso(a(10, 30), null);
        assertEquals(EstadoReserva.EN_USO, r.getEstado());

        IllegalStateException out = assertThrows(IllegalStateException.class,
                () -> r.registrarSalida(false, a(10, 30), null));
        assertTrue(out.getMessage().contains("La reunión termina a las 11:00"));
        assertEquals(EstadoReserva.EN_USO, r.getEstado(), "El check-out fallido no cambia el estado");
    }

    // 4) 11:00 -> permite check-out y finaliza correctamente.
    @Test
    @DisplayName("Al terminar el rango, el check-out finaliza y libera la sala")
    void enFin_checkOutFinaliza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        r.registrarIngreso(a(10, 0), null);

        r.registrarSalida(false, a(11, 0), null);

        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
        assertNotNull(r.getFechaSalida());
        assertEquals(EstadoSala.DISPONIBLE, r.getSala().getEstado());
    }

    // 5) Check-out sin check-in previo -> rechazado.
    @Test
    @DisplayName("Check-out sin check-in previo no se permite")
    void checkOutSinCheckIn_rechaza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> r.registrarSalida(false, a(11, 0), null));
        assertTrue(ex.getMessage().contains("no tiene check-in previo"));
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    // 6) Check-in en una reunión finalizada -> rechazado.
    @Test
    @DisplayName("No se permite check-in en una reserva finalizada")
    void checkInEnFinalizada_rechaza() {
        Reserva r = reserva(EstadoReserva.FINALIZADA);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> r.registrarIngreso(a(10, 0), null));
        assertTrue(ex.getMessage().contains("ya finalizó"));
    }

    // 7) Doble check-in -> no duplica el registro de entrada.
    @Test
    @DisplayName("El segundo check-in es rechazado (no hay doble entrada)")
    void dobleCheckIn_rechaza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        r.registrarIngreso(a(10, 0), null);
        LocalDateTime primeraEntrada = r.getFechaIngreso();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> r.registrarIngreso(a(10, 15), null));
        assertTrue(ex.getMessage().contains("ya tiene un check-in"));
        assertEquals(primeraEntrada, r.getFechaIngreso(), "La entrada original no se sobrescribe");
    }

    // 8) Doble check-out -> no duplica ni altera estados.
    @Test
    @DisplayName("El segundo check-out es rechazado (no hay doble salida)")
    void dobleCheckOut_rechaza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        r.registrarIngreso(a(10, 0), null);
        r.registrarSalida(false, a(11, 0), null);
        LocalDateTime primeraSalida = r.getFechaSalida();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> r.registrarSalida(false, a(11, 30), null));
        assertTrue(ex.getMessage().contains("ya finalizó"));
        assertEquals(primeraSalida, r.getFechaSalida(), "La salida original no se sobrescribe");
        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
    }

    // Extra) Check-in y check-out no comparten lógica: cada acción cambia solo su estado.
    @Test
    @DisplayName("Check-in y check-out son operaciones independientes")
    void checkInYCheckOut_noCompartenLogica() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);

        r.registrarIngreso(a(10, 0), null);
        assertEquals(EstadoReserva.EN_USO, r.getEstado());
        assertNull(r.getFechaSalida(), "El check-in no ejecuta la lógica de check-out");

        r.registrarSalida(false, a(11, 0), null);
        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
        assertNotNull(r.getFechaIngreso(), "El check-out conserva la entrada registrada por el check-in");
    }
}
