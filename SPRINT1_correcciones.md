# Correcciones revisión Sprint 1 — SWGEC (Clean Code Crew, Grupo 4)

## 1. Backlog corregido

HU01 (Paraguas): Como usuario (Administrador / Recepcionista / Cliente) quiero iniciar sesión en el sistema, para acceder al panel correspondiente a mi rol.

- HU01-A: Login Administrador — Como Administrador quiero iniciar sesión con mis credenciales para gestionar el sistema.
- HU01-B: Login Recepcionista — Como Recepcionista quiero iniciar sesión con mis credenciales para gestionar reservas y atención.
- HU01-C: Login Cliente — Como Cliente quiero iniciar sesión con mis credenciales para reservar salas y ver mis reservas.

- HU02 (Paraguas): Como actor autorizado quiero gestionar el inventario de salas para mantener actualizado el catálogo de recursos.

- HU02-A: Registrar nueva sala — Como Administrador quiero registrar una nueva sala con nombre, tipo y capacidad máxima.
- HU02-B: Editar datos de sala — Como Administrador quiero editar el nombre, tipo y capacidad máxima de una sala existente.
- HU02-C: Eliminar sala — Como Administrador quiero eliminar una sala del inventario.
- HU02-D: Consultar listado de salas — Como cualquier usuario autorizado quiero consultar el listado de salas disponibles.

- HU04 (reescrita): Como Cliente, quiero seleccionar y reservar una sala de coworking especificando fecha y rango de horas, para garantizar la exclusividad del espacio durante mi jornada de trabajo.

> Nota: La regla anti-solapamiento queda como criterio de aceptación independiente `CA-HU04-02` (ver sección CA).

---

## 2. Criterios de Aceptación (CA)

- HU01 (Login, endpoint único `/api/auth/login`)
  - CA-HU01-01: El sistema autentica credenciales válidas y devuelve sesión/Token.
  - CA-HU01-02: Tras autenticación, el sistema redirige al panel correspondiente al rol del usuario.
  - CA-HU01-03: Intentos fallidos muestran mensaje genérico sin revelar existencia de usuario.

- HU02-A (Registrar)
  - CA-HU02-01: Al registrar sala, los campos `nombre`, `tipo`, `capacidadMaxima` son obligatorios y válidos.
  - CA-HU02-02: Validaciones por campo: `nombre` no vacío; `tipo` aceptado; `capacidadMaxima` > 0.
  - CA-HU02-03: Registro exitoso persiste la sala en el repositorio.

- HU02-B (Editar)
  - CA-HU02-04: Editar sala actualiza los campos permitidos y persiste cambios.
  - CA-HU02-05: No se permite dejar campos obligatorios vacíos.

- HU02-C (Eliminar)
  - CA-HU02-06: Eliminar sala remueve el registro si no tiene reservas activas.

- HU02-D (Consultar)
  - CA-HU02-07: Consultar devuelve listado paginado y filtrable por tipo.

- HU04 (Reservas)
  - CA-HU04-01: Reserva exitosa crea una reserva con estado CONFIRMADA.
  - CA-HU04-02: El sistema impide reservas solapadas para la misma sala (regla anti-solapamiento).
  - CA-HU04-03: Validación de fecha y hora (fecha en formato válido, horaInicio < horaFin, rango dentro horario operativo).

---

## 3. Matriz de trazabilidad (CA → TC → Tipo → Estado)

| CA | TC | Tipo | Estado |
|---|---|---:|---|
| CA-HU01-01 | TC-HU01-01-HP-01 | HP | Ready |
| CA-HU01-01 | TC-HU01-01-EC-01 | EC | Draft |
| CA-HU01-02 | TC-HU01-02-HP-01 | HP | Ready |
| CA-HU01-02 | TC-HU01-02-EC-01 | EC | Draft |
| CA-HU01-03 | TC-HU01-03-HP-01 | HP | Ready |
| CA-HU02-01 | TC-HU02-01-HP-01 | HP | Ready |
| CA-HU02-01 | TC-HU02-01-EC-01 | EC | Ready |
| CA-HU02-02 | TC-HU02-02-EC-01 | EC | Ready |
| CA-HU02-02 | TC-HU02-02-EC-02 | EC | Ready |
| CA-HU02-02 | TC-HU02-02-EC-03 | EC | Ready |
| CA-HU02-03 | TC-HU02-03-HP-01 | HP | Ready |
| CA-HU02-04 | TC-HU02-04-HP-01 | HP | Ready |
| CA-HU02-05 | TC-HU02-05-EC-01 | EC | Ready |
| CA-HU02-06 | TC-HU02-06-HP-01 | HP | Ready |
| CA-HU02-07 | TC-HU02-07-HP-01 | HP | Ready |
| CA-HU04-01 | TC-HU04-01-HP-01 | HP | Ready |
| CA-HU04-01 | TC-HU04-01-EC-01 | EC | Ready |
| CA-HU04-02 | TC-HU04-02-HP-01 | HP | Ready |
| CA-HU04-02 | TC-HU04-02-EC-01 | EC | Ready |
| CA-HU04-03 | TC-HU04-03-HP-01 | HP | Ready |
| CA-HU04-03 | TC-HU04-03-EC-01 | EC | Ready |

