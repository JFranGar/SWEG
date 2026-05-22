# 🚀 PROMPT: SPRINT 2 — SWGEC (Clean Code Crew)

## 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** y **Evaluador QA Universitario (ISO/IEC 25010)** del proyecto **SWGEC** (Sistema Web de Gestión de Espacios de CoWorking).

**Stack ya existente (Sprint 1 — NO modificar arquitectura):**
- Backend: Spring Boot 4.0.6 + Java 21 + JPA/Hibernate + PostgreSQL + Bean Validation.
- Seguridad: `HttpSession` + `AuthInterceptor` (sin Spring Security).
- Dominio: Rich Domain Model con lógica en entidades.
- Frontend: HTML + CSS Premium Dark Mode + Vanilla JS desde `src/main/resources/static`.
- DTOs: `ApiError` (con `fields` granular), `LoginRequest`, `SalaRequest`, `ReservaRequest`.
- Entidades: `Usuario`, `Sala`, `Reserva` con enums `Rol`, `TipoSala`, `EstadoSala`, `EstadoReserva`.
- Endpoints existentes: `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`, `/api/salas` (CRUD), `/api/reservas` (POST + GET propias).

**Reglas inviolables:**
- ✅ KISS: sin microservicios, sin Redis, sin JWT, sin Spring Security, sin nuevos starters salvo justificación.
- ✅ Rich Domain Model: validaciones de negocio van **dentro de las entidades**, no en controllers ni "services anémicos".
- ✅ Validaciones en cliente (JS) Y servidor (Java). HTTP codes correctos: 400, 401, 403, 404, 409, 423.
- ✅ Premium Dark Mode (#0A0A0A, #161616, dorado #FFD700, Inter, radius 10px) — reutilizar componentes ya creados.
- ✅ Trazabilidad: todo método de dominio y endpoint lleva comentario `CA-HU{nn}-{kk}`.

---

## 🔄 CAMBIO TRANSVERSAL — REACTIVAR SELECTOR DE ROL

En Sprint 1 se eliminó el "Seleccionar rol" del login. **Para Sprint 2 el equipo decidió reactivarlo** como capa de **UX/intención de acceso** (no como mecanismo de seguridad — la autorización real sigue dependiendo de `Usuario.rol` en backend).

### Backend (`AuthController.java`)
1. Añadir campo `rolSeleccionado` a `LoginRequest`:
   ```java
   @NotNull(message = "Debe seleccionar un rol")
   private Rol rolSeleccionado;
   ```
2. En `/api/auth/login`, después de validar credenciales y bloqueo:
   - Si `usuario.getRol() != req.getRolSeleccionado()` → **403** con mensaje:
     > "El rol seleccionado no coincide con su cuenta."
   - Esto es validación de coherencia, NO de seguridad (la seguridad sigue siendo el rol real del usuario almacenado en sesión).
3. Comentario obligatorio:
   ```java
   // CA-HU01-01: el rol seleccionado debe coincidir con el rol real.
   ```

### Frontend (`html/login.html` + `js/auth.js`)
1. Agregar selector visual de rol como **3 cards clickeables** (no `<select>`) con estética Premium Dark Mode:
   ```
   [ 👑 Administrador ]  [ 🛎️ Recepcionista ]  [ 👤 Cliente ]
   ```
2. Al hacer click se activa el card (borde dorado `#FFD700`) y se setea `data-rol` en un hidden input `#rol-seleccionado`.
3. Si el usuario envía el form sin seleccionar rol → mostrar error `#err-rol` y NO enviar al backend.
4. En el `fetch` enviar `{ correo, contrasena, rolSeleccionado }`.
5. Manejar el nuevo 403 "rol no coincide" con `toast.error` y resaltar los cards.

**CSS nuevo (añadir al final de `style.css`):**
```css
.role-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.role-card {
    background: #161616;
    border: 1px solid #2a2a2a;
    border-radius: 10px;
    padding: 16px 8px;
    text-align: center;
    cursor: pointer;
    transition: all .2s ease;
    color: #ddd;
    font-size: 13px;
    user-select: none;
}
.role-card:hover { border-color: #FFD700; transform: translateY(-1px); }
.role-card.active { border-color: #FFD700; background: linear-gradient(135deg, #1a1a1a, #161616); box-shadow: 0 0 0 1px #FFD700 inset; }
.role-card .role-icon { font-size: 22px; display: block; margin-bottom: 6px; }
```

---

## 📦 HU5 — CONSULTA DE DISPONIBILIDAD DE SALA (Cliente)

### Backend

#### 1. Nuevo endpoint en `ReservaController.java`
```
GET /api/reservas/disponibilidad
    ?salaId={id}&fecha=YYYY-MM-DD&horaInicio=HH:mm&horaFin=HH:mm
```
- Protegido por `AuthInterceptor` para rol **CLIENTE**.
- Responde con DTO `DisponibilidadResponse`:
  ```java
  public record DisponibilidadResponse(
      Long salaId,
      String nombreSala,
      LocalDate fecha,
      LocalTime horaInicio,
      LocalTime horaFin,
      boolean disponible,
      String mensaje
  ) {}
  ```

#### 2. Validaciones del controller (orden estricto)
1. **CA-HU05-04**: Campos `salaId`, `fecha`, `horaInicio`, `horaFin` obligatorios → `400` con `fields`.
2. **CA-HU05-04**: `horaInicio < horaFin` → reusar `Reserva.validarRangoHorario()` (instanciar Reserva temporal o extraer a método estático en `Reserva`).
3. **CA-HU05-05**: `fecha < hoy` → reusar `Reserva.validarFechaNoPasada()` → `400` "No se puede consultar fechas pasadas".
4. **CA-HU05-01 / 02 / 03**: consultar repositorio.

#### 3. Nuevo método en `ReservaRepository.java`
```java
@Query("""
   SELECT r FROM Reserva r
   WHERE r.sala.id = :salaId
     AND r.fecha = :fecha
     AND r.estado <> com.cleancodecrew.sweg.model.EstadoReserva.CANCELADA
""")
List<Reserva> findActivasPorSalaYFecha(@Param("salaId") Long salaId,
                                       @Param("fecha") LocalDate fecha);
```

#### 4. Lógica en el controller (reutilizando `Reserva.seSolapaCon`)
```java
Reserva consulta = Reserva.deConsulta(sala, fecha, horaInicio, horaFin); // factory
List<Reserva> activas = reservaRepo.findActivasPorSalaYFecha(salaId, fecha);
boolean ocupada = activas.stream().anyMatch(consulta::seSolapaCon);
```
Agregar factory **estática** en `Reserva.java`:
```java
/** CA-HU05-01: Construye una Reserva no persistida solo para chequear solapamiento. */
public static Reserva deConsulta(Sala sala, LocalDate f, LocalTime hi, LocalTime hf) {
    Reserva r = new Reserva();
    r.sala = sala; r.fecha = f; r.horaInicio = hi; r.horaFin = hf;
    return r;
}
```

### Frontend (`html/cliente.html` + `js/cliente.js`)
1. Nueva sección **"Consultar Disponibilidad"** con formulario:
   - Select `#sala`, input `#fecha` (`min=today`), inputs `#hora-inicio`, `#hora-fin`, botón **"Consultar"**.
2. Resultado se muestra debajo en un card:
   - ✅ **Disponible** → fondo `#1a2a1a`, borde `#00C851`, mensaje + botón **"Reservar ahora"** (autocompleta el formulario de reserva existente).
   - ❌ **No disponible** → fondo `#2a1a1a`, borde `#FF4444`, mensaje y NO botón de reserva.
3. Validaciones JS antes del fetch: campos requeridos + fecha no pasada + `horaInicio < horaFin`.
4. Reutilizar `pintarErroresCampo(err, mapeo)` con mapeo `{ horaInicio: 'hora-inicio', horaFin: 'hora-fin' }`.

---

## 📦 HU6 — CANCELACIÓN DE RESERVAS (Cliente)

### Backend

#### 1. Nuevo método en `Reserva.java` (Rich Domain Model)
```java
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
```

#### 2. Añadir campo a `Reserva.java`
```java
@Column(name = "fecha_cancelacion")
private LocalDateTime fechaCancelacion;
```

#### 3. Nuevo endpoint en `ReservaController.java`
```java
// CA-HU06-01, CA-HU06-02
@PatchMapping("/{id}/cancelar")
public ResponseEntity<?> cancelar(@PathVariable Long id, HttpSession session) {
    Usuario cliente = (Usuario) session.getAttribute("usuario");
    Reserva r = reservaRepo.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no existe"));
    r.cancelar(cliente);
    reservaRepo.save(r);
    return ResponseEntity.ok(Map.of(
        "mensaje", "Reserva cancelada correctamente",
        "id", r.getId(),
        "estado", r.getEstado()
    ));
}
```

#### 4. Manejo de `IllegalStateException` en `GlobalExceptionHandler`
- Si aún no existe, agregar handler que devuelva **409 Conflict** con `ApiError`.

### Frontend (`js/cliente.js`)
1. En la lista "Mis Reservas" añadir botón **"Cancelar"** SOLO si `estado === 'PENDIENTE' || estado === 'CONFIRMADA'`.
2. Al click → **modal de confirmación** Premium Dark Mode:
   > "¿Está seguro de cancelar esta reserva? Esta acción no se puede deshacer."
   > [Cancelar] [Sí, cancelar reserva]
3. Si confirma → `PATCH /api/reservas/{id}/cancelar` → toast verde + refrescar lista.
4. La fila cancelada se muestra en gris con etiqueta "CANCELADA" (`#888`).

---

## 📦 HU7 — REGISTRO DE INGRESO DE CLIENTES (Recepcionista)

### Backend

#### 1. Nuevo enum (si no existe)
- Asegurar que `EstadoReserva` tenga: `PENDIENTE`, `CONFIRMADA`, `EN_USO`, `FINALIZADA`, `CANCELADA`.
- Asegurar que `EstadoSala` tenga: `DISPONIBLE`, `RESERVADA`, `EN_USO`, `MANTENIMIENTO`.

#### 2. Nuevos métodos de dominio

**`Reserva.java`:**
```java
/** Ventana de tolerancia (minutos) para registrar entrada antes del inicio. */
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
```

**`Sala.java`:**
```java
/** CA-HU07-02: Cambio de estado RESERVADA -> EN_USO. */
public void marcarEnUso() {
    this.estado = EstadoSala.EN_USO;
}
```

**Nuevo campo en `Reserva.java`:**
```java
@Column(name = "fecha_ingreso")
private LocalDateTime fechaIngreso;
```

#### 3. Nuevo controller `RecepcionController.java`
```java
@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionController {

    private final ReservaRepository reservaRepo;

    /** CA-HU07-01, CA-HU07-04: busca reservas activas del dia por cliente. */
    @GetMapping("/buscar-reserva")
    public ResponseEntity<?> buscar(@RequestParam String correoCliente, HttpServletRequest req) {
        List<Reserva> reservas = reservaRepo
            .findReservasDelDiaPorCorreoCliente(correoCliente.trim().toLowerCase(), LocalDate.now());

        if (reservas.isEmpty()) {
            return ResponseEntity.status(404).body(ApiError.of(
                404, "No existe una reserva activa para este cliente hoy", req.getRequestURI()));
        }
        return ResponseEntity.ok(reservas.stream().map(ReservaResponse::de).toList());
    }

    /** CA-HU07-02, CA-HU07-03: registra ingreso. */
    @PatchMapping("/reservas/{id}/ingreso")
    public ResponseEntity<?> registrarIngreso(@PathVariable Long id) {
        Reserva r = reservaRepo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no existe"));
        r.registrarIngreso(LocalDateTime.now());
        reservaRepo.save(r);
        return ResponseEntity.ok(ReservaResponse.de(r));
    }
}
```

#### 4. Nuevo método en `ReservaRepository.java`
```java
@Query("""
   SELECT r FROM Reserva r
   WHERE LOWER(r.cliente.correo) = :correo
     AND r.fecha = :fecha
     AND r.estado IN (
        com.cleancodecrew.sweg.model.EstadoReserva.PENDIENTE,
        com.cleancodecrew.sweg.model.EstadoReserva.CONFIRMADA
     )
   ORDER BY r.horaInicio ASC
""")
List<Reserva> findReservasDelDiaPorCorreoCliente(@Param("correo") String correo,
                                                 @Param("fecha") LocalDate fecha);
```

#### 5. Actualizar `WebConfig.java` / `AuthInterceptor.java`
- Rutas `/api/recepcion/**` permitidas SOLO para rol **RECEPCIONISTA** (Admin opcionalmente).
- Mantener `/api/auth/**` excluido como ya está.

### Frontend (`html/recepcionista.html` + `js/recepcion.js`)

1. Nueva vista con dos paneles:
   - **Panel búsqueda**: input `#correo-cliente` + botón "Buscar Reserva".
   - **Panel resultados**: lista de reservas del día con cards:
     - Nombre cliente, sala, hora inicio – hora fin, estado.
     - Botón **"Registrar Ingreso"** habilitado solo si dentro de ventana.
2. Si backend devuelve 404 → toast rojo: "No existe una reserva activa para este cliente hoy" (**CA-HU07-04**).
3. Si backend devuelve 409 al registrar ingreso → toast naranja con el mensaje exacto (**CA-HU07-03**, ej. "Reserva inicia a las 15:00").
4. Al éxito → cambiar visualmente la card al estado verde (#00C851) con etiqueta "EN USO" y deshabilitar botón.

---

## 📁 ESTRUCTURA DE ARCHIVOS NUEVOS / MODIFICADOS

```
src/main/java/com/cleancodecrew/sweg/
├── controller/
│   ├── AuthController.java          (MOD: validar rolSeleccionado)
│   ├── ReservaController.java       (MOD: /disponibilidad, PATCH /cancelar)
│   ├── RecepcionController.java     (NEW)
│   └── GlobalExceptionHandler.java  (MOD: IllegalStateException -> 409)
├── dto/
│   ├── LoginRequest.java            (MOD: rolSeleccionado)
│   ├── DisponibilidadResponse.java  (NEW)
│   └── ReservaResponse.java         (MOD: agregar fechaIngreso, fechaCancelacion)
├── model/
│   ├── Reserva.java                 (MOD: cancelar, registrarIngreso, deConsulta, campos nuevos)
│   ├── Sala.java                    (MOD: marcarEnUso)
│   └── EstadoReserva.java           (verificar valores)
└── repository/
    └── ReservaRepository.java       (MOD: +2 queries)

src/main/resources/static/
├── html/
│   ├── login.html          (MOD: role-grid)
│   ├── cliente.html        (MOD: seccion disponibilidad + boton cancelar)
│   └── recepcionista.html  (NEW)
├── css/
│   └── style.css           (MOD: .role-card, .availability-card)
└── js/
    ├── auth.js             (MOD: enviar rolSeleccionado)
    ├── cliente.js          (MOD: disponibilidad + cancelacion)
    └── recepcion.js        (NEW)
```

---

## 🧪 MATRIZ DE TRAZABILIDAD CA → CÓDIGO

| HU | CA | Capa | Implementación |
|----|----|------|----------------|
| HU5 | CA-HU05-01 | Controller | `GET /api/reservas/disponibilidad` |
| HU5 | CA-HU05-02 | Domain | `Reserva.seSolapaCon` (negado) |
| HU5 | CA-HU05-03 | Domain | `Reserva.seSolapaCon` (positivo) |
| HU5 | CA-HU05-04 | DTO + JS | `@NotNull` + `pintarErroresCampo` |
| HU5 | CA-HU05-05 | Domain | `Reserva.validarFechaNoPasada` |
| HU6 | CA-HU06-01 | Domain | `Reserva.cancelar(cliente)` |
| HU6 | CA-HU06-02 | Frontend | modal + toast confirmación |
| HU7 | CA-HU07-01 | Repository | `findReservasDelDiaPorCorreoCliente` |
| HU7 | CA-HU07-02 | Domain | `Reserva.registrarIngreso` + `Sala.marcarEnUso` |
| HU7 | CA-HU07-03 | Domain | `Reserva.validarVentanaDeIngreso` |
| HU7 | CA-HU07-04 | Controller | `404` cuando lista vacía |

---

## 📤 FORMATO DE ENTREGA

Entrega los archivos en este orden estricto, cada uno con **bloque de código completo** (sin diffs parciales):

1. `model/Reserva.java`
2. `model/Sala.java`
3. `dto/LoginRequest.java`
4. `dto/DisponibilidadResponse.java`
5. `dto/ReservaResponse.java`
6. `repository/ReservaRepository.java`
7. `controller/AuthController.java`
8. `controller/ReservaController.java`
9. `controller/RecepcionController.java`
10. `controller/GlobalExceptionHandler.java`
11. `static/html/login.html`
12. `static/html/cliente.html`
13. `static/html/recepcionista.html`
14. `static/css/style.css` (solo el bloque añadido al final, no reemplazo total)
15. `static/js/auth.js`
16. `static/js/cliente.js`
17. `static/js/recepcion.js`

Al final, incluye una **tabla resumen de cambios** y la frase exacta:
> "Sprint 2 implementado. HU5, HU6 y HU7 integradas con trazabilidad CA → código. Selector de rol reactivado en login. Sin regresiones en Sprint 1."

---

## 🚫 PROHIBIDO

- ❌ Crear nuevas entidades JPA (todo se modela con `Usuario`, `Sala`, `Reserva`).
- ❌ Agregar `@Service` o "service layer". La lógica vive en entidades (Rich Domain).
- ❌ Modificar `pom.xml`, `application.properties`, `AuthInterceptor`, `WebConfig` (salvo el ajuste mínimo para `/api/recepcion/**`), `PasswordHasher`, `DataSeeder`.
- ❌ Cambiar la paleta Premium Dark Mode o tipografía.
- ❌ Romper endpoints de Sprint 1 (`/api/auth/*`, `/api/salas/*`, `POST /api/reservas`).
- ❌ Volver a redactar el `ApiError` — usar el que ya soporta `fields`.
- ❌ Usar JWT, WebSocket, scheduler ni librerías nuevas.