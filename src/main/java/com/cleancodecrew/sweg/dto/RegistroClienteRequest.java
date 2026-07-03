package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO de entrada para el auto-registro público de clientes (HU RBAC Híbrido).
 * El rol NUNCA se recibe del cliente: el backend fuerza siempre CLIENTE.
 *
 * Validaciones (CA-HU12-01 / CA-HU12-04):
 *  - nombre y apellido: solo letras (incluye tildes y ñ) y espacios; sin números
 *    ni caracteres especiales.
 *  - contrasena: contraseña segura de mínimo 8 caracteres, con al menos una
 *    mayúscula, una minúscula y un número.
 */
@Data
public class RegistroClienteRequest {

    /** Solo letras y espacios (permite nombres compuestos y tildes/ñ). */
    public static final String PATRON_NOMBRE = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?: [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$";

    /** Mínimo 8 caracteres, al menos una minúscula, una mayúscula y un número. */
    public static final String PATRON_CONTRASENA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = PATRON_NOMBRE, message = "El nombre solo puede contener letras")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = PATRON_NOMBRE, message = "El apellido solo puede contener letras")
    private String apellido;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PATRON_CONTRASENA,
            message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
    private String contrasena;
}
