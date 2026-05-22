# 🛠️ PROMPT: CORRECCIONES POST-REVISIÓN — SPRINT 1 SWGEC

## 🎯 ROL Y CONTEXTO

Actúas como **Senior Software Architect** y **Evaluador QA Universitario (ISO/IEC 25010)** del proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** del equipo **Clean Code Crew (Grupo 4)**.

El Sprint 1 ya fue revisado por el Grupo 3 (revisores). Recibimos un **Informe de Revisión** con observaciones sobre **HU01, HU02 y HU04** que debes aplicar **antes del cierre formal del Sprint 1**.

Mantén intacto todo lo entregado en Pasos 1 a 5 (estructura, seguridad por HttpSession, Rich Domain Model, Controllers REST, Frontend Vanilla Premium Dark Mode). **Solo aplica los ajustes listados aquí.**

---

## 📌 OBSERVACIONES A CORREGIR

### Observación 1 — INVEST "Small": dividir historias multi-responsabilidad

**HU01 original:** "Como usuario (Admin/Recepcionista/Cliente) quiero iniciar sesión…"
👉 **Acción:** redactar 3 sub-historias por rol manteniendo el código HU01 como historia "paraguas":
- **HU01-A** Login Administrador.
- **HU01-B** Login Recepcionista.
- **HU01-C** Login Cliente.

**HU02 original:** "…quiero gestionar el inventario de salas…"
👉 **Acción:** dividir en 4 sub-historias CRUD:
- **HU02-A** Registrar nueva sala.
- **HU02-B** Editar datos de sala.
- **HU02-C** Eliminar sala.
- **HU02-D** Consultar listado de salas.

**HU04 original:** "…quiero reservar…para garantizar la exclusividad…y evitar reservas simultáneas…"
👉 **Acción:** reescribir el enunciado eliminando la lógica del sistema:
> "Como Cliente, quiero seleccionar y reservar una sala de coworking especificando fecha y rango de horas, para garantizar la exclusividad del espacio durante mi jornada de trabajo."
> La regla anti-solapamiento queda **únicamente como CA** (`CA-HU04-02`).

---

### Observación 2 — Códigos identificadores trazables

Todos los CA y casos de prueba deben llevar código. Usa el siguiente esquema y aplícalo tanto en Jira como en TestRail:

| Tipo | Patrón | Ejemplo |
|---|---|---|
| Historia | `HU{nn}` | `HU01` |
| Criterio de aceptación | `CA-HU{nn}-{kk}` | `CA-HU01-03` |
| Caso de prueba | `TC-HU{nn}-{kk}-{nn}` (HP = Happy Path, EC = Edge Case) | `TC-HU02-01-HP-01` |

Entrega una **tabla de trazabilidad** completa con tres columnas: `CA → TC → Estado`.

---

### Observación 3 — Precondiciones ambiguas

Reemplazar en TODOS los casos de prueba:

| Precondición ambigua | Precondición corregida |
|---|---|
| "Usuario logueado exitosamente" | "Ingresar al sistema con las credenciales de Administrador" (HU2) |
| "Ingresar al sistema con credenciales de usuario" | "Ingresar al sistema con las credenciales de Cliente" (HU4) |

---

### Observación 4 — Paso fantasma "Seleccionar rol"

Los casos **C51, C52, C61** incluyen el paso "Seleccionar rol", pero el sistema **no expone esa opción** porque el rol se infiere automáticamente del usuario autenticado (campo `Usuario.rol`).

👉 **Decisión arquitectónica:** se mantiene el comportamiento actual (rol implícito) por ser más seguro y KISS. **Eliminar el paso "Seleccionar rol" de C51, C52 y C61** y reemplazarlo por:
> "El sistema redirige automáticamente al panel correspondiente al rol del usuario."

Documenta esta decisión en una sección **"Decisiones de diseño"** del informe Sprint 1.

---

### Observación 5 — Cobertura: Happy Path + Edge Case por cada CA

Completar la matriz de pruebas garantizando que **cada CA tenga al menos 1 HP y 1 EC**.

Faltantes detectados por el revisor:

| CA | Falta | Crear |
|---|---|---|
| `CA-HU02-03` Editar sala | Happy Path | `TC-HU02-03-HP-01` |
| `CA-HU04-01` Reserva exitosa | Edge Case | `TC-HU04-01-EC-01` (ej. reserva justo al límite del horario laboral) |
| `CA-HU04-03` Validación fecha/hora | Happy Path | `TC-HU04-03-HP-01` |

---

### Observación 6 — Validación de campos obligatorios: granularidad

En `CA-HU02-02` y `CA-HU04-02` el revisor pide **un caso de prueba independiente por cada campo obligatorio** en lugar de un único caso "todos vacíos":

- HU02: `nombre`, `tipo`, `capacidadMaxima` → 3 EC adicionales.
- HU04: `sala`, `fecha`, `horaInicio`, `horaFin` → 4 EC adicionales.

---

## 📤 ENTREGABLES

1. **Backlog corregido** (markdown) con las nuevas historias HU01-A/B/C, HU02-A/B/C/D y HU04 reescrita.
2. **Tabla de Criterios de Aceptación** completa con códigos `CA-HU{nn}-{kk}`.
3. **Matriz de trazabilidad** `CA → TC → Tipo (HP/EC) → Estado`.
4. **Listado de casos de prueba nuevos** (los del punto Observación 5 y 6) con título, precondición corregida, pasos, resultado esperado.
5. **Sección "Decisiones de diseño"** justificando:
   - Por qué se elimina "Seleccionar rol" (Seguridad ISO 25010 — Confidencialidad y Autenticación: el rol se determina en el servidor, no en el cliente).
   - Por qué se mantiene HU01 unificada en backend (1 endpoint `/api/auth/login`) aunque se documente como 3 sub-historias (separación a nivel de producto, no de implementación → KISS).
6. **Tabla actualizada de KPIs ISO 25010** para HU01 reflejando los nuevos puntajes:
   - Completitud funcional: 4 → **5**
   - Capacidad para reconocer su adecuación: 4 → **5**
   - Responsabilidad (Seguridad): 3 → **5**
7. **Confirmación final** en una línea:
   > "Correcciones de revisión Sprint 1 aplicadas. Backlog, matriz de trazabilidad y casos de prueba alineados con el informe del Grupo 3. Sprint 1 cerrado con calidad ISO/IEC 25010."

---

## 🚫 NO HACER

- ❌ No modificar el código de backend ni frontend ya entregado en Pasos 1-5 salvo eliminar referencias al "selector de rol" si existieran en el HTML del login (no debería haber ninguna).
- ❌ No crear endpoints separados `/login-admin`, `/login-cliente`, `/login-recepcionista`. La división de HU01 es **a nivel de documentación de producto**, no de implementación.
- ❌ No agregar el botón "Seleccionar rol" al login: la decisión es eliminarlo de los test cases, no implementarlo.