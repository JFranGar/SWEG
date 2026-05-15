# 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** y **Evaluador de Calidad Universitario (ISO/IEC 25010)** para el proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** del equipo **Clean Code Crew**.

Vienes de:
- **Paso 1**: estructura de carpetas creada.
- **Paso 2**: configuración de seguridad implementada (`AuthInterceptor`, `WebConfig`, `PasswordHasher`, `DataSeeder`, `application.properties`, `pom.xml`).

Ahora ejecutas el **Paso 3: Modelos JPA con Rich Domain Model**. La regla suprema de este paso es:

> 🟡 **TODA la lógica de negocio vive dentro de las entidades.**
> NO existe carpeta `service/`. NO hay "Anemic Domain Models".
> Si un método pertenece al negocio (validar, cambiar de estado, comparar), va dentro de la entidad correspondiente.

---

# 📋 REGLAS NO NEGOCIABLES

- **Spring Boot 4.0.6**, **Java 21**, **JPA / Hibernate**, **PostgreSQL**.
- **Lombok**: usar `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` cuando convenga.
  - ❌ NO usar `@Data` (genera `equals/hashCode` peligrosos en entidades JPA).
- **NO** anotaciones de Spring Security.
- **NO** lógica en setters; usar métodos de dominio expresivos (`registrarIntentoFallido()`, `seSolapaCon()`, etc.).
- **Bean Validation** en los campos (`@NotBlank`, `@Email`, `@Min`, `@NotNull`).
- **Paquete base**: `com.cleancodecrew.sweg.model`.
- Cada entidad **debe documentar en JavaDoc** qué Criterios de Aceptación (CA) implementa.

---

# 🎯 ARCHIVOS A GENERAR

| Archivo | Tipo | Propósito |
|---|---|---|
| `Rol.java` | Enum | Roles del sistema. |
| `TipoSala.java` | Enum | Tipos de sala. |
| `EstadoSala.java` | Enum | Estado físico de la sala. |
| `EstadoReserva.java` | Enum | Estado lógico de una reserva. |
| `Usuario.java` | Entidad | HU1 — login, intentos fallidos, bloqueo. |
| `Sala.java` | Entidad | HU2 — gestión de salas. |
| `Reserva.java` | Entidad | HU4 — reserva y solapamiento. |

---

# 📦 ESPECIFICACIONES DETALLADAS

## 1. `Rol.java`
```java
public enum Rol { ADMIN, RECEPCIONISTA, CLIENTE }
```

## 2. `TipoSala.java`
```java
public enum TipoSala { REUNION, SEMINARIO, TRABAJO }
```

## 3. `EstadoSala.java`
```java
public enum EstadoSala { DISPONIBLE, RESERVADA, EN_USO, EN_LIMPIEZA, INACTIVA }
```

## 4. `EstadoReserva.java`
```java
public enum EstadoReserva { PENDIENTE, CONFIRMADA, ACTIVA, CANCELADA, FINALIZADA }
```

Todos los enums llevan el package `com.cleancodecrew.sweg.model` y un JavaDoc breve.

---

## 5. `Usuario.java`  → HU1

