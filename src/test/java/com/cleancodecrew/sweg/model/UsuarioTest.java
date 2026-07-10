package com.cleancodecrew.sweg.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del bloqueo temporal de cuenta tras intentos fallidos (HU01 - CA3).
 */
class UsuarioTest {

    private Usuario nuevoUsuario() {
        return Usuario.builder()
                .nombre("Demo")
                .correo("demo@swgec.ec")
                .contrasenaHash("salt:digest")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("Un usuario recién creado no está bloqueado")
    void nuevo_noBloqueado() {
        assertFalse(nuevoUsuario().estaBloqueado());
        assertEquals(0L, nuevoUsuario().minutosRestantesDeBloqueo());
    }

    @Test
    @DisplayName("Menos de MAX_INTENTOS_FALLIDOS no bloquea la cuenta")
    void intentosInsuficientes_noBloquea() {
        Usuario u = nuevoUsuario();
        u.registrarIntentoFallido();
        u.registrarIntentoFallido(); // 2 < 3
        assertFalse(u.estaBloqueado());
    }

    @Test
    @DisplayName("Al alcanzar MAX_INTENTOS_FALLIDOS la cuenta se bloquea y el contador se reinicia")
    void alcanzaLimite_bloquea() {
        Usuario u = nuevoUsuario();
        for (int i = 0; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            u.registrarIntentoFallido();
        }
        assertTrue(u.estaBloqueado());
        assertEquals(0, u.getIntentosFallidos(), "El contador se reinicia tras bloquear");
        long restantes = u.minutosRestantesDeBloqueo();
        assertTrue(restantes > 0 && restantes <= Usuario.MINUTOS_BLOQUEO + 1,
                "Los minutos restantes deben estar dentro del rango de bloqueo");
    }

    @Test
    @DisplayName("Un login exitoso reinicia intentos y desbloquea la cuenta")
    void loginExitoso_resetea() {
        Usuario u = nuevoUsuario();
        for (int i = 0; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            u.registrarIntentoFallido();
        }
        assertTrue(u.estaBloqueado());

        u.registrarLoginExitoso();

        assertFalse(u.estaBloqueado());
        assertEquals(0, u.getIntentosFallidos());
        assertEquals(0L, u.minutosRestantesDeBloqueo());
        assertNotNull(u.getUltimoLogin());
    }
}
