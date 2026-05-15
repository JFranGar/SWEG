
# 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** y **Evaluador QA Universitario (ISO/IEC 25010)** para el proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** del equipo **Clean Code Crew**.

Vienes de:
- **Paso 1**: estructura.
- **Paso 2**: seguridad por `HttpSession` + `AuthInterceptor`.
- **Paso 3**: modelos JPA con Rich Domain Model.
- **Paso 4**: DTOs, Repositories y Controllers REST con códigos 400 / 401 / 403 / 409 / 423.

Ahora ejecutas el **Paso 5: Frontend Vanilla (HTML + CSS puro + JavaScript)** que consume la API del Paso 4 y completa Sprint 1.

---

# 📋 REGLAS NO NEGOCIABLES

- **100% Vanilla**: HTML5 + CSS puro + JavaScript ES6+ sin frameworks.
  - ❌ NO React, Angular, Vue, jQuery, Bootstrap, Tailwind, Alpine.
  - ❌ NO bundlers (Vite, Webpack), NO Thymeleaf, NO template engines.
- Archivos servidos por Spring Boot desde `src/main/resources/static/`.
- `fetch` SIEMPRE con `credentials: 'include'` (cookie `SWGEC_SESSION`).
- **Validación dual**: cliente (JS) + servidor (Bean Validation + entidad).
- **Defensa en profundidad**: aunque el servidor valide, el JS también valida y muestra mensajes inline + toasts.
- **DRY**: un único wrapper `api.js`; un único `guards.js` para proteger vistas.
- **Diseño**: Premium Dark Mode con acento dorado `#FFD700`, tipografía **Inter**, radio base `0.625rem`.

---

# 🎨 SISTEMA DE DISEÑO (TOKENS)

| Token | Valor |
|---|---|
| `--bg-base` | `#0A0A0A` |
| `--bg-panel` | `#161616` |
| `--bg-panel-alt` | `#111111` |
| `--border-subtle` | `#2a2a2a` |
| `--border-soft` | `#1e1e1e` |
| `--gold` | `#FFD700` |
| `--gold-dark` | `#C0A000` |
| `--gold-gradient` | `linear-gradient(135deg, #FFD700, #C0A000)` |
| `--text-primary` | `#f5f5f5` |
| `--text-muted` | `#9a9a9a` |
| `--state-pending` | `#888888` |
| `--state-confirmed` | `#4488FF` |
| `--state-active` | `#00C851` |
| `--state-cancelled` | `#FF4444` |
| `--font-base` | `'Inter', system-ui, sans-serif` |
| `--radius` | `0.625rem` |

Importar **Inter** desde Google Fonts en `theme.css`.

---

# 🎯 ARCHIVOS A GENERAR

Todos van en `src/main/resources/static/`.

## CSS (4 archivos)
1. `css/theme.css` — tokens + reset.
2. `css/components.css` — botones, inputs, cards, tablas, badges, toasts, spinner.
3. `css/login.css` — pantalla login.
4. `css/dashboard.css` — sidebar + main layout.

## HTML (5 archivos)
5. `index.html` — redirige a `/html/login.html`.
6. `html/login.html` — HU1.
7. `html/admin.html` — HU2.
8. `html/cliente.html` — HU4.
9. `html/recepcion.html` — placeholder Sprint 2.

## JavaScript (5 archivos)
10. `js/api.js` — wrapper `fetch` + sistema de toasts.
11. `js/guards.js` — protección de vistas por rol.
12. `js/auth.js` — HU1.
13. `js/admin.js` — HU2.
14. `js/cliente.js` — HU4.

---

# 📦 ESPECIFICACIONES DETALLADAS

## A. CSS

### A.1 `css/theme.css`
- Importar Inter (`@import url(...)`).
- Declarar TODAS las variables CSS de la tabla de tokens en `:root`.
- Reset universal: `* { box-sizing: border-box; margin: 0; padding: 0; }`.
- `html, body`: `min-height: 100vh`, `background: var(--bg-base)`, `color: var(--text-primary)`, `font-family: var(--font-base)`.
- Enlaces `<a>` → dorados.
- `::selection` → fondo dorado.

### A.2 `css/components.css`

**Componentes obligatorios:**
- `.btn`, `.btn-primary` (gradiente dorado), `.btn-secondary` (outline), `.btn-danger` (rojo outline).
  - Hover con `transform: translateY(-1px)` y sombra dorada en `.btn-primary`.
  - Estado `disabled` con opacidad 0.55.
- `.field`, `.field label` (uppercase, muted), `.input`, `.select`.
  - Focus → borde dorado + `box-shadow` dorado al 15%.
  - Clase `.error` → borde rojo (`--state-cancelled`).
  - `.field .error-msg` → texto rojo pequeño con `min-height: 14px` para evitar saltos.
