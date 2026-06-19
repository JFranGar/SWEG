package com.cleancodecrew.sweg.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "horario_reglas")
public class HorarioRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRegla tipo;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** null = todos los días; 1=Lunes ... 7=Domingo (ISO 8601) */
    @Column(name = "dia_semana")
    private Integer diaSemana;

    @Column(length = 200)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
