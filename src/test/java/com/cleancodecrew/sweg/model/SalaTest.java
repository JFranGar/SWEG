package com.cleancodecrew.sweg.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de validación y estados de Sala (HU02).
 *  - Happy path: sala completa válida; actualización correcta.
 *  - Edge cases: nombre en blanco, tipo nulo y capacidad no positiva.
 */
class SalaTest {

    private Sala salaValida() {
        return Sala.builder()
                .nombre("Sala A")
                .tipo(TipoSala.REUNION)
                .capacidadMaxima(4)
                .build();
    }

    @Test
    @DisplayName("Una sala con todos sus campos pasa la validación (happy path)")
    void validacion_ok() {
        assertDoesNotThrow(() -> salaValida().validarCamposObligatorios());
    }

    @Test
    @DisplayName("El estado por defecto de una sala nueva es DISPONIBLE")
    void estadoPorDefecto() {
        assertEquals(EstadoSala.DISPONIBLE, salaValida().getEstado());
    }

    @Test
    @DisplayName("Nombre en blanco es rechazado (edge case)")
    void nombreEnBlanco_rechaza() {
        Sala s = Sala.builder().nombre("   ").tipo(TipoSala.REUNION).capacidadMaxima(4).build();
        assertThrows(IllegalArgumentException.class, s::validarCamposObligatorios);
    }

    @Test
    @DisplayName("Tipo nulo es rechazado (edge case)")
    void tipoNulo_rechaza() {
        Sala s = Sala.builder().nombre("Sala A").tipo(null).capacidadMaxima(4).build();
        assertThrows(IllegalArgumentException.class, s::validarCamposObligatorios);
    }

    @Test
    @DisplayName("Capacidad menor o igual a cero es rechazada (edge case)")
    void capacidadInvalida_rechaza() {
        Sala s = Sala.builder().nombre("Sala A").tipo(TipoSala.REUNION).capacidadMaxima(0).build();
        assertThrows(IllegalArgumentException.class, s::validarCamposObligatorios);
    }

    @Test
    @DisplayName("actualizarDatos modifica los campos y revalida (happy path)")
    void actualizarDatos_ok() {
        Sala s = salaValida();
        s.actualizarDatos("Sala B", TipoSala.SEMINARIO, 10);
        assertEquals("Sala B", s.getNombre());
        assertEquals(TipoSala.SEMINARIO, s.getTipo());
        assertEquals(10, s.getCapacidadMaxima());
    }

    @Test
    @DisplayName("actualizarDatos con datos inválidos lanza excepción (edge case)")
    void actualizarDatos_invalido() {
        Sala s = salaValida();
        assertThrows(IllegalArgumentException.class,
                () -> s.actualizarDatos("", TipoSala.REUNION, 5));
    }

    @Test
    @DisplayName("eliminar() aplica borrado lógico (estado ELIMINADA)")
    void eliminar_logico() {
        Sala s = salaValida();
        s.eliminar();
        assertEquals(EstadoSala.ELIMINADA, s.getEstado());
    }
}