- `.card` y `.card-title` (dorado).
- `.table` con `thead` muted y `tbody tr:hover` con fondo muy sutil.
- `.badge`, `.badge-pending`, `.badge-confirmed`, `.badge-active`, `.badge-cancelled` (con fondo translúcido del color y texto del color).
- `#toast-container` fijo abajo-derecha con `z-index: 9999`. `.toast` con animación `toastIn` y variantes `.success`, `.error`, `.info`.
- `.spinner` 14px con animación `spin` para los botones de loading.

### A.3 `css/login.css`
- `.login-shell` centrado vertical y horizontalmente.
- `.login-card` ancho máximo 420px, panel oscuro, padding 36px 32px.
- `.login-brand h1` con `background-clip: text` para el efecto dorado.
- `.login-brand p` muted, 13px.

### A.4 `css/dashboard.css`
- `.app` flex.
- `.sidebar` 240px, panel oscuro, borde derecho `--border-subtle`.
  - Soporte `.collapsed` → 72px y oculta `.nav-label` y `.brand-text`.
  - `.brand` con `.brand-dot` (gradiente dorado), `.brand-text` dorado.
  - `.nav a` con padding y radio; `.nav a.active` → fondo dorado al 8% + borde dorado al 25%.
  - `.sidebar-footer` con borde superior.
- `.main` flex 1, padding 28px 36px, overflow auto.
- `.main-header` con `h2` y `.user-chip` (píldora con borde).

---

## B. HTML

### B.1 `index.html` (raíz)
```html
<!DOCTYPE html>
<html><head><meta http-equiv="refresh" content="0; url=/html/login.html"></head>
<body></body></html>
```

### B.2 `html/login.html` (HU1)
**Estructura:**
- `<main class="login-shell">` con `<section class="login-card">`.
- Header con brand "SWGEC" y subtítulo.
- Form `#login-form` con `novalidate`:
  - Campo `#correo` (input type email) + `#err-correo`.
  - Campo `#contrasena` (input type password) + `#err-contrasena`.
  - Botón `#btn-login` `.btn-primary` ancho completo.
- Pie de form: texto muted con cuentas demo.
- `<div id="toast-container"></div>`.
- Scripts (en orden): `/js/api.js`, `/js/auth.js`.

### B.3 `html/admin.html` (HU2)
**Estructura:**
- `<div class="app">` con `<aside class="sidebar">` y `<main class="main">`.
- Sidebar: brand + nav con "Gestión de Salas" (activo) + footer con `#btn-logout`.
- Header: `<h2>Inventario de Salas</h2>` + chip con `#user-rol`.
- **Card "Formulario"**:
  - `#form-title` (cambia entre "Nueva Sala" y "Editar Sala #X").
  - `<form id="sala-form">`:
    - hidden `#sala-id`.
    - `#nombre` + `#err-nombre`.
    - `#tipo` (select con `<option value="">Seleccione...</option>`, REUNION, SEMINARIO, TRABAJO) + `#err-tipo`.
    - `#capacidad` (input number `min="1"`) + `#err-capacidad`.
    - Botones: `#btn-guardar` (primary) y `#btn-cancelar` (secondary, oculto por defecto).
- **Card "Salas registradas"**: `<table class="table">` con `thead` (Nombre, Tipo, Capacidad, Estado, Acciones) y `<tbody id="tabla-salas">`.
- Scripts: `/js/api.js`, `/js/guards.js`, `/js/admin.js`.

### B.4 `html/cliente.html` (HU4)
**Estructura:**
- Sidebar con "Nueva Reserva" (activo) + footer logout.
- Header con `<h2>Reservar Sala</h2>` + chip con `#user-correo`.
- **Card "Nueva Reserva"** (max-width 520px):
  - `<form id="reserva-form">`:
    - `#sala` (select cargado dinámicamente) + `#err-sala`.
    - `#fecha` (input type date) + `#err-fecha`.
    - Dos columnas: `#hora-inicio` + `#err-hora-inicio` y `#hora-fin` + `#err-hora-fin`.
    - Botón `#btn-reservar` primary.
- **Card "Mis Reservas"**: tabla con (Sala, Fecha, Inicio, Fin, Estado) y `<tbody id="tabla-reservas">`.
- Scripts: `/js/api.js`, `/js/guards.js`, `/js/cliente.js`.

### B.5 `html/recepcion.html` (placeholder Sprint 2)
- Sidebar simple + main con título "Panel Recepción" + mensaje "Disponible en Sprint 2".
- Botón `#btn-logout`.
- Script inline que llama `guard(['RECEPCIONISTA'])` y configura logout.

---

## C. JavaScript

### C.1 `js/api.js`

**Objeto `api`** (módulo IIFE) con métodos:
- `api.get(url)`
- `api.post(url, body)`
- `api.put(url, body)`
- `api.del(url)`

