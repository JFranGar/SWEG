# 🛠️ PROMPT: CORRECCIONES DE CÓDIGO — POST-REVISIÓN SPRINT 1 SWGEC

## 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** del proyecto **SWGEC** (equipo Clean Code Crew).
Ya tienes implementados los Pasos 1–5 (estructura, seguridad con `HttpSession` + `AuthInterceptor`, Rich Domain Model, Controllers REST, Frontend Vanilla Premium Dark Mode).

El informe del Grupo revisor exige cambios documentales **y 4 ajustes a nivel de código** que mejoran la trazabilidad CA↔código y la granularidad de validaciones. Aplica **solo** lo siguiente. No alteres arquitectura ni paquetes.

---

## ✅ AJUSTE 1 — Trazabilidad CA en JavaDoc

Agregar el **código del CA** (`CA-HU{nn}-{kk}`) en el JavaDoc de cada método de dominio y endpoint REST que lo implementa. Esto cierra la observación de "trazabilidad CA ↔ TestRail ↔ código".

### 1.1 Entidades (paquete `model/`)

**`Usuario.java`** — actualizar JavaDocs:
```java
/** CA-HU01-03: Bloqueo temporal tras MAX_INTENTOS_FALLIDOS. */
public boolean estaBloqueado() { ... }

/** CA-HU01-03: Suma intento y bloquea si alcanza el límite. */
public void registrarIntentoFallido() { ... }

/** CA-HU01-01: Login exitoso resetea contadores. */
public void registrarLoginExitoso() { ... }
```

**`Sala.java`** — actualizar JavaDocs:
```java
/** CA-HU02-04: Validación servidor de campos obligatorios. */
public void validarCamposObligatorios() { ... }

/** CA-HU02-03: Modificación segura de datos de sala. */
public void actualizarDatos(...) { ... }
```

**`Reserva.java`** — actualizar JavaDocs:
```java
/** CA-HU04-04: Campos obligatorios. */
public void validarCamposObligatorios() { ... }

/** CA-HU04-05: No reservas en fechas pasadas. */
public void validarFechaNoPasada() { ... }

/** CA-HU04-03: horaInicio < horaFin. */
public void validarRangoHorario() { ... }

/** CA-HU04-02: Detección de solapamiento. */
public boolean seSolapaCon(Reserva otra) { ... }
```

### 1.2 Controllers (paquete `controller/`)

Sobre cada handler agrega `@CA` como comentario al inicio:

```java
// AuthController
@PostMapping("/login")  // CA-HU01-01, CA-HU01-02, CA-HU01-03, CA-HU01-04
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, ...) { ... }

// SalaController
@PostMapping              // CA-HU02-01, CA-HU02-02, CA-HU02-04
@PutMapping("/{id}")      // CA-HU02-03, CA-HU02-02, CA-HU02-04

// ReservaController
@PostMapping              // CA-HU04-01, CA-HU04-02, CA-HU04-03, CA-HU04-04, CA-HU04-05
```

---

## ✅ AJUSTE 2 — Errores granulares por campo (Bean Validation)

**Problema detectado:** el revisor pidió "un caso de prueba por cada campo obligatorio". Hoy el `GlobalExceptionHandler` concatena todos los errores en una sola string, lo que impide a TestRail asociar un error a su campo. **Cambio:** devolver un mapa `fields: { campo: mensaje }` además del mensaje global.

### 2.1 Reemplazar `dto/ApiError.java`

```java
package com.cleancodecrew.sweg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Respuesta de error uniforme.
 * ISO 25010 - Usabilidad / Prevención de errores.
 * Soporta errores granulares por campo (CA-HU02-04, CA-HU04-04).
 */
@Data
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> fields;   // NUEVO

    public static ApiError of(int status, String error, String path) {
        return new ApiError(status, error, path, LocalDateTime.now(), Collections.emptyMap());
    }

    public static ApiError of(int status, String error, String path, Map<String, String> fields) {
        return new ApiError(status, error, path, LocalDateTime.now(), fields);
    }
}
```

