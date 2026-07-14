package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Sala;

import java.util.List;
import java.util.Set;

public record PanelSalaResponse(
        Long id,
        String nombre,
        String tipo,
        int capacidadMaxima,
        String estadoPanel
) {

    /**
     * CA-HU09-01: Estado efectivo de una sala para el panel.
     * OCUPADA→EN_USO, EN_LIMPIEZA/MANTENIMIENTO tal cual y, para las DISPONIBLES,
     * RESERVADA si tiene reserva activa hoy o DISPONIBLE en caso contrario.
     *
     * Centraliza la regla que antes estaba duplicada en SalaController y RecepcionController.
     */
    public static PanelSalaResponse deSala(Sala s, Set<Long> salasConReservaHoy) {
        String estadoPanel = switch (s.getEstado()) {
            case OCUPADA       -> "EN_USO";
            case EN_LIMPIEZA   -> "EN_LIMPIEZA";
            case MANTENIMIENTO -> "MANTENIMIENTO";
            default            -> salasConReservaHoy.contains(s.getId()) ? "RESERVADA" : "DISPONIBLE";
        };
        return new PanelSalaResponse(s.getId(), s.getNombre(), s.getTipo().name(), s.getCapacidadMaxima(), estadoPanel);
    }

    /**
     * Construye el panel completo: descarta salas eliminadas, calcula el estado efectivo
     * de cada una y las ordena alfabéticamente por nombre.
     */
    public static List<PanelSalaResponse> construirPanel(List<Sala> salas, Set<Long> salasConReservaHoy) {
        return salas.stream()
                .filter(s -> s.getEstado() != EstadoSala.ELIMINADA)
                .map(s -> deSala(s, salasConReservaHoy))
                .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
                .toList();
    }
}
