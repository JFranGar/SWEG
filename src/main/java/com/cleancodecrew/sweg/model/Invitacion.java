package com.cleancodecrew.sweg.model;

import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Entidad Invitacion - HU RBAC Hibrido (registro de cuentas internas).
 *
 * Modelo hibrido de registro:
 *  - Las cuentas CLIENTE se auto-registran (ver RegistroController).
 *  - Las cuentas RECEPCIONISTA y ADMIN solo pueden crearse mediante una
 *    invitacion segura emitida por un administrador activo.
 *
 * Rich Domain Model (ISO 25010 - Mantenibilidad / Seguridad):
 *  - CA-HU12-01: token seguro de un solo uso con expiracion.
 *  - CA-HU12-02: trazabilidad de quien la creo y cuando fue utilizada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invitaciones")
public class Invitacion {

    /** Longitud en bytes del token aleatorio (256 bits). */
    public static final int TOKEN_BYTES = 32;

    /** Horas de validez por defecto de una invitacion. */
    public static final int HORAS_VALIDEZ_DEFECTO = 48;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CA-HU12-01: token seguro e irrepetible que viaja en el enlace de invitacion. */
    @Column(length = 64, nullable = false, unique = true)
    private String token;

    /** Rol interno que se asignara a la cuenta creada (RECEPCIONISTA o ADMIN). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    /** Correo sugerido del invitado (opcional, informativo). */
    @Column(length = 150)
    private String correo;

    /** CA-HU12-02: administrador que emitio la invitacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por_id")
    private Usuario creadaPor;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** CA-HU12-01: momento de expiracion tras el cual la invitacion deja de ser valida. */
    @Column(nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private boolean usada = false;

    /** CA-HU12-02: momento exacto en que la invitacion fue consumida. */
    private LocalDateTime usadaEn;

    /** CA-HU12-02: cuenta interna resultante de completar la invitacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creado_id")
    private Usuario usuarioCreado;

    /** Permite que un administrador anule una invitacion pendiente. */
    @Column(nullable = false)
    @Builder.Default
    private boolean revocada = false;

    /**
     * CA-HU12-01: Factory de dominio que crea una invitacion con token seguro y expiracion.
     * Solo admite roles internos (RECEPCIONISTA o ADMIN); rechaza CLIENTE.
     */
    public static Invitacion crear(Rol rol, String correo, Usuario creador, int horasValidez) {
        if (rol == null || rol == Rol.CLIENTE) {
            throw new IllegalArgumentException("Solo se pueden invitar cuentas RECEPCIONISTA o ADMIN");
        }
        if (creador == null || creador.getRol() != Rol.ADMIN) {
            throw new IllegalStateException("Solo un administrador activo puede emitir invitaciones");
        }
        int horas = horasValidez > 0 ? horasValidez : HORAS_VALIDEZ_DEFECTO;
        LocalDateTime ahora = LocalDateTime.now();
        return Invitacion.builder()
                .token(generarToken())
                .rol(rol)
                .correo(correo != null && !correo.isBlank() ? correo.trim().toLowerCase() : null)
                .creadaPor(creador)
                .creadaEn(ahora)
                .expiraEn(ahora.plusHours(horas))
                .usada(false)
                .revocada(false)
                .build();
    }

    /** CA-HU12-01: genera un token aleatorio URL-safe de 256 bits. */
    public static String generarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean estaExpirada(LocalDateTime ahora) {
        return expiraEn != null && ahora.isAfter(expiraEn);
    }

    /** CA-HU12-01: una invitacion es utilizable solo si no fue usada, revocada ni expiro. */
    public boolean esValida(LocalDateTime ahora) {
        return !usada && !revocada && !estaExpirada(ahora);
    }

    /**
     * CA-HU12-01, CA-HU12-02: Consume la invitacion registrando la cuenta creada y el momento de uso.
     * Impide el doble uso, el uso de invitaciones revocadas y el uso tras la expiracion.
     */
    public void marcarUsada(Usuario usuarioCreado, LocalDateTime ahora) {
        if (this.usada) {
            throw new IllegalStateException("La invitacion ya fue utilizada");
        }
        if (this.revocada) {
            throw new IllegalStateException("La invitacion fue revocada");
        }
        if (estaExpirada(ahora)) {
            throw new IllegalStateException("La invitacion ha expirado");
        }
        this.usada = true;
        this.usadaEn = ahora;
        this.usuarioCreado = usuarioCreado;
    }

    /** Anula una invitacion pendiente para que no pueda utilizarse. */
    public void revocar() {
        if (this.usada) {
            throw new IllegalStateException("No se puede revocar una invitacion ya utilizada");
        }
        this.revocada = true;
    }

    /** Estado legible para el panel de administracion. */
    public String estadoLegible(LocalDateTime ahora) {
        if (revocada) return "REVOCADA";
        if (usada) return "USADA";
        if (estaExpirada(ahora)) return "EXPIRADA";
        return "PENDIENTE";
    }
}
