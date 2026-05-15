# 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** y **Evaluador QA Universitario (ISO/IEC 25010)** para el proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** del equipo **Clean Code Crew**.

Vienes de:
- **Paso 1**: estructura de carpetas y placeholders.
- **Paso 2**: seguridad por `HttpSession` + `AuthInterceptor` (sin Spring Security) + `PasswordHasher` + `DataSeeder`.
- **Paso 3**: modelos JPA con **Rich Domain Model** (`Usuario`, `Sala`, `Reserva`, enums).

Ahora ejecutas el **Paso 4: DTOs + Repositories + Controllers REST** para el Sprint 1.

---

# 📋 REGLAS NO NEGOCIABLES

- **Spring Boot 4.0.6**, **Java 21**, **JPA**, **PostgreSQL**.
- **NO usar Spring Security**. La autenticación y autorización ya están resueltas por el `AuthInterceptor` del Paso 2 + claves en `HttpSession`.
- **NO existe capa `service/`**. Los controllers orquestan el flujo y delegan las validaciones de negocio a las **entidades** (Rich Domain Model).
- Los controllers usan **DTOs**, NUNCA exponen entidades JPA directamente.
- **Bean Validation** (`@Valid`, `@NotBlank`, `@NotNull`, `@Min`, `@Email`) en los DTOs.
- **Códigos HTTP exactos** según ISO 25010 (Fiabilidad/Usabilidad):
  - `200 OK` — operación correcta.
  - `201 Created` — recurso creado.
  - `204 No Content` — eliminado.
  - `400 Bad Request` — Bean Validation falló o regla de dominio rompió.
  - `401 Unauthorized` — credenciales inválidas o sesión inexistente.
  - `403 Forbidden` — rol incorrecto (ya lo maneja el `AuthInterceptor`).
  - `409 Conflict` — duplicado o solapamiento.
  - `423 Locked` — usuario temporalmente bloqueado.
- **Paquete base**: `com.cleancodecrew.sweg`.

---

# 🎯 ARCHIVOS A GENERAR

## DTOs (`dto/`)
1. `LoginRequest.java`
2. `SalaRequest.java`
3. `ReservaRequest.java`
4. `ApiError.java`

## Repositories (`repository/`)
5. `UsuarioRepository.java`
6. `SalaRepository.java`
7. `ReservaRepository.java`

## Controllers (`controller/`)
8. `AuthController.java` → HU1
9. `SalaController.java` → HU2
10. `ReservaController.java` → HU4
11. `GlobalExceptionHandler.java` → manejo uniforme de errores

---

# 📦 ESPECIFICACIONES DETALLADAS

## A. DTOs

### A.1 `LoginRequest`
```java
package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de entrada para HU1 Inicio de Sesion.
 * HU1 CA4: validacion de campos obligatorios via Bean Validation.
 */
@Data
public class LoginRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo invalido")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;
}
```

### A.2 `SalaRequest`
```java
package com.cleancodecrew.sweg.dto;

import com.cleancodecrew.sweg.model.TipoSala;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de entrada para HU2 Gestion de Salas.
 * HU2 CA4: campos obligatorios via Bean Validation.
 */
@Data
public class SalaRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo es obligatorio")
    private TipoSala tipo;

    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private int capacidadMaxima;
}
```

### A.3 `ReservaRequest`
```java
package com.cleancodecrew.sweg.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada para HU4 Reserva de Sala.
 * HU4 CA4: campos obligatorios via Bean Validation.
 */
@Data
public class ReservaRequest {
    @NotNull(message = "Seleccione una sala")
    private Long salaId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}
```

### A.4 `ApiError`
```java
package com.cleancodecrew.sweg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Respuesta de error uniforme. ISO 25010 - Usabilidad (mensajes consistentes).
 */
@Data
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String path;
    private LocalDateTime timestamp;

    public static ApiError of(int status, String error, String path) {
        return new ApiError(status, error, path, LocalDateTime.now());
    }
}
```

