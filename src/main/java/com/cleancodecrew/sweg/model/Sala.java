package com.cleancodecrew.sweg.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad Sala - HU2 Gestion de Salas.
 *
 *  - HU2 CA1: creacion (orquestada por el controller, validacion en la entidad).
 *  - HU2 CA3: modificacion via {@link #actualizarDatos}.
 *  - HU2 CA4: validacion de campos obligatorios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "salas")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 100, nullable = false, unique = true)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSala tipo;

    @Min(1)
    @Column(nullable = false)
    private int capacidadMaxima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoSala estado = EstadoSala.DISPONIBLE;

    public void validarCamposObligatorios() {
        if (nombre == null || nombre.isBlank() || tipo == null || capacidadMaxima <= 0) {
            throw new IllegalArgumentException("Todos los campos son obligatorios");
        }
    }

    public void actualizarDatos(String nombre, TipoSala tipo, int capacidadMaxima) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidadMaxima = capacidadMaxima;
        validarCamposObligatorios();
    }
}