### 2.2 Actualizar `GlobalExceptionHandler` — handler de `MethodArgumentNotValidException`

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiError> handleBeanValidation(
        MethodArgumentNotValidException ex, HttpServletRequest req) {

    Map<String, String> campos = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
        // El primer mensaje por campo es suficiente
        campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }

    String mensajeGeneral = campos.isEmpty()
        ? "Datos invalidos"
        : "Hay campos invalidos en el formulario";

    return ResponseEntity.badRequest()
        .body(ApiError.of(400, mensajeGeneral, req.getRequestURI(), campos));
}
```

> Los demás handlers (`IllegalArgumentException`, `DuplicadoException`, `ConflictoException`, etc.) se mantienen igual usando el constructor `ApiError.of(status, error, path)`.

---

## ✅ AJUSTE 3 — Frontend: mostrar errores por campo

El frontend ya tiene los `<small class="error-msg">` por campo. Hay que pintarlos cuando el backend devuelva `fields`.

### 3.1 Actualizar `js/api.js`

En el bloque de error, propagar `data.fields`:

```js
if (!res.ok) {
    throw {
        status: res.status,
        message: data?.error || `Error HTTP ${res.status}`,
        fields: data?.fields || {},   // NUEVO
        data
    };
}
```

### 3.2 Helper compartido (añadir al final de `js/api.js`)

```js
/**
 * Pinta errores por campo enviados por el backend (CA-HU02-04, CA-HU04-04).
 * Asume convención: input id="campo" + span id="err-campo".
 * Mapeo opcional para nombres distintos (ej. capacidadMaxima -> capacidad).
 */
window.pintarErroresCampo = function (err, mapeo = {}) {
    if (!err || !err.fields) return;
    Object.entries(err.fields).forEach(([campoBackend, msg]) => {
        const campoUi = mapeo[campoBackend] || campoBackend;
        const input = document.getElementById(campoUi);
        const span  = document.getElementById('err-' + campoUi);
        if (input) input.classList.add('error');
        if (span)  span.textContent = msg;
    });
};
```

### 3.3 Usar el helper en los catch (`auth.js`, `admin.js`, `cliente.js`)

**`auth.js`** — en el catch del submit:
```js
} catch (err) {
    if (err.status === 400) {
        pintarErroresCampo(err);                       // CA-HU01-04
        toast.error(err.message);
    } else if (err.status === 423) {
        toast.error(err.message);                      // CA-HU01-03
    } else if (err.status === 401) {
        document.getElementById('correo').classList.add('error');
        document.getElementById('contrasena').classList.add('error');
        toast.error('Credenciales no validas');        // CA-HU01-02
    } else {
        toast.error(err.message);
    }
}
```

**`admin.js`** — en el catch:
```js
} catch (err) {
    if (err.status === 400) {
        pintarErroresCampo(err, { capacidadMaxima: 'capacidad' });  // CA-HU02-04
        toast.error(err.message);
    } else if (err.status === 409) {
        document.getElementById('nombre').classList.add('error');
        document.getElementById('err-nombre').textContent = err.message;
        toast.error(err.message);                                   // CA-HU02-02
    } else if (err.status === 401 || err.status === 403) {
        toast.error('Sesion expirada');
        setTimeout(() => location.href = '/html/login.html', 800);
    } else {
        toast.error(err.message);
    }
}
```

**`cliente.js`** — en el catch:
```js
} catch (err) {
    if (err.status === 400) {
        pintarErroresCampo(err, {
            salaId: 'sala',
            horaInicio: 'hora-inicio',
            horaFin: 'hora-fin'
        });                                                 // CA-HU04-03/04/05
        toast.error(err.message);
    } else if (err.status === 409) {
        document.getElementById('err-sala').textContent = err.message;
        toast.error(err.message);                           // CA-HU04-02
    } else if (err.status === 401 || err.status === 403) {
        toast.error('Sesion expirada');
        setTimeout(() => location.href = '/html/login.html', 800);
    } else {
        toast.error(err.message);
    }
}
```

---

## ✅ AJUSTE 4 — Confirmar ausencia de "selector de rol" en login

El revisor detectó que C51, C52 y C61 incluyen el paso "Seleccionar rol" que **no existe en la implementación** (correcto: el rol se infiere del usuario en backend, alineado con ISO 25010 Seguridad/Confidencialidad).

### Verificación obligatoria en `html/login.html`
- ✅ NO debe existir `<select id="rol">`, `<input name="rol">`, ni botones tipo "Soy Admin/Cliente/Recepcionista".
- ✅ El form solo expone `#correo` y `#contrasena`.
- ✅ `auth.js` redirige automáticamente según `me.rol` recibido del backend.