---

## B. Repositories

### B.1 `UsuarioRepository`
- Extiende `JpaRepository<Usuario, Long>`.
- Método: `Optional<Usuario> findByCorreoIgnoreCase(String correo);`

### B.2 `SalaRepository`
- Extiende `JpaRepository<Sala, Long>`.
- Métodos:
  - `Optional<Sala> findByNombreIgnoreCase(String nombre);` → HU2 CA2 (duplicado).
  - `boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);` → HU2 CA2 al editar.

### B.3 `ReservaRepository`
- Extiende `JpaRepository<Reserva, Long>`.
- Métodos:
  - `List<Reserva> findBySala_IdAndFechaAndEstadoNot(Long salaId, LocalDate fecha, EstadoReserva estado);` → para HU4 CA2 (busca posibles solapadas excluyendo `CANCELADA`).
  - `List<Reserva> findByCliente_IdOrderByFechaDescHoraInicioDesc(Long clienteId);` → tabla "Mis Reservas".

---

## C. Controllers

### C.1 `AuthController` → `/api/auth`

**Rutas (todas públicas, excluidas del interceptor):**

| Método | Path | Descripción | CA |
|---|---|---|---|
| POST | `/api/auth/login` | Login y creación de sesión | HU1 CA1, CA2, CA3, CA4 |
| POST | `/api/auth/logout` | Invalida la sesión | — |
| GET  | `/api/auth/me` | Devuelve usuario autenticado | usado por `guards.js` |

**Dependencias inyectadas (por constructor):**
- `UsuarioRepository`
- `PasswordHasher`

**`POST /login` — flujo paso a paso:**

```
1. Recibir LoginRequest con @Valid. Si falla -> 400 (lo maneja el handler global).
2. Buscar usuario por correo (case-insensitive).
   - Si no existe -> 401 "Credenciales no validas".          [HU1 CA2]
3. Si usuario.estaBloqueado():
   - Responder 423 LOCKED con mensaje:
     "Cuenta bloqueada. Intentelo en {minutosRestantesDeBloqueo()} minutos." [HU1 CA3]
4. Verificar passwordHasher.matches(plain, usuario.contrasenaHash):
   - Si NO: usuario.registrarIntentoFallido(); guardar; 401.   [HU1 CA2/CA3]
5. Si SI: usuario.registrarLoginExitoso(); guardar;
   - Guardar en HttpSession:
       SESSION_USER_ID -> usuario.getId()
       SESSION_ROL     -> usuario.getRol().name()
       SESSION_NOMBRE  -> usuario.getNombre()
       SESSION_CORREO  -> usuario.getCorreo()
   - Responder 200 con { id, nombre, correo, rol }.            [HU1 CA1]
```

> ⚠️ Usar las constantes `AuthInterceptor.SESSION_USER_ID`, etc. NO strings sueltos.

**`POST /logout`:**
- `request.getSession(false)?.invalidate();`
- Responde 204.

**`GET /me`:**
- Lee de la sesión. Si no hay `usuarioId` → **401**.
- Responde `{ id, nombre, correo, rol }`.

**Respuesta DTO interna** (clase `record` o `Map<String,Object>` — preferible un `record` privado o un nuevo DTO `SessionResponse`):
```java
public record SessionResponse(Long id, String nombre, String correo, String rol) {}
```
Puedes declararlo dentro del propio `AuthController` o crearlo en `dto/`. Recomendado: `dto/SessionResponse.java`.

---

### C.2 `SalaController` → `/api/admin/salas`

**Protección:** automática vía interceptor — solo `ADMIN`.

**Dependencias:** `SalaRepository`.