---

## 4. Casos de prueba nuevos (detallados)

Nota sobre precondición corregida: Reemplazar "Usuario logueado exitosamente" por la precondición específica de cada HU (ej.: "Ingresar al sistema con las credenciales de Administrador" para HU02; "Ingresar al sistema con las credenciales de Cliente" para HU04).

- TC-HU02-03-HP-01 (Editar sala — Happy Path)
  - Precondición: Ingresar al sistema con las credenciales de Administrador.
  - Pasos: 1) Ir a gestión de salas; 2) Seleccionar sala existente; 3) Modificar `nombre` y `capacidadMaxima`; 4) Guardar.
  - Resultado esperado: Los cambios se guardan y se muestran en el listado.

- TC-HU04-01-EC-01 (Reserva exitosa — Edge Case: reserva al límite horario)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: 1) Seleccionar sala; 2) Seleccionar fecha válida; 3) Seleccionar `horaInicio` = inicio del horario operativo, `horaFin` = fin del horario operativo; 4) Confirmar reserva.
  - Resultado esperado: Reserva aceptada si no hay solapamientos; sistema registra reserva en el límite horario.

- TC-HU04-03-HP-01 (Validación fecha/hora — Happy Path)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: 1) Seleccionar sala; 2) Ingresar fecha y `horaInicio` < `horaFin` dentro de horario operativo; 3) Confirmar.
  - Resultado esperado: Reserva creada correctamente.

Adicional — CA-HU02-02: casos Edge Case por campo (uno por cada campo obligatorio)

- TC-HU02-02-EC-01 (nombre vacío)
  - Precondición: Ingresar al sistema con las credenciales de Administrador.
  - Pasos: Intentar crear/editar sala con `nombre` vacío; guardar.
  - Resultado esperado: Validación falla y muestra mensaje "El nombre es obligatorio".

- TC-HU02-02-EC-02 (tipo inválido)
  - Precondición: Ingresar al sistema con las credenciales de Administrador.
  - Pasos: Intentar crear/editar sala con `tipo` no permitido; guardar.
  - Resultado esperado: Validación falla y muestra mensaje "Tipo de sala inválido".

- TC-HU02-02-EC-03 (capacidadMaxima no positiva)
  - Precondición: Ingresar al sistema con las credenciales de Administrador.
  - Pasos: Intentar crear/editar sala con `capacidadMaxima` = 0 o negativo; guardar.
  - Resultado esperado: Validación falla y muestra mensaje "Capacidad debe ser mayor que 0".

Adicional — CA-HU04-02: casos Edge Case por campo obligatorio (uno por cada campo)

- TC-HU04-02-EC-01 (sala no seleccionada)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: Intentar reservar sin seleccionar sala; confirmar.
  - Resultado esperado: Validación falla y muestra mensaje "Debe seleccionar una sala".

- TC-HU04-02-EC-02 (fecha inválida)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: Intentar reservar con fecha en formato inválido o en pasado; confirmar.
  - Resultado esperado: Validación falla y muestra mensaje "Fecha inválida".

- TC-HU04-02-EC-03 (horaInicio ausente/ inválida)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: Intentar reservar sin `horaInicio` o con formato inválido; confirmar.
  - Resultado esperado: Validación falla y muestra mensaje "Hora de inicio inválida".

- TC-HU04-02-EC-04 (horaFin ausente/ inválida o horaFin <= horaInicio)
  - Precondición: Ingresar al sistema con las credenciales de Cliente.
  - Pasos: Intentar reservar con `horaFin` vacío o `horaFin` <= `horaInicio`; confirmar.
  - Resultado esperado: Validación falla y muestra mensaje "Hora de fin inválida".

---

## 5. Decisiones de diseño (documentadas)

- Eliminación del paso "Seleccionar rol" en los casos de prueba: el sistema determina el rol por `Usuario.rol` en servidor y redirige automáticamente al panel correspondiente. Razonamiento: mayor seguridad (no exponer elección de rol en cliente), menor superficie de ataque, cumplimiento ISO/IEC 25010 en aspectos de Confidencialidad y Autenticación; principio KISS.

- Mantener HU01 unificada en backend: se conserva un único endpoint `/api/auth/login` que autentica credencialmente y devuelve rol en la sesión/Token. La separación en HU01-A/B/C es a nivel de backlog/producto para trazabilidad y pruebas, no a nivel de endpoints, para evitar endpoints redundantes y mantener diseño simple y seguro.

---

## 6. KPIs ISO 25010 (HU01) — Actualización de puntajes

| Métrica | Anterior | Nuevo |
|---|---:|---:|
| Completitud funcional | 4 | 5 |
| Capacidad para reconocer su adecuación | 4 | 5 |
| Responsabilidad (Seguridad) | 3 | 5 |

---

## 7. Confirmación final

Correcciones de revisión Sprint 1 aplicadas. Backlog, matriz de trazabilidad y casos de prueba alineados con el informe del Grupo 3. Sprint 1 cerrado con calidad ISO/IEC 25010.
