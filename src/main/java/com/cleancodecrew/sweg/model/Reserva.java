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
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "fecha_ingreso")
    private LocalDateTime fechaIngreso;

    /** CA-HU04-04: Campos obligatorios. */
    public void validarCamposObligatorios() {
        if (sala == null || cliente == null || fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("Campos obligatorios ausentes");
        }
    }

    /** CA-HU04-05: No reservas en fechas pasadas. */
    public void validarFechaNoPasada() {
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se permiten reservas en fechas pasadas");
        }
    }

    /** CA-HU04-03: horaInicio < horaFin. */
    public void validarRangoHorario() {
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser menor a la hora de fin");
        }
    }

    /** CA-HU04-02: Detección de solapamiento. */
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

    /** CA-HU05-01: Construye una Reserva no persistida solo para chequear solapamiento. */
    public static Reserva deConsulta(Sala sala, LocalDate f, LocalTime hi, LocalTime hf) {
        Reserva r = new Reserva();
        r.sala = sala; r.fecha = f; r.horaInicio = hi; r.horaFin = hf;
        return r;
    }

    /** CA-HU06-01: Cancela la reserva si esta activa y pertenece al cliente. */
    public void cancelar(Usuario cliente) {
        if (!this.cliente.getId().equals(cliente.getId())) {
            throw new IllegalStateException("Solo el dueno de la reserva puede cancelarla");
        }
        if (this.estado == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("La reserva ya esta cancelada");
        }
        if (this.estado == EstadoReserva.EN_USO || this.estado == EstadoReserva.FINALIZADA) {
            throw new IllegalStateException("No se puede cancelar una reserva en uso o finalizada");
        }
        this.estado = EstadoReserva.CANCELADA;
        this.fechaCancelacion = LocalDateTime.now();
    }

    public static final int MINUTOS_TOLERANCIA_ENTRADA = 15;

    /** CA-HU07-03: Valida si la reserva esta dentro del rango para hacer check-in. */
    public void validarVentanaDeIngreso(LocalDateTime ahora) {
        LocalDateTime inicio = LocalDateTime.of(this.fecha, this.horaInicio);
        LocalDateTime fin    = LocalDateTime.of(this.fecha, this.horaFin);
        LocalDateTime ventanaInicio = inicio.minusMinutes(MINUTOS_TOLERANCIA_ENTRADA);

        if (ahora.isBefore(ventanaInicio)) {
            throw new IllegalStateException(
                "La sala aun no esta disponible para ocupacion. Reserva inicia a las " + this.horaInicio);
        }
        if (ahora.isAfter(fin)) {
            throw new IllegalStateException("La reserva ya finalizo");
        }
    }

    /** CA-HU07-02: Marca la reserva como EN_USO y actualiza la sala. */
    public void registrarIngreso(LocalDateTime ahora) {
        if (this.estado != EstadoReserva.CONFIRMADA && this.estado != EstadoReserva.PENDIENTE) {
            throw new IllegalStateException("Solo reservas activas pueden registrar ingreso");
        }
        validarVentanaDeIngreso(ahora);
        this.estado = EstadoReserva.EN_USO;
        this.fechaIngreso = ahora;
        this.sala.marcarEnUso();
    }

    @PrePersist
    private void onCreate() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (estado == null) estado = EstadoReserva.CONFIRMADA;
    }
}