| Método | Path | Lógica | Códigos | CA |
|---|---|---|---|---|
| GET | `/` | Listar todas las salas ordenadas por nombre | 200 | — |
| POST | `/` | Crear sala | 201 / 400 / 409 | HU2 CA1, CA2, CA4 |
| PUT | `/{id}` | Editar sala | 200 / 400 / 404 / 409 | HU2 CA3, CA4 |
| DELETE | `/{id}` | Eliminar sala | 204 / 404 | — |

**Flujo POST:**
```
1. @Valid SalaRequest. -> 400 si falla (handler global).
2. Si salaRepository.findByNombreIgnoreCase(req.nombre).isPresent():
   throw new DuplicadoException("Ya existe una sala con ese nombre");   [HU2 CA2]
3. Construir Sala con los datos.
4. sala.validarCamposObligatorios();   [HU2 CA4 - defensa adicional]
5. salaRepository.save(sala);
6. return 201 con la sala.
```

**Flujo PUT:**
```
1. Buscar sala por id -> 404 si no existe.
2. Si existsByNombreIgnoreCaseAndIdNot(req.nombre, id):
   throw DuplicadoException("Ya existe otra sala con ese nombre");
3. sala.actualizarDatos(req.nombre, req.tipo, req.capacidadMaxima);     [HU2 CA3, CA4]
4. save -> 200.
```

---

### C.3 `ReservaController` → `/api/cliente/reservas`

**Protección:** vía interceptor — solo `CLIENTE`.

**Dependencias:** `SalaRepository`, `ReservaRepository`, `UsuarioRepository`.

| Método | Path | Lógica | Códigos | CA |
|---|---|---|---|---|
| GET | `/salas-disponibles` | Lista salas DISPONIBLES para el formulario | 200 | — |
| GET | `/` | "Mis reservas" del cliente autenticado | 200 | — |
| POST | `/` | Crear reserva | 201 / 400 / 404 / 409 | HU4 CA1-CA5 |

**Flujo POST /:**
```
1. @Valid ReservaRequest -> 400.                                       [HU4 CA4]
2. Obtener clienteId desde HttpSession (SESSION_USER_ID).
   Si null -> 401 (el interceptor lo evita, pero defensiva).
3. Buscar Sala por salaId -> 404 si no existe.
4. Buscar Usuario cliente por id -> 404 si no existe.
5. Construir Reserva con los datos.
6. reserva.validarTodo();                                              [HU4 CA3, CA4, CA5]
7. List<Reserva> candidatas =
     reservaRepository.findBySala_IdAndFechaAndEstadoNot(
         req.salaId, req.fecha, EstadoReserva.CANCELADA);
   for (Reserva r : candidatas) {
       if (reserva.seSolapaCon(r))
           throw new ConflictoException(
               "La sala ya esta reservada en ese horario");           [HU4 CA2]
   }
8. reservaRepository.save(reserva);
9. return 201 con un payload { id, mensaje: "Reserva confirmada", sala:{...}, fecha, horaInicio, horaFin, estado }.
```

**Respuesta de reserva:** crear un mini DTO o `record`:
```java
public record ReservaResponse(
    Long id, String mensaje, SalaMini sala,
    LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String estado
) {
    public record SalaMini(Long id, String nombre, String tipo, int capacidadMaxima) {}
}
```
Colocarlo en `dto/ReservaResponse.java`.

---

### C.4 `GlobalExceptionHandler`

**Anotación:** `@RestControllerAdvice`.

**Handlers obligatorios:**

| Excepción | HTTP | Comportamiento |
|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 400 | Concatena los `getDefaultMessage()` de cada `FieldError` separados por `; `. |
| `IllegalArgumentException` (lanzada por entidades) | 400 | `e.getMessage()` |
| `DuplicadoException` (custom) | 409 | `e.getMessage()` |
| `ConflictoException` (custom) | 409 | `e.getMessage()` |
| `NoSuchElementException` / `EntityNotFoundException` | 404 | `e.getMessage()` |
| `Exception` genérica | 500 | "Error interno del servidor" |

