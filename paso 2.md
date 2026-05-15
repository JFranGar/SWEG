# 🎯 ROL Y CONTEXTO

Actúa como un **Senior Software Architect** especializado en proyectos universitarios bajo **ISO/IEC 25010**. Continúas el desarrollo del proyecto **SWGEC (Sistema Web de Gestión de Espacios de CoWorking)** del equipo **Clean Code Crew**.

En el **Paso 1** ya creaste la estructura de carpetas y archivos placeholder. Ahora debes implementar el **Paso 2: Configuración de Seguridad y Sesión** SIN tocar todavía los modelos, repositorios ni controladores (esos van en pasos posteriores).

---

# 📋 STACK Y REGLAS DE ORO (RECORDATORIO)

- **Spring Boot 4.0.6**, **Java 21**, **Maven**, **PostgreSQL**.
- ❌ **NO usar Spring Security**. La autenticación y autorización se hacen manualmente con `HandlerInterceptor` + `HttpSession`.
- **Hashing manual**: SHA-256 + sal aleatoria por usuario.
- **Sesión vía cookie HttpOnly** llamada `SWGEC_SESSION`.
- **Frontend**: Vanilla JS usa `fetch` con `credentials: 'include'`.
- **Paquete base**: `com.cleancodecrew.sweg`.
- **KISS**: sin JWT, sin Redis, sin filters complejos, sin OAuth.
- **Rich Domain Model**: NO hay capa `service/`.

---

# 🎯 OBJETIVO DEL PASO 2

Implementar **completamente** los siguientes archivos:

| Archivo | Propósito |
|---|---|
| `pom.xml` | Dependencias mínimas (Web, JPA, PostgreSQL, Validation, Lombok). **SIN Spring Security**. |
| `application.properties` | Conexión PostgreSQL + configuración de `HttpSession` con cookie segura. |
| `SwegApplication.java` | Clase main de Spring Boot. |
| `config/AuthInterceptor.java` | Intercepta `/api/**` para validar sesión y roles. |
| `config/WebConfig.java` | Registra el `AuthInterceptor` excluyendo rutas públicas. |
| `config/PasswordHasher.java` | Componente Spring para hashear y verificar contraseñas (SHA-256 + sal). |
| `config/DataSeeder.java` | Crea 3 usuarios demo al levantar la app. |

---

# 📦 ESPECIFICACIONES DETALLADAS

## 1. `pom.xml`

- `parent`: `spring-boot-starter-parent` **4.0.6**.
- `java.version`: **21**.
- `groupId`: `com.cleancodecrew`
- `artifactId`: `sweg`
- `name`: `swgec`
- `description`: "Sistema Web de Gestion de Espacios de CoWorking"

**Dependencias (exactamente estas, ni una más):**
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `org.postgresql:postgresql` (runtime)
- `org.projectlombok:lombok` (optional)
- `spring-boot-starter-test` (scope test)

**Plugin**: `spring-boot-maven-plugin` con exclusión de Lombok.

❌ Prohibido: `spring-boot-starter-security`, `jjwt`, `springdoc`, `mapstruct`.

---

## 2. `application.properties`

```properties
# ====== Aplicacion ======
spring.application.name=swgec
server.port=8080

# ====== PostgreSQL ======
spring.datasource.url=jdbc:postgresql://localhost:5432/swgec_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# ====== JPA / Hibernate ======
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ====== Sesion HTTP (ISO 25010 - Seguridad / Confidencialidad) ======
server.servlet.session.timeout=30m
server.servlet.session.cookie.name=SWGEC_SESSION
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.same-site=lax
server.servlet.session.tracking-modes=cookie

# ====== Logging ======
logging.level.org.springframework.web=INFO
logging.level.com.cleancodecrew.sweg=DEBUG
```

> 📝 Nota: `cookie.secure=false` solo para desarrollo. En producción debe ser `true` (HTTPS).

---

## 3. `SwegApplication.java`

```java
package com.cleancodecrew.sweg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Sistema Web de Gestion de Espacios de CoWorking (SWGEC).
 * Equipo: Clean Code Crew.
 */
@SpringBootApplication
public class SwegApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwegApplication.class, args);
    }
}
```

---

## 4. `config/AuthInterceptor.java`

**Responsabilidad:** Interceptar TODA llamada a `/api/**` (excepto `/api/auth/**`) y verificar:

1. Si existe un atributo `usuarioId` en la `HttpSession` → caso contrario responde **401**.
2. Si existe atributo `rol` en sesión, valida que coincida con el rol permitido del path:
   - `/api/admin/**` → requiere rol `ADMIN`.
   - `/api/recepcion/**` → requiere rol `RECEPCIONISTA`.
   - `/api/cliente/**` → requiere rol `CLIENTE`.
   - Si no coincide → **403**.
3. Responde JSON uniforme: `{"status":401,"error":"No autenticado","path":"..."}`.

**Reglas:**
- Implementar `HandlerInterceptor` y sobreescribir `preHandle`.
- Marcar la clase con `@Component`.
- Usar `HttpServletResponse` para escribir JSON con `Content-Type: application/json`.
- NO usar Spring Security ni filters de bajo nivel.
- Log con SLF4J cuando rechace una petición (`DEBUG`).

