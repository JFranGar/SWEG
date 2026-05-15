package com.cleancodecrew.sweg.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Entidad Usuario - HU1 Inicio de Sesion.
 *
 * Rich Domain Model (ISO 25010 - Mantenibilidad):
 *  - HU1 CA1: rol diferenciado (campo {@link Rol}).
 *  - HU1 CA2: la verificacion de credenciales se delega al PasswordHasher,
 *             pero el manejo de intentos vive aqui.
 *  - HU1 CA3: bloqueo temporal por intentos fallidos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final int MAX_INTENTOS_FALLIDOS = 3;
    public static final int MINUTOS_BLOQUEO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 120, nullable = false)
    private String nombre;

    @NotBlank
    @Email
    @Column(length = 150, nullable = false, unique = true)
    private String correo;

    @NotBlank
    @Column(length = 255, nullable = false)
    private String contrasenaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    private LocalDateTime bloqueadoHasta;

    private LocalDateTime ultimoLogin;

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    public void registrarIntentoFallido() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            this.bloqueadoHasta = LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO);
            this.intentosFallidos = 0;
        }
    }

    public void registrarLoginExitoso() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
        this.ultimoLogin = LocalDateTime.now();
    }

    public long minutosRestantesDeBloqueo() {
        if (!estaBloqueado()) return 0L;
        long diff = ChronoUnit.MINUTES.between(LocalDateTime.now(), bloqueadoHasta);
        return diff + 1;
    }
}

