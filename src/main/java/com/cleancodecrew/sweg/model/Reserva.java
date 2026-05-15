package com.cleancodecrew.sweg.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad Reserva - HU4 Reservar Sala.
 *
 *  - HU4 CA1: reserva exitosa.
 *  - HU4 CA2: deteccion de solapamiento via {@link #seSolapaCon}.
 *  - HU4 CA3: rango horario coherente.
 *  - HU4 CA4: validacion de campos obligatorios.
 *  - HU4 CA5: rechazo de fechas pasadas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado = EstadoReserva.CONFIRMADA;

    private LocalDateTime creadaEn;

    public void validarCamposObligatorios() {
        if (sala == null || cliente == null || fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("Campos obligatorios ausentes");
        }
    }

    public void validarFechaNoPasada() {
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se permiten reservas en fechas pasadas");
        }
    }

    public void validarRangoHorario() {
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser menor a la hora de fin");
        }
    }

    public boolean seSolapaCon(Reserva otra) {
        if (otra == null) return false;
        if (this.estado == EstadoReserva.CANCELADA || otra.estado == EstadoReserva.CANCELADA) return false;
        if (this.sala == null || otra.sala == null) return false;
        Long thisSalaId = this.sala.getId();
        Long otraSalaId = otra.sala.getId();
        if (thisSalaId == null || otraSalaId == null) return false;
        if (!thisSalaId.equals(otraSalaId)) return false;
        if (!this.fecha.equals(otra.fecha)) return false;
        return this.horaInicio.isBefore(otra.horaFin) && otra.horaInicio.isBefore(this.horaFin);
    }

    public void validarTodo() {
        validarCamposObligatorios();
        validarFechaNoPasada();
        validarRangoHorario();
    }

    @PrePersist
    private void onCreate() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (estado == null) estado = EstadoReserva.CONFIRMADA;
    }
}