**Constantes de claves de sesión** dentro de la misma clase:
```java
public static final String SESSION_USER_ID = "usuarioId";
public static final String SESSION_ROL     = "rol";
public static final String SESSION_NOMBRE  = "nombre";
public static final String SESSION_CORREO  = "correo";
```

---

## 5. `config/WebConfig.java`

**Responsabilidad:** Registrar el `AuthInterceptor`.

- Implementa `WebMvcConfigurer`.
- Anotada con `@Configuration`.
- Inyecta el `AuthInterceptor` por constructor.
- En `addInterceptors`:
  - Incluye: `/api/**`
  - Excluye: `/api/auth/**` (login, logout, me)
- NO redefinir `addResourceHandlers` (Spring sirve `static/` automáticamente).

---

## 6. `config/PasswordHasher.java`

**Responsabilidad:** Hashear y verificar contraseñas SIN Spring Security.

- Marcada `@Component`.
- Algoritmo: **SHA-256** con sal aleatoria de **16 bytes**.
- Formato de almacenamiento: `Base64(sal) + ":" + Base64(hash)`.
- Métodos públicos:

```java
public String hash(String plain);
public boolean matches(String plain, String stored);
```

- `hash`: genera sal con `SecureRandom`, concatena `sal + plain.getBytes(UTF_8)`, aplica `MessageDigest.getInstance("SHA-256")`, devuelve `Base64(sal):Base64(hash)`.
- `matches`: parsea el formato, recalcula el hash con la sal extraída y compara con `MessageDigest.isEqual` (tiempo constante).
- Lanzar `IllegalStateException` si SHA-256 no existe (nunca pasará en la JVM, pero por buena práctica).

> 📝 Justificación ISO 25010 — Seguridad/Integridad: sal por usuario + hash one-way + comparación a tiempo constante (resistente a timing attacks).

---

## 7. `config/DataSeeder.java`

**Responsabilidad:** Crear usuarios demo al levantar la aplicación, SOLO si la tabla está vacía.

- Implementa `CommandLineRunner`.
- Anotada con `@Component`.
- Inyecta `UsuarioRepository` y `PasswordHasher`.
- Si `usuarioRepository.count() == 0`, crea:

| Correo | Contraseña | Rol | Nombre |
|---|---|---|---|
| `admin@swgec.ec` | `admin123` | `ADMIN` | `Administrador SWGEC` |
| `recep@swgec.ec` | `recep123` | `RECEPCIONISTA` | `Recepcionista Demo` |
| `cliente@swgec.ec` | `cliente123` | `CLIENTE` | `Cliente Demo` |

- Usa `passwordHasher.hash(...)` antes de guardar.
- Log informativo: `"Usuarios demo creados: 3"`.

> ⚠️ Como `Usuario.java` y `UsuarioRepository.java` aún son placeholders del Paso 1, este `DataSeeder` puede dejarse **compilable pero con un comentario** indicando que las dependencias se resolverán en el Paso 3. Si el agente prefiere, puede dejar el cuerpo del método dentro de un `try/catch` o usar un `if (usuarioRepository != null)` defensivo. **Lo importante: el proyecto debe seguir compilando.**

---

# 🧪 VERIFICACIÓN POST-PASO 2

Después de generar el código, el agente debe poder responder afirmativamente a:

1. ¿El `pom.xml` NO incluye `spring-boot-starter-security`? ✅
2. ¿`application.properties` define cookie `SWGEC_SESSION` HttpOnly? ✅
3. ¿`AuthInterceptor` responde 401 JSON cuando no hay sesión? ✅
4. ¿`AuthInterceptor` responde 403 JSON cuando el rol no coincide? ✅
5. ¿`WebConfig` excluye `/api/auth/**`? ✅
6. ¿`PasswordHasher` usa sal aleatoria y comparación a tiempo constante? ✅
7. ¿El proyecto compila (`mvn clean compile`) sin errores? ✅

---

# 🚫 PROHIBIDO EN ESTE PASO

- ❌ Importar `org.springframework.security.*` en cualquier archivo.
- ❌ Usar `BCryptPasswordEncoder` o `PasswordEncoder` de Spring Security.
- ❌ Implementar JWT, OAuth, o tokens en headers.
- ❌ Crear clases nuevas fuera de las listadas (sin carpeta `service/`, `exception/`, `util/`).
- ❌ Tocar archivos del Paso 3 en adelante (entidades, DTOs, controllers).
- ❌ Modificar la estructura de carpetas del Paso 1.

---

# 📤 FORMATO DE ENTREGA

Entrega la respuesta en este orden estricto:

1. **`pom.xml`** completo dentro de un bloque ```xml.
2. **`application.properties`** completo dentro de un bloque ```properties.
3. **`SwegApplication.java`** completo dentro de un bloque ```java.
4. **`config/AuthInterceptor.java`** completo dentro de un bloque ```java.
5. **`config/WebConfig.java`** completo dentro de un bloque ```java.
6. **`config/PasswordHasher.java`** completo dentro de un bloque ```java.
7. **`config/DataSeeder.java`** completo dentro de un bloque ```java.
8. **Tabla de cumplimiento ISO 25010** (Seguridad, Mantenibilidad) explicando qué subcaracterística cubre cada archivo de este paso.
9. **Confirmación final**: "Paso 2 listo. Seguridad por HttpSession + Interceptor implementada SIN Spring Security. Listo para Paso 3 (Modelos JPA con Rich Domain Model)."