**Excepciones custom a crear** (en el mismo paquete `controller/` o mejor en `controller/exception/` SI quieres mantenerlo simple ponlas en `controller/`):

```java
public class DuplicadoException extends RuntimeException {
    public DuplicadoException(String msg) { super(msg); }
}

public class ConflictoException extends RuntimeException {
    public ConflictoException(String msg) { super(msg); }
}
```

> ⚠️ No crear carpeta `exception/`. Mantenerlas en `controller/` para respetar la regla KISS del Paso 1.

**Formato de respuesta:** `ApiError.of(status, mensaje, request.getRequestURI())`.

---

# 🧪 TABLA DE AUDITORÍA QA (incluir al final)

| HU | CA | Capa que lo cubre | Mecanismo |
|---|---|---|---|
| HU1 | CA1 | `AuthController.login` | Devuelve rol → frontend redirige |
| HU1 | CA2 | `AuthController.login` | 401 "Credenciales no validas" |
| HU1 | CA3 | `Usuario` + `AuthController` | 423 con minutos restantes |
| HU1 | CA4 | `LoginRequest` + `GlobalExceptionHandler` | 400 Bean Validation |
| HU2 | CA1 | `SalaController.crear` | 201 |
| HU2 | CA2 | `SalaController` + `SalaRepository` | 409 `DuplicadoException` |
| HU2 | CA3 | `SalaController.editar` + `Sala.actualizarDatos` | 200 |
| HU2 | CA4 | `SalaRequest` + `Sala.validarCamposObligatorios` | 400 |
| HU4 | CA1 | `ReservaController.crear` | 201 |
| HU4 | CA2 | `Reserva.seSolapaCon` + `ReservaRepository` | 409 `ConflictoException` |
| HU4 | CA3 | `Reserva.validarRangoHorario` | 400 |
| HU4 | CA4 | `ReservaRequest` + `Reserva.validarCamposObligatorios` | 400 |
| HU4 | CA5 | `Reserva.validarFechaNoPasada` | 400 |

---

# 🚫 PROHIBIDO EN ESTE PASO

- ❌ Crear clases en una carpeta `service/`.
- ❌ Importar `org.springframework.security.*`.
- ❌ Devolver entidades JPA crudas (siempre via DTO/record).
- ❌ Mezclar lógica de negocio dentro de los controllers (delegar a las entidades).
- ❌ Strings mágicos como claves de sesión: usar las constantes de `AuthInterceptor`.
- ❌ Manejar errores con `try/catch` enormes dentro de controllers: usar el `GlobalExceptionHandler`.

---

# 📤 FORMATO DE ENTREGA

Entrega la respuesta en este orden estricto:

1. **DTOs**:
   - `dto/LoginRequest.java`
   - `dto/SalaRequest.java`
   - `dto/ReservaRequest.java`
   - `dto/ApiError.java`
   - `dto/SessionResponse.java`
   - `dto/ReservaResponse.java`
2. **Repositories**:
   - `repository/UsuarioRepository.java`
   - `repository/SalaRepository.java`
   - `repository/ReservaRepository.java`
3. **Excepciones custom**:
   - `controller/DuplicadoException.java`
   - `controller/ConflictoException.java`
4. **Controllers**:
   - `controller/AuthController.java`
   - `controller/SalaController.java`
   - `controller/ReservaController.java`
   - `controller/GlobalExceptionHandler.java`
5. **Tabla de auditoría QA** (la de arriba).
6. **Justificación ISO 25010** (Fiabilidad + Usabilidad + Seguridad): explicar en 5 líneas por qué los códigos HTTP exactos (400/401/403/409/423) cumplen con ISO 25010.
7. **Confirmación final**: "Paso 4 listo. DTOs, Repositories y Controllers REST con manejo uniforme de errores implementados. Sprint 1 — Backend completo. Listo para Paso 5 (Frontend Vanilla)."