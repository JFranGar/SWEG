package com.cleancodecrew.sweg.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta agregada del Dashboard del Administrador (HU Dashboard).
 * Reúne indicadores para tarjetas y series listas para graficar.
 * Todos los valores se calculan con datos reales del sistema (sin datos quemados).
 */
public record DashboardResponse(
        // ── Tarjetas: usuarios ──
        long totalUsuarios,
        long clientesActivos,
        long recepcionistasActivos,
        long administradoresActivos,

        // ── Tarjetas: reservas ──
        long reservasCreadasHoy,
        long reservasPendientes,
        long reservasConfirmadas,
        long reservasEnUso,
        long reservasCanceladas,
        long reservasFinalizadas,

        // ── Tarjetas: accesos del día ──
        long checkinsHoy,
        long checkoutsHoy,

        // ── Tarjetas: espacios ──
        long totalSalas,
        long espaciosDisponibles,
        long espaciosOcupados,
        long espaciosReservados,

        // ── Gráficas ──
        List<Punto> reservasPorDia,            // últimos 7 días (por fecha de reserva)
        List<Punto> reservasPorEstado,         // distribución por estado
        List<Punto> usoPorTipoSala,            // reservas por tipo de sala
        List<PuntoAcceso> accesosPorDia,       // check-ins/check-outs últimos 7 días
        List<Punto> topClientes,               // clientes con mayor actividad
        List<Punto> ocupacionPorHorario,       // reservas por hora de inicio (demanda horaria)

        // ── Actividad reciente ──
        List<Actividad> actividadReciente
) {
    /** Par etiqueta-valor genérico para una serie de gráfica. */
    public record Punto(String etiqueta, long valor) {}

    /** Punto con dos series (check-ins y check-outs) por fecha. */
    public record PuntoAcceso(String etiqueta, long checkins, long checkouts) {}

    /** Evento reciente para el feed de actividad del sistema. */
    public record Actividad(String tipo, String descripcion, LocalDateTime fecha) {}
}