### Campos JPA

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` |
| `nombre` | `String` | `@NotBlank`, `length=120` |
| `correo` | `String` | `@NotBlank`, `@Email`, **único** (`unique=true`), `length=150` |
| `contrasenaHash` | `String` | `@NotBlank`, `length=255`, no nulo |
| `rol` | `Rol` | `@Enumerated(EnumType.STRING)`, `@NotNull` |
| `intentosFallidos` | `int` | default 0 |
| `bloqueadoHasta` | `LocalDateTime` | nullable |
| `ultimoLogin` | `LocalDateTime` | nullable |

`@Entity @Table(name = "usuarios")`.

### Constantes de negocio (dentro de la clase)
```java
public static final int MAX_INTENTOS_FALLIDOS = 3;
public static final int MINUTOS_BLOQUEO = 15;
```

### Métodos de dominio obligatorios

| Método | Lógica | CA cubierto |
|---|---|---|
| `boolean estaBloqueado()` | `bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now())` | HU1 CA3 |
| `void registrarIntentoFallido()` | `intentosFallidos++`; si llega a `MAX_INTENTOS_FALLIDOS`, asigna `bloqueadoHasta = now() + MINUTOS_BLOQUEO` y resetea `intentosFallidos = 0`. | HU1 CA3 |
| `void registrarLoginExitoso()` | resetea `intentosFallidos = 0`, `bloqueadoHasta = null`, `ultimoLogin = now()`. | HU1 CA1 |
| `long minutosRestantesDeBloqueo()` | si `estaBloqueado()`, retorna `ChronoUnit.MINUTES.between(now(), bloqueadoHasta) + 1`; si no, retorna 0. | HU1 CA3 (UX) |

> ⚠️ El **hash de la contraseña NO se calcula aquí**. Se recibe ya hasheado desde `PasswordHasher` (responsabilidad única).

### JavaDoc obligatorio (cabecera de la clase)
```java
/**
 * Entidad Usuario - HU1 Inicio de Sesion.
 *
 * Rich Domain Model (ISO 25010 - Mantenibilidad):
 *  - HU1 CA1: rol diferenciado (campo {@link Rol}).
 *  - HU1 CA2: la verificacion de credenciales se delega al PasswordHasher,
 *             pero el manejo de intentos vive aqui.
 *  - HU1 CA3: bloqueo temporal por intentos fallidos.
 */
```

---

## 6. `Sala.java` → HU2

### Campos JPA

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | identity |
| `nombre` | `String` | `@NotBlank`, **único** (`unique=true`), `length=100` |
| `tipo` | `TipoSala` | `@Enumerated(STRING)`, `@NotNull` |
| `capacidadMaxima` | `int` | `@Min(1)` |
| `estado` | `EstadoSala` | `@Enumerated(STRING)`, default `DISPONIBLE` |

`@Entity @Table(name = "salas")`.

### Métodos de dominio obligatorios

| Método | Lógica | CA |
|---|---|---|
| `void validarCamposObligatorios()` | Si `nombre` es null/blank, `tipo` null, o `capacidadMaxima <= 0`, lanza `IllegalArgumentException("Todos los campos son obligatorios")`. | HU2 CA4 |
| `void actualizarDatos(String nombre, TipoSala tipo, int capacidadMaxima)` | Asigna los 3 campos y al final llama `validarCamposObligatorios()`. | HU2 CA3 |

> 🔒 **La validación de nombre duplicado NO vive aquí** (es responsabilidad del controller consultando al repositorio). Pero las validaciones de forma SÍ están aquí.

### JavaDoc obligatorio
```java
/**
 * Entidad Sala - HU2 Gestion de Salas.
 *
 *  - HU2 CA1: creacion (orquestada por el controller, validacion en la entidad).
 *  - HU2 CA3: modificacion via {@link #actualizarDatos}.
 *  - HU2 CA4: validacion de campos obligatorios.
 */