**Función interna `request(method, url, body)`:**
- `fetch` con `credentials: 'include'`, `Content-Type: application/json`.
- Si hay error de red → lanza `{ status: 0, message: 'Error de conexion con el servidor' }`.
- Si `res.status === 204` → retorna `null`.
- Intenta `res.json()` en try/catch para no romper en respuestas vacías.
- Si `!res.ok` → lanza `{ status, message: data?.error || 'Error HTTP <status>', data }`.

**Objeto `toast`** (IIFE) con:
- `toast.success(msg)` → 3500ms.
- `toast.error(msg)` → 4500ms.
- `toast.info(msg)` → 3500ms.
- Inserta `<div class="toast {tipo}">msg</div>` en `#toast-container` y lo elimina con `setTimeout`.
- Fallback a `alert()` si no existe el contenedor.

### C.2 `js/guards.js`

**Función global `async guard(rolesPermitidos)`:**
1. `const me = await api.get('/api/auth/me');`
2. Si `!rolesPermitidos.includes(me.rol)` → toast error + redirige a `/html/login.html` en 800ms y retorna `null`.
3. Si existe `#user-correo`, asigna `me.correo`; si existe `#user-rol`, asigna `me.rol`.
4. Si existe `#btn-logout`, conecta listener: `POST /api/auth/logout` y redirige al login.
5. Retorna `me`.
6. Si el `api.get` falla (no hay sesión) → redirige al login y retorna `null`.

### C.3 `js/auth.js` (HU1)

**Flujo IIFE:**
- Al cargar: intentar `GET /api/auth/me`. Si responde → ya hay sesión → redirigir por rol.
- Submit del form:
  1. Prevenir default.
  2. Limpiar errores visuales.
  3. **Validación cliente** [HU1 CA4]:
     - Correo vacío → mensaje.
     - Regex email simple `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`.
     - Contraseña vacía → mensaje.
  4. Botón → modo loading (`<span class="spinner"></span> Ingresando...`).
  5. `POST /api/auth/login` con body `{correo: lower.trim, contrasena}`.
  6. **Manejo de respuestas:**
     - `200`: toast success + redirigir por rol (`ADMIN→admin.html`, `RECEPCIONISTA→recepcion.html`, `CLIENTE→cliente.html`).
     - `401`: toast "Credenciales no validas" + clase `error` en ambos inputs [HU1 CA2].
     - `423`: toast con el mensaje del servidor (incluye minutos) [HU1 CA3].
     - `400`: toast con mensaje (validación servidor) [HU1 CA4].
     - Cualquier otro: toast genérico con `err.message`.
  7. Quitar modo loading en `finally`.

### C.4 `js/admin.js` (HU2)

**Flujo IIFE async:**
- `const me = await guard(['ADMIN']); if (!me) return;`
- Función `cargarSalas()`: `GET /api/admin/salas` y renderiza filas.
  - Si lista vacía → fila con texto muted "Sin salas registradas".
  - Cada fila: nombre, tipo, capacidad, badge de estado, botones **Editar** y **Eliminar**.
  - Usar `data-edit='{JSON}'` y `data-del='{id}'`.
- Función `escapar(s)` para evitar XSS al insertar HTML.
- Función `validar()` [HU2 CA4]: nombre no vacío, tipo seleccionado, capacidad > 0.
- Submit del form:
  1. Validar; si falla → return.
  2. Body: `{nombre, tipo, capacidadMaxima}`.
  3. Si `#sala-id` tiene valor → `PUT /api/admin/salas/{id}`; si no → `POST /api/admin/salas`.
  4. **Manejo de respuestas:**
     - `201/200`: toast success + `modoCreacion()` + `cargarSalas()`.
     - `409`: marcar `#nombre.error`, mostrar mensaje en `#err-nombre`, toast error [HU2 CA2].
     - `400`: toast con mensaje (Bean Validation o entidad) [HU2 CA4].
     - `401/403`: toast "Sesion expirada" + redirigir.
- Función `modoEdicion(s)`: rellena form + cambia título + muestra "Cancelar" + scroll al top.
- Función `modoCreacion()`: reset form + título "Nueva Sala" + oculta "Cancelar".
- Listener de `tbody` por delegación: `data-edit` → `modoEdicion`; `data-del` → confirm + `DELETE`.

### C.5 `js/cliente.js` (HU4)

**Flujo IIFE async:**
- `const me = await guard(['CLIENTE']); if (!me) return;`
- `inpFecha.min = new Date().toISOString().split('T')[0];` [HU4 CA5 — UX]
- Función `cargarSalas()`: `GET /api/cliente/reservas/salas-disponibles` → llenar select.
- Función `cargarMisReservas()`: `GET /api/cliente/reservas` → renderizar tabla con badges semánticos.
- Función `validar()`:
  - Todos los campos obligatorios [HU4 CA4].
  - Fecha no anterior a hoy [HU4 CA5].
  - `horaInicio >= horaFin` → error inline [HU4 CA3].
