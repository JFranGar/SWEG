package com.cleancodecrew.sweg.dto;

public record PanelSalaResponse(
        Long id,
        String nombre,
        String tipo,
        int capacidadMaxima,
        String estadoPanel
) {}