Si encuentras cualquier rastro de selector de rol en HTML, JS o CSS → **elimínalo**.

### Comentario obligatorio en `auth.js` (parte superior)
```js
/**
 * HU01 - Inicio de Sesion.
 *
 * Decision de diseno (Informe Revision Sprint 1):
 *   El paso "Seleccionar rol" descrito en C51/C52/C61 NO existe en el sistema.
 *   El rol se determina en el SERVIDOR a partir de Usuario.rol y el cliente
 *   solo redirige al panel correspondiente. Esto es mas seguro
 *   (ISO 25010 - Confidencialidad) y mas KISS.
 */
```

---

## 📤 FORMATO DE ENTREGA

Devuelve **únicamente los archivos modificados** en este orden y con bloques de código completos (no diffs parciales):

1. `model/Usuario.java` (solo JavaDocs nuevos)
2. `model/Sala.java` (solo JavaDocs nuevos)
3. `model/Reserva.java` (solo JavaDocs nuevos)
4. `dto/ApiError.java` (reemplazo completo)
5. `controller/GlobalExceptionHandler.java` (handler de validación actualizado, resto igual)
6. `controller/AuthController.java` (solo añadir comentarios `// CA-…`)
7. `controller/SalaController.java` (solo comentarios `// CA-…`)
8. `controller/ReservaController.java` (solo comentarios `// CA-…`)
9. `static/js/api.js` (actualizado con `fields` + helper `pintarErroresCampo`)
10. `static/js/auth.js` (catch actualizado + comentario decisión de diseño)
11. `static/js/admin.js` (catch actualizado)
12. `static/js/cliente.js` (catch actualizado)
13. `static/html/login.html` (verificación: sin selector de rol)

Al final, una **tabla resumen** así:

| Archivo | Cambio | CA cubierto |
|---|---|---|
| `Usuario.java` | JavaDocs con CA | CA-HU01-01, CA-HU01-03 |
| `Sala.java` | JavaDocs con CA | CA-HU02-03, CA-HU02-04 |
| `Reserva.java` | JavaDocs con CA | CA-HU04-02 a CA-HU04-05 |
| `ApiError.java` | Soporte `fields` | CA-HU02-04, CA-HU04-04 |
| `GlobalExceptionHandler.java` | Errores granulares | CA-HU02-04, CA-HU04-04 |
| `Auth/Sala/ReservaController.java` | Anotaciones CA en endpoints | Todos |
| `api.js` | helper `pintarErroresCampo` | CA-HU02-04, CA-HU04-04 |
| `auth.js` / `admin.js` / `cliente.js` | catch con errores por campo | Todos |
| `login.html` | Verificación sin selector rol | Decisión informe |

Y la frase de cierre exacta:
> "Correcciones a nivel de código aplicadas. Trazabilidad CA implementada en backend y errores granulares por campo activos en frontend. Sprint 1 listo para auditoría final."

---

## 🚫 PROHIBIDO

- ❌ Crear nuevos endpoints o entidades.
- ❌ Romper la firma de `ApiError.of(status, error, path)` existente (mantener sobrecarga retro-compatible).
- ❌ Tocar `pom.xml`, `application.properties`, `AuthInterceptor`, `WebConfig`, `PasswordHasher` o `DataSeeder`.
- ❌ Modificar el CSS Premium Dark Mode.
- ❌ Reintroducir un selector de rol en el login.