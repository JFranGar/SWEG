# 🎯 ROL Y CONTEXTO

Actúa como un **Senior Software Architect** especializado en proyectos universitarios bajo la norma **ISO/IEC 25010**. Tu trabajo es generar la estructura inicial de carpetas y archivos vacíos (placeholders) para el **Sprint 1** del proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** desarrollado por el equipo **Clean Code Crew**.

NO escribas todavía la lógica de las clases. Solo crea la **estructura del proyecto** y archivos vacíos con un comentario de cabecera que indique el propósito de cada uno.

---

# 📋 STACK TÉCNICO OBLIGATORIO

- **Backend:** Spring Boot 4.0.6, Java 21, Maven.
- **Persistencia:** Spring Data JPA + Hibernate + PostgreSQL.
- **Validación:** Bean Validation (`spring-boot-starter-validation`).
- **Lombok:** Sí.
- **Spring Security:** ❌ NO usar. La seguridad se hará manualmente con `HandlerInterceptor` + `HttpSession`.
- **Frontend:** 100% Vanilla — HTML5, CSS puro, JavaScript Vanilla.
  - ❌ NADA de React, Angular, Vue, Thymeleaf, JSP, ni bundlers.
  - Los archivos estáticos se sirven desde `src/main/resources/static/`.
- **Hashing:** Implementación manual SHA-256 + sal (clase `PasswordHasher`).

---

# 🧱 PRINCIPIOS ARQUITECTÓNICOS NO NEGOCIABLES

1. **KISS** — Sin microservicios, sin Redis, sin JWT, sin colas, sin Docker.
2. **Rich Domain Model** — La lógica de negocio vive en las **entidades JPA**, NO en services. Por eso **NO existe la carpeta `service/`**.
3. **DTOs obligatorios** — Para no exponer entidades JPA al frontend (Confidencialidad, ISO 25010).
4. **Enums tipados** — Para roles, tipos y estados (Fiabilidad, ISO 25010).
5. **Validación dual** — Cliente (JS) y servidor (Bean Validation + entidad).
6. **Un único wrapper** `api.js` para `fetch` (DRY).

---

# 📦 PAQUETE BASE

`com.cleancodecrew.sweg`

---

# 🌲 ESTRUCTURA EXACTA A GENERAR

Genera la siguiente estructura. Los archivos marcados con ⭐ son **prioritarios del Sprint 1** y deben crearse con un comentario de cabecera que indique:
- HU/CA que cubrirá.
- Propósito.

Los archivos sin ⭐ son placeholders para Sprints futuros y solo necesitan el comentario de propósito.

```
swgec/
├── pom.xml                                              ⭐
├── README.md                                            ⭐
├── .gitignore                                           ⭐
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── cleancodecrew/
        │           └── sweg/
        │               ├── SwegApplication.java         ⭐
        │               │
        │               ├── config/
        │               │   ├── AuthInterceptor.java     ⭐ (HU1 - Sesión y roles)
        │               │   ├── WebConfig.java           ⭐ (Registra el interceptor)
        │               │   ├── PasswordHasher.java      ⭐ (HU1 - Hash SHA-256 + salt)
        │               │   └── DataSeeder.java          ⭐ (Usuarios demo iniciales)
        │               │
        │               ├── model/
        │               │   ├── Usuario.java             ⭐ (HU1 CA1, CA2, CA3)
        │               │   ├── Sala.java                ⭐ (HU2 CA1, CA3, CA4)
        │               │   ├── Reserva.java             ⭐ (HU4 CA1-CA5)
        │               │   ├── Rol.java                 ⭐ (Enum: ADMIN, RECEPCIONISTA, CLIENTE)
        │               │   ├── TipoSala.java            ⭐ (Enum: REUNION, SEMINARIO, TRABAJO)
        │               │   ├── EstadoSala.java          ⭐ (Enum)
        │               │   └── EstadoReserva.java       ⭐ (Enum: PENDIENTE, CONFIRMADA, ACTIVA, CANCELADA, FINALIZADA)
        │               │
        │               ├── repository/
        │               │   ├── UsuarioRepository.java   ⭐
        │               │   ├── SalaRepository.java      ⭐
        │               │   └── ReservaRepository.java   ⭐
        │               │
        │               ├── dto/
        │               │   ├── LoginRequest.java        ⭐
        │               │   ├── SalaRequest.java         ⭐
        │               │   ├── ReservaRequest.java      ⭐
        │               │   └── ApiError.java            ⭐ (Respuesta de error uniforme)
        │               │
        │               └── controller/
        │                   ├── AuthController.java      ⭐ (HU1)
        │                   ├── SalaController.java      ⭐ (HU2 - /api/admin/salas)
        │                   ├── ReservaController.java   ⭐ (HU4 - /api/cliente/reservas)
        │                   └── GlobalExceptionHandler.java ⭐ (Bean Validation → JSON 400)
        │
        └── resources/
            ├── application.properties                   ⭐
            │
            └── static/
                ├── index.html                           ⭐ (Redirige a /html/login.html)
                │
                ├── html/
                │   ├── login.html                       ⭐ (HU1)
                │   ├── admin.html                       ⭐ (HU2)
                │   ├── cliente.html                     ⭐ (HU4)
                │   └── recepcion.html                   (Placeholder Sprint 2)
                │
                ├── css/
                │   ├── theme.css                        ⭐ (Tokens Premium Dark + dorado #FFD700)
                │   ├── components.css                   ⭐ (Botones, inputs, cards, toasts)
                │   ├── login.css                        ⭐
                │   └── dashboard.css                    ⭐ (Sidebar + main layout)
                │
                └── js/
                    ├── api.js                           ⭐ (fetch wrapper + toasts)
                    ├── guards.js                        ⭐ (Protección por rol en el cliente)
                    ├── auth.js                          ⭐ (HU1)
                    ├── admin.js                         ⭐ (HU2)
                    └── cliente.js                       ⭐ (HU4)
```