```

---

## 7. `Reserva.java` → HU4

### Campos JPA

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | identity |
| `sala` | `Sala` | `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name="sala_id")`, `@NotNull` |
| `cliente` | `Usuario` | `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name="cliente_id")`, `@NotNull` |
| `fecha` | `LocalDate` | `@NotNull` |
| `horaInicio` | `LocalTime` | `@NotNull` |
| `horaFin` | `LocalTime` | `@NotNull` |
| `estado` | `EstadoReserva` | `@Enumerated(STRING)`, default `CONFIRMADA` |
| `creadaEn` | `LocalDateTime` | default `now()` en `@PrePersist` |

`@Entity @Table(name = "reservas")`.

### Métodos de dominio obligatorios

| Método | Lógica | CA |
|---|---|---|
| `void validarCamposObligatorios()` | Lanza `IllegalArgumentException` si cualquier campo (`sala`, `cliente`, `fecha`, `horaInicio`, `horaFin`) es null. | HU4 CA4 |
| `void validarFechaNoPasada()` | Si `fecha.isBefore(LocalDate.now())`, lanza `IllegalArgumentException("No se permiten reservas en fechas pasadas")`. | HU4 CA5 |
| `void validarRangoHorario()` | Si `!horaInicio.isBefore(horaFin)`, lanza `IllegalArgumentException("La hora de inicio debe ser menor a la hora de fin")`. | HU4 CA3 |
| `boolean seSolapaCon(Reserva otra)` | Devuelve `true` si misma `sala.id` && misma `fecha` && los rangos `[horaInicio, horaFin)` se cruzan. **Ignorar** reservas con estado `CANCELADA`. | HU4 CA2 |
| `void validarTodo()` | Atajo: ejecuta los 3 `validar*` en orden. | Reusable en el controller |

### Fórmula de solapamiento (referencia)
```
solapan ⇔ this.horaInicio < otra.horaFin  &&  otra.horaInicio < this.horaFin
```

### Hook JPA
```java
@PrePersist
private void onCreate() {
    if (creadaEn == null) creadaEn = LocalDateTime.now();
    if (estado == null) estado = EstadoReserva.CONFIRMADA;
}
```

### JavaDoc obligatorio
```java
/**
 * Entidad Reserva - HU4 Reservar Sala.
 *
 *  - HU4 CA1: reserva exitosa.
 *  - HU4 CA2: deteccion de solapamiento via {@link #seSolapaCon}.
 *  - HU4 CA3: rango horario coherente.
 *  - HU4 CA4: validacion de campos obligatorios.
 *  - HU4 CA5: rechazo de fechas pasadas.
 */
```

---

# 🧪 TABLA DE AUDITORÍA QA (incluir al final de la respuesta)

| HU | CA | Entidad responsable | Método específico |
|---|---|---|---|
| HU1 | CA1 | `Usuario` | campo `rol` |
| HU1 | CA2 | `PasswordHasher` + `Usuario` | `registrarIntentoFallido()` |
| HU1 | CA3 | `Usuario` | `estaBloqueado()`, `registrarIntentoFallido()`, `minutosRestantesDeBloqueo()` |
| HU1 | CA4 | DTO `LoginRequest` (Paso 4) | `@NotBlank` (no aplica aquí) |
| HU2 | CA1 | `Sala` | constructor + `validarCamposObligatorios()` |
| HU2 | CA2 | `SalaRepository` (Paso 4) | no aplica aquí |
| HU2 | CA3 | `Sala` | `actualizarDatos()` |
| HU2 | CA4 | `Sala` | `validarCamposObligatorios()` |
| HU4 | CA1 | `Reserva` | flujo completo |
| HU4 | CA2 | `Reserva` | `seSolapaCon()` |
| HU4 | CA3 | `Reserva` | `validarRangoHorario()` |
| HU4 | CA4 | `Reserva` | `validarCamposObligatorios()` |
| HU4 | CA5 | `Reserva` | `validarFechaNoPasada()` |

---

# 🚫 PROHIBIDO EN ESTE PASO

- ❌ Crear ninguna clase `Service`, `Manager`, `Helper` o `Validator` externo.
- ❌ Usar `@Data` de Lombok.
- ❌ Implementar `equals/hashCode` (que JPA los maneje por id).
- ❌ Importar paquetes de Spring Security.
- ❌ Lógica de negocio dentro de los controllers (todavía no se tocan).
- ❌ Persistir contraseñas en texto plano.
- ❌ Romper la estructura de carpetas del Paso 1.

---

# 📤 FORMATO DE ENTREGA

Entrega la respuesta en este orden estricto:

1. `model/Rol.java` (bloque ```java).
2. `model/TipoSala.java`
3. `model/EstadoSala.java`
4. `model/EstadoReserva.java`
5. `model/Usuario.java`
6. `model/Sala.java`
7. `model/Reserva.java`
8. **Tabla de auditoría QA** (la de arriba, rellena con los métodos reales que generaste).
9. **Justificación ISO 25010 — Mantenibilidad/Modularidad** (3-5 líneas explicando por qué Rich Domain Model cumple esta subcaracterística).
10. **Confirmación final**: "Paso 3 listo. Modelos JPA con Rich Domain Model implementados. Lógica de negocio encapsulada en entidades. Listo para Paso 4 (DTOs + Repositories + Controllers REST)."