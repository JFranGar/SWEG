package com.cleancodecrew.sweg.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del hashing de contraseñas (HU01 - CA2).
 *  - Happy path: una contraseña coincide con su propio hash.
 *  - Edge cases: contraseña incorrecta, hash mal formado y aleatoriedad de la sal.
 */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    @DisplayName("El hash tiene formato salt:digest y no expone la contraseña en claro")
    void hash_formato() {
        String hash = hasher.hash("secreto123");
        assertNotNull(hash);
        assertTrue(hash.contains(":"), "El hash debe tener el formato salt:digest");
        assertFalse(hash.contains("secreto123"), "El hash no debe contener la contraseña en claro");
    }

    @Test
    @DisplayName("matches() es verdadero para la contraseña correcta (happy path)")
    void matches_correcta() {
        String hash = hasher.hash("Contrasena#2026");
        assertTrue(hasher.matches("Contrasena#2026", hash));
    }

    @Test
    @DisplayName("matches() es falso para una contraseña incorrecta (edge case)")
    void matches_incorrecta() {
        String hash = hasher.hash("Contrasena#2026");
        assertFalse(hasher.matches("otra-clave", hash));
    }

    @Test
    @DisplayName("matches() es falso ante un hash mal formado (sin separador)")
    void matches_hashMalFormado() {
        assertFalse(hasher.matches("cualquier", "sin-separador"));
    }

    @Test
    @DisplayName("Dos hashes de la misma contraseña difieren por la sal aleatoria")
    void hash_saltAleatoria() {
        assertNotEquals(hasher.hash("misma"), hasher.hash("misma"));
    }
}