---

# 📝 CONTENIDO MÍNIMO DE CADA ARCHIVO PLACEHOLDER

Cada archivo `.java` debe contener únicamente:

```java
package com.cleancodecrew.sweg.<subpaquete>;

/**
 * <NombreClase>
 *
 * Propósito: <una línea>.
 * Sprint 1 - HU<N> CA<N>: <descripción si aplica>.
 *
 * TODO: implementar en el Paso <N>.
 */
public class <NombreClase> {
}
```

Cada archivo `.html` debe contener únicamente:

```html
<!-- <nombre>.html - Placeholder Sprint 1 - HU<N> -->
<!-- TODO: implementar en el Paso 5 -->
```

Cada archivo `.css` y `.js` debe contener únicamente:

```css
/* <nombre>.css - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */
```

---

# 📄 ARCHIVOS QUE SÍ DEBES POBLAR COMPLETAMENTE EN ESTE PASO

Solo los siguientes archivos llevan contenido real desde el Paso 1:

### 1. `.gitignore`
```
target/
*.class
*.jar
.idea/
.vscode/
*.iml
.DS_Store
application-local.properties
```

### 2. `README.md`
Debe incluir:
- Título: **SWGEC — Sistema Web de Gestión de Espacios de CoWorking**
- Equipo: **Clean Code Crew**
- Stack: Spring Boot 4.0.6 + Java 21 + PostgreSQL + Vanilla JS
- Sprint actual: **Sprint 1 — HU1 Login, HU2 Gestión de Salas, HU4 Reserva**
- Sección "Cómo correr":
  1. Crear BD: `CREATE DATABASE swgec_db;`
  2. Configurar `application.properties`
  3. `mvn spring-boot:run`
  4. Abrir `http://localhost:8080`
- Sección "Usuarios demo":
  - `admin@swgec.ec` / `admin123`
  - `recep@swgec.ec` / `recep123`
  - `cliente@swgec.ec` / `cliente123`
- Cumplimiento ISO 25010: Seguridad, Mantenibilidad, Fiabilidad, Usabilidad.

---

# ✅ CRITERIOS DE ACEPTACIÓN DE ESTE PASO

1. La estructura de carpetas coincide **exactamente** con el árbol mostrado.
2. **NO** existe carpeta `service/` ni `util/` ni `exception/`.
3. **NO** se importa Spring Security en ningún archivo.
4. Todos los archivos `.java` están en el paquete correcto (`com.cleancodecrew.sweg.<subpaquete>`).
5. Cada placeholder lleva el comentario de cabecera con HU/CA.
6. `README.md` y `.gitignore` están completos.
7. El proyecto compila aún cuando todas las clases estén vacías (deben tener al menos `public class X {}`).

---

# 🚫 PROHIBIDO EN ESTE PASO

- No escribir lógica de negocio.
- No declarar campos, anotaciones JPA, ni métodos (excepto la firma vacía de la clase).
- No incluir Spring Security en `pom.xml`.
- No crear archivos extra que no estén en el árbol.
- No usar Thymeleaf, JSP ni template engines.

---

# 📤 FORMATO DE ENTREGA

Entrega la respuesta en este orden:

1. **Árbol final** generado (tipo `tree`).
2. **Contenido completo** del `.gitignore`.
3. **Contenido completo** del `README.md`.
4. **Lista de los archivos placeholder creados** con su comentario de cabecera, agrupados por carpeta.
5. **Confirmación final**: "Paso 1 listo. Estructura creada según ISO 25010. Listo para Paso 2 (pom.xml + application.properties + AuthInterceptor)."