# Sprint 1 — Historias de Usuario y Criterios de Aceptación corregidos

> **Propósito:** documento de apoyo para transcribir a **Jira** (HU/CA) y **TestRail** (casos de prueba) las correcciones solicitadas en el *Informe Sprint 1*. **No** modifica el código: es el insumo QA para cerrar las observaciones de proceso.
>
> **Observaciones del informe que se resuelven aquí:**
> 1. INVEST *Small*: dividir HU que agrupan varias responsabilidades (HU01 por rol, HU02 por operaciones CRUD).
> 2. HU04: mover la validación de simultaneidad (lógica del sistema) desde la narrativa a un criterio de aceptación.
> 3. Precondiciones ambiguas ("usuario logueado"): especificar el rol requerido.
> 4. Falta de códigos identificadores que vinculen cada caso de prueba con su criterio de aceptación (trazabilidad).
> 5. Balance de cobertura: cada CA debe tener al menos un *happy path* y un *edge case*.

## 1. Convención de códigos (trazabilidad)

| Elemento | Formato | Ejemplo |
|----------|---------|---------|
| Historia de Usuario | `HU<nn>-<SUFIJO>` | `HU02-REG` |
| Criterio de Aceptación | `CA-<HU>-<nn>` | `CA-HU02-REG-01` |
| Caso de Prueba | `TC-<HU>-<nn>` | `TC-HU02-REG-01` |

En Jira, cada CA se etiqueta con su código; en TestRail, cada caso de prueba referencia en su título/campo personalizado el código del CA que cubre. Así, un CA y sus casos comparten el prefijo y se relacionan de un vistazo.

---

## 2. HU01 — Inicio de Sesión (dividida por rol)

**Original (una sola HU con 3 roles):** *"Como usuario del sistema (Administrador, Recepcionista o Cliente), quiero iniciar sesión con mi correo y contraseña, para acceder de forma segura al panel que corresponde a mi nivel de permisos."*

**Corrección (INVEST *Small*): separar en tres historias**, porque cada panel tiene un propósito distinto.

### HU01-ADM — Login de Administrador
*Como **Administrador**, quiero iniciar sesión con mi correo y contraseña, para acceder al **panel de administración** (gestión de salas, usuarios e invitaciones).*

| CA | Descripción | Precondición (rol explícito) |
|----|-------------|------------------------------|
| CA-HU01-ADM-01 | Con credenciales válidas de Administrador, el sistema concede acceso y redirige al panel de administración. | Ingresar con credenciales de **Administrador**. |
| CA-HU01-ADM-02 | Con credenciales inválidas, el sistema niega el acceso y muestra un mensaje de error. | Cuenta de **Administrador** existente. |
| CA-HU01-ADM-03 | Tras 3 intentos fallidos consecutivos, la cuenta se bloquea temporalmente (15 min). | Cuenta de **Administrador** existente. |

### HU01-REC — Login de Recepcionista
*Como **Recepcionista**, quiero iniciar sesión con mi correo y contraseña, para acceder al **panel de recepción** (vista del día y control de accesos).*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU01-REC-01 | Con credenciales válidas de Recepcionista, se concede acceso al panel de recepción. | Ingresar con credenciales de **Recepcionista**. |
| CA-HU01-REC-02 | Credenciales inválidas → acceso denegado con mensaje. | Cuenta de **Recepcionista** existente. |

### HU01-CLI — Login de Cliente
*Como **Cliente**, quiero iniciar sesión con mi correo y contraseña, para acceder al **panel de cliente** (buscar y reservar salas).*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU01-CLI-01 | Con credenciales válidas de Cliente, se concede acceso al panel de cliente. | Ingresar con credenciales de **Cliente**. |
| CA-HU01-CLI-02 | Credenciales inválidas → acceso denegado con mensaje. | Cuenta de **Cliente** existente. |

> **Sobre el paso "Seleccionar rol"** (observación del informe: aparecía en C51/C52/C61 pero no estaba implementado): **ya está implementado y es coherente**. El login presenta un selector de rol (tarjetas ADMIN/RECEPCIONISTA/CLIENTE) y el backend valida en `CA-HU01-*-01` que el rol seleccionado coincida con el de la cuenta (rechazo con HTTP 403 si no coincide). En Sprint 3 este selector se hizo accesible (`role="radiogroup"` + teclado).

---

## 3. HU02 — Gestión de Salas (dividida en operaciones CRUD)

**Original:** *"Como Administrador, quiero gestionar el inventario de salas detallando nombre, tipo y capacidad máxima..."* — "Gestionar" engloba ≥4 responsabilidades.

**Corrección (INVEST *Small*): dividir en cuatro historias.**

### HU02-REG — Registrar sala
*Como Administrador, quiero registrar una sala nueva con nombre, tipo y capacidad máxima, para incorporarla al inventario.*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU02-REG-01 | Con datos válidos, la sala se registra y aparece en el listado. | Ingresar con credenciales de **Administrador**. |
| CA-HU02-REG-02 | Se rechaza el registro si falta un campo obligatorio (nombre, tipo o capacidad). | Ingresar con credenciales de **Administrador**. |
| CA-HU02-REG-03 | Se rechaza un nombre de sala duplicado. | Ingresar con credenciales de **Administrador**. |

