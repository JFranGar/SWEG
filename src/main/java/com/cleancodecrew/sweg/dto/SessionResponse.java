package com.cleancodecrew.sweg.dto;

/**
 * SessionResponse
 *
 * Respuesta de sesión con datos básicos del usuario.
 */
public record SessionResponse(Long id, String nombre, String correo, String rol) {}
