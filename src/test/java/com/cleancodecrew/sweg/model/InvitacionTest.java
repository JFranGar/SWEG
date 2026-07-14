package com.cleancodecrew.sweg.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del modelo de dominio Invitacion (HU12 - RBAC híbrido).
 * Cubre la factory segura, la validez temporal, el consumo de un solo uso,
 * la revocación y el estado legible. Todo se hace sin base de datos.
 */
class InvitacionTest {

    private Usuario admin() {
        return Usuario.builder().id(1L).nombre("Admin").correo("admin@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.ADMIN).build();
    }

    private Usuario noAdmin() {
        return Usuario.builder().id(2L).nombre("Cli").correo("cli@swgec.ec")
                .contrasenaHash("s:d").rol(Rol.CLIENTE).build();
    }

    // --- Factory crear() ---
    @Test
    @DisplayName("crear() genera invitación válida con token, rol, correo normalizado y expiración")
    void crear_ok() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "  Nuevo@Correo.EC ", admin(), 24);
        assertNotNull(inv.getToken());
        assertEquals(Rol.RECEPCIONISTA, inv.getRol());
        assertEquals("nuevo@correo.ec", inv.getCorreo(), "El correo se normaliza a minúsculas y sin espacios");
        assertFalse(inv.isUsada());
        assertFalse(inv.isRevocada());
        assertTrue(inv.getExpiraEn().isAfter(inv.getCreadaEn()));
        assertTrue(inv.esValida(LocalDateTime.now()));
    }

    @Test
    @DisplayName("crear() con horas <= 0 aplica la validez por defecto (48h)")
    void crear_horasPorDefecto() {
        LocalDateTime antes = LocalDateTime.now();
        Invitacion inv = Invitacion.crear(Rol.ADMIN, null, admin(), 0);
        assertNull(inv.getCorreo(), "Correo en blanco/nulo queda como null");
        long horas = java.time.Duration.between(antes, inv.getExpiraEn()).toHours();
        assertTrue(horas >= Invitacion.HORAS_VALIDEZ_DEFECTO - 1, "Debe usar la validez por defecto");
    }

    @Test
    @DisplayName("crear() rechaza el rol CLIENTE y el rol nulo")
    void crear_rolInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Invitacion.crear(Rol.CLIENTE, "a@a.ec", admin(), 24));
        assertThrows(IllegalArgumentException.class, () -> Invitacion.crear(null, "a@a.ec", admin(), 24));
    }

    @Test
    @DisplayName("crear() exige un administrador activo como emisor")
    void crear_emisorInvalido() {
        assertThrows(IllegalStateException.class, () -> Invitacion.crear(Rol.ADMIN, "a@a.ec", null, 24));
        assertThrows(IllegalStateException.class, () -> Invitacion.crear(Rol.ADMIN, "a@a.ec", noAdmin(), 24));
    }

    @Test
    @DisplayName("generarToken() produce tokens no nulos y distintos")
    void generarToken_aleatorio() {
        String t1 = Invitacion.generarToken();
        String t2 = Invitacion.generarToken();
        assertNotNull(t1);
        assertFalse(t1.isBlank());
        assertNotEquals(t1, t2);
    }

    // --- Validez temporal ---
    @Test
    @DisplayName("estaExpirada() compara contra expiraEn")
    void estaExpirada() {
        Invitacion inv = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        assertTrue(inv.estaExpirada(inv.getExpiraEn().plusMinutes(1)));
        assertFalse(inv.estaExpirada(inv.getExpiraEn().minusMinutes(1)));
    }

    @Test
    @DisplayName("esValida() es falso si está usada, revocada o expirada")
    void esValida_estados() {
        Invitacion inv = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        assertTrue(inv.esValida(LocalDateTime.now()));

        Invitacion revocada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        revocada.revocar();
        assertFalse(revocada.esValida(LocalDateTime.now()));

        assertFalse(inv.esValida(inv.getExpiraEn().plusHours(1)), "Expirada no es válida");
    }

    // --- Consumo (un solo uso) ---
    @Test
    @DisplayName("marcarUsada() consume la invitación una única vez")
    void marcarUsada_ok() {
        Invitacion inv = Invitacion.crear(Rol.RECEPCIONISTA, "a@a.ec", admin(), 24);
        Usuario creado = noAdmin();
        LocalDateTime ahora = LocalDateTime.now();

        inv.marcarUsada(creado, ahora);

        assertTrue(inv.isUsada());
        assertEquals(ahora, inv.getUsadaEn());
        assertEquals(creado, inv.getUsuarioCreado());
        assertFalse(inv.esValida(ahora));
    }

    @Test
    @DisplayName("marcarUsada() falla si ya fue usada, revocada o expiró")
    void marcarUsada_rechazos() {
        Usuario creado = noAdmin();

        Invitacion usada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        usada.marcarUsada(creado, LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> usada.marcarUsada(creado, LocalDateTime.now()));

        Invitacion revocada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        revocada.revocar();
        assertThrows(IllegalStateException.class, () -> revocada.marcarUsada(creado, LocalDateTime.now()));

        Invitacion expira = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        LocalDateTime futuro = expira.getExpiraEn().plusHours(1);
        assertThrows(IllegalStateException.class, () -> expira.marcarUsada(creado, futuro));
    }

    // --- Revocación ---
    @Test
    @DisplayName("revocar() anula una invitación pendiente pero no una ya usada")
    void revocar() {
        Invitacion inv = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        inv.revocar();
        assertTrue(inv.isRevocada());

        Invitacion usada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        usada.marcarUsada(noAdmin(), LocalDateTime.now());
        assertThrows(IllegalStateException.class, usada::revocar);
    }

    // --- Estado legible ---
    @Test
    @DisplayName("estadoLegible() refleja REVOCADA, USADA, EXPIRADA o PENDIENTE")
    void estadoLegible() {
        LocalDateTime ahora = LocalDateTime.now();

        Invitacion pendiente = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        assertEquals("PENDIENTE", pendiente.estadoLegible(ahora));
        assertEquals("EXPIRADA", pendiente.estadoLegible(pendiente.getExpiraEn().plusHours(1)));

        Invitacion usada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        usada.marcarUsada(noAdmin(), ahora);
        assertEquals("USADA", usada.estadoLegible(ahora));

        Invitacion revocada = Invitacion.crear(Rol.ADMIN, "a@a.ec", admin(), 24);
        revocada.revocar();
        assertEquals("REVOCADA", revocada.estadoLegible(ahora));
    }
}