### HU02-EDI — Editar sala
*Como Administrador, quiero editar los datos de una sala existente, para mantener la información actualizada.*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU02-EDI-01 | Con datos válidos, los cambios se guardan correctamente. | Ingresar con credenciales de **Administrador**; existe al menos una sala. |
| CA-HU02-EDI-02 | Se rechaza la edición que deje un campo obligatorio vacío. | Ingresar con credenciales de **Administrador**; existe al menos una sala. |

### HU02-ELI — Eliminar sala
*Como Administrador, quiero dar de baja (lógica) una sala, para retirarla de la oferta sin perder el historial de reservas.*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU02-ELI-01 | La sala pasa a estado ELIMINADA y deja de ofrecerse, conservando su historial. | Ingresar con credenciales de **Administrador**; existe al menos una sala. |

### HU02-CON — Consultar salas
*Como Administrador, quiero consultar el listado de salas registradas, para revisar el inventario.*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU02-CON-01 | El listado muestra las salas no eliminadas con su estado actual. | Ingresar con credenciales de **Administrador**. |

---

## 4. HU04 — Reserva de Sala (narrativa corregida)

**Original (mezcla acción del cliente + lógica del sistema):** *"...para garantizar la exclusividad del espacio físico durante mi jornada de trabajo **y evitar que el sistema permita reservas simultáneas en el mismo horario**."*

**Corrección (una responsabilidad; la validación pasa a ser CA):**

> *Como **Cliente**, quiero seleccionar y reservar una sala de coworking especificando **fecha y rango de horas**, para garantizar la exclusividad del espacio durante mi jornada de trabajo.*

| CA | Descripción | Precondición |
|----|-------------|--------------|
| CA-HU04-01 | Con datos válidos, la reserva se crea y queda asociada al cliente. | Ingresar con credenciales de **Cliente**. |
| CA-HU04-02 | El sistema **rechaza reservas que se solapen** con otra reserva activa de la misma sala en el mismo horario *(antes estaba en la narrativa de la HU)*. | Ingresar con credenciales de **Cliente**; existe una reserva previa. |
| CA-HU04-03 | Se rechaza un rango horario inválido (inicio ≥ fin). | Ingresar con credenciales de **Cliente**. |
| CA-HU04-04 | Se rechaza la reserva si falta un campo obligatorio. | Ingresar con credenciales de **Cliente**. |
| CA-HU04-05 | Se rechazan reservas en fechas pasadas. | Ingresar con credenciales de **Cliente**. |

---

## 5. Matriz de trazabilidad CA ↔ Casos de prueba

Cada CA cuenta con al menos un **happy path (HP)** y un **edge case (EC)**, resolviendo la observación de balance de cobertura. La columna "TestRail original" mapea los casos previos (C51–C66) del informe.

| Criterio de Aceptación | Caso de Prueba | Tipo | TestRail original |
|------------------------|----------------|------|-------------------|
| CA-HU01-ADM-01 | TC-HU01-ADM-01: login con credenciales válidas de Admin | HP | C51 |
| CA-HU01-ADM-02 | TC-HU01-ADM-02: login con contraseña incorrecta | EC | C53 |
| CA-HU01-ADM-03 | TC-HU01-ADM-03: bloqueo tras 3 intentos fallidos | EC | — (nuevo) |
| CA-HU01-CLI-01 | TC-HU01-CLI-01: login con credenciales válidas de Cliente | HP | C61 |
| CA-HU01-CLI-02 | TC-HU01-CLI-02: login con correo inexistente | EC | C52 |
| CA-HU02-REG-01 | TC-HU02-REG-01: registrar sala con datos válidos | HP | C55 |
| CA-HU02-REG-02 | TC-HU02-REG-02: registrar sala con campo vacío | EC | C66 |
| CA-HU02-REG-03 | TC-HU02-REG-03: registrar sala con nombre duplicado | EC | — (nuevo) |
| CA-HU02-EDI-01 | TC-HU02-EDI-01: editar sala con datos válidos | HP | — (nuevo, *happy path* faltante) |
| CA-HU02-EDI-02 | TC-HU02-EDI-02: editar dejando un campo vacío | EC | C65 |
| CA-HU04-01 | TC-HU04-01: reserva con datos válidos | HP | C56 |
| CA-HU04-02 | TC-HU04-02: reserva solapada rechazada | EC | C57 |
| CA-HU04-03 | TC-HU04-03: rango horario inválido | EC | C64 |
| CA-HU04-04 | TC-HU04-04: campo obligatorio ausente | EC | C58 |
| CA-HU04-05 | TC-HU04-05: fecha pasada rechazada | EC | C59 |

> Varias de estas reglas ya cuentan con **verificación automatizada** en la Fase 6 del plan (`ReservaValidacionTest`, `SalaTest`, `UsuarioTest`, `PasswordHasherTest`), lo que refuerza la trazabilidad entre CA y prueba ejecutable.

---

## 6. Checklist de transcripción a Jira/TestRail

- [ ] Crear en Jira las HU divididas (HU01-ADM/REC/CLI, HU02-REG/EDI/ELI/CON) y actualizar la narrativa de HU04.
- [ ] Registrar cada CA con su código identificador.
- [ ] En TestRail, renombrar/crear los casos de prueba con el código `TC-*` y enlazarlos al CA correspondiente.
- [ ] Reemplazar toda precondición "usuario logueado exitosamente" por "Ingresar con credenciales de **&lt;rol&gt;**".
- [ ] Verificar que cada CA tenga al menos 1 HP y 1 EC.