- Submit del form:
  1. Validar; si falla → return.
  2. Body: `{salaId: parseInt, fecha, horaInicio: 'HH:mm:00', horaFin: 'HH:mm:00'}`.
  3. Botón modo loading (spinner).
  4. `POST /api/cliente/reservas`.
  5. **Manejo de respuestas:**
     - `201`: toast success con `r.mensaje`, reset form, recargar `min` de fecha, recargar tabla.
     - `409`: error inline en `#err-sala` + toast [HU4 CA2].
     - `400`: toast con mensaje [HU4 CA3, CA4, CA5].
     - `401/403`: toast "Sesion expirada" + redirigir.
  6. Quitar loading en `finally`.

---

# 🧪 PRUEBAS QA OBLIGATORIAS (incluir al final)

Tabla con los 10 escenarios que el evaluador puede ejecutar manualmente:

| # | Escenario | HU/CA | Resultado esperado |
|---|---|---|---|
| 1 | Submit login vacío | HU1 CA4 | Errores rojos inline en ambos campos |
| 2 | Login con `admin@swgec.ec` / contraseña incorrecta | HU1 CA2 | Toast rojo "Credenciales no válidas" + inputs en error |
| 3 | 3 intentos fallidos seguidos | HU1 CA3 | Toast con "Cuenta bloqueada. Intentelo en X minutos." |
| 4 | Login correcto admin | HU1 CA1 | Redirige a `/html/admin.html` |
| 5 | Crear "Sala Quito" / REUNION / 10 | HU2 CA1 | Toast verde + aparece en tabla |
| 6 | Crear "sala quito" otra vez | HU2 CA2 | Toast rojo "Ya existe una sala con ese nombre" + `#nombre.error` |
| 7 | Submit admin con todos los campos vacíos | HU2 CA4 | Mensajes inline rojos |
| 8 | Cliente intenta reservar fecha anterior a hoy | HU4 CA5 | Input `min` bloquea; si manipula manualmente → 400 |
| 9 | Cliente con horaInicio=14:00, horaFin=12:00 | HU4 CA3 | Error inline + 400 si llega al servidor |
| 10 | Dos reservas del mismo horario en la misma sala | HU4 CA2 | Segunda → 409 + toast rojo |

---

# 🚫 PROHIBIDO EN ESTE PASO

- ❌ Usar librerías externas (jQuery, Lodash, etc.).
- ❌ Inline styles (excepto microajustes puntuales como `style="width:100%"`).
- ❌ `innerHTML` con datos de usuario SIN escapar (usar función `escapar`).
- ❌ Strings mágicos de roles ("admin", "Admin") — usar SIEMPRE mayúsculas (`'ADMIN'`).
- ❌ Crear archivos JS adicionales fuera de los 5 listados.
- ❌ Manejar autenticación con localStorage/sessionStorage (la sesión la maneja la cookie HttpOnly).

---

# 📤 FORMATO DE ENTREGA

Entrega la respuesta en este orden estricto:

1. **CSS**:
   - `css/theme.css` (```css)
   - `css/components.css`
   - `css/login.css`
   - `css/dashboard.css`
2. **HTML**:
   - `index.html`
   - `html/login.html`
   - `html/admin.html`
   - `html/cliente.html`
   - `html/recepcion.html`
3. **JavaScript**:
   - `js/api.js`
   - `js/guards.js`
   - `js/auth.js`
   - `js/admin.js`
   - `js/cliente.js`
4. **Tabla QA de los 10 escenarios** (la de arriba).
5. **Cumplimiento ISO 25010** — tabla resumen del Sprint 1 completo:

| Subcaracterística | Evidencia frontend |
|---|---|
| Seguridad / Confidencialidad | `credentials:'include'` + redirección por rol + `guards.js` |
| Fiabilidad / Tolerancia a fallos | Validación dual cliente + servidor |
| Usabilidad / Prevención de errores | `input[min=hoy]`, mensajes inline, toasts |
| Usabilidad / Estética | Premium Dark Mode, tipografía Inter, dorado #FFD700 |
| Mantenibilidad / Reusabilidad | `api.js`, `guard()`, sistema de tokens CSS |
| Mantenibilidad / Modularidad | 1 JS por HU, separación CSS por capa |

6. **Confirmación final**:
> "Paso 5 listo. Frontend Vanilla Premium Dark Mode integrado con la API. **Sprint 1 cerrado** — HU1, HU2, HU4 cumplen todos sus Criterios de Aceptación con validación dual y mapeo correcto de códigos HTTP. Listo para Sprint 2 (HU5 Disponibilidad, HU6 Cancelación, HU7 Check-in)."