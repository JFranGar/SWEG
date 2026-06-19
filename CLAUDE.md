# CLAUDE.md

Instrucciones permanentes para trabajar en SWGEC.

## Contexto del proyecto

- Aplicación web de gestión de espacios de coworking.
- Stack principal: Spring Boot 4.0.6, Java 25, PostgreSQL y frontend Vanilla JS.
- El proyecto usa Maven Wrapper, no Maven instalado globalmente.
- En Windows, usa `mvnw.cmd`; en Unix o WSL, usa `./mvnw`.

## Arquitectura

- `src/main/java/com/cleancodecrew/sweg/config`: configuración, seguridad, inicialización y utilidades globales.
- `src/main/java/com/cleancodecrew/sweg/controller`: controladores HTTP y manejo de errores.
- `src/main/java/com/cleancodecrew/sweg/dto`: objetos de transporte para requests y responses.
- `src/main/java/com/cleancodecrew/sweg/model`: entidades y enums del dominio.
- `src/main/java/com/cleancodecrew/sweg/repository`: acceso a datos con Spring Data JPA.
- `src/main/resources/static`: frontend estático.
- `src/test/java`: pruebas automatizadas.

## Comandos útiles

- Compilar y validar tests: `./mvnw clean test`
- Solo compilar tests: `./mvnw test-compile`
- Ejecutar la app: `./mvnw spring-boot:run`
- Empaquetar: `./mvnw clean package`

## Convenciones de trabajo

- Mantén los cambios pequeños y alineados con la estructura existente.
- Prefiere arreglar la causa raíz antes que añadir parches superficiales.
- No cambies APIs públicas ni nombres de paquetes sin necesidad.
- Conserva el estilo actual de Spring Boot y del frontend estático.
- Si agregas validación o seguridad, comprueba que no rompa el flujo de login, reservas o gestión de salas.

## Reglas técnicas

- El objetivo del runtime es Java 25.
- Si actualizas configuración del build, deja el proyecto compilable con Maven Wrapper.
- No introduzcas dependencias nuevas sin justificación clara.
- Si cambias código de backend, verifica también la compatibilidad con el frontend estático.
- Si una modificación afecta persistencia o seguridad, valida el flujo completo con pruebas.

## Datos locales

- BD esperada: `swgec_db`.
- Usuarios demo documentados en `README.md`.
- La documentación visible del stack debe mantenerse sincronizada con la versión real del proyecto.

Analiza este proyecto completo como si fueras un arquitecto de software senior.

Contexto:
Es una aplicación Java/Spring Boot con PostgreSQL.
La base de datos se llama swgec_db.
La conexión debe mantenerse así:
- host: localhost
- puerto: 5432
- usuario: postgres
- contraseña: postgres

Objetivo:
Quiero desarrollar y corregir este proyecto de forma profesional.

Instrucciones:
1. Revisa los archivos .md ya que ahi se encuentran las correcciones solcitadas. 
2. Primero revisa la estructura del proyecto.
3. Identifica las tecnologías usadas.
4. Explica la arquitectura actual.
5. Detecta errores, riesgos o malas prácticas.
6. No modifiques ningún archivo todavía.
7. Al final dame un plan de trabajo ordenado por prioridad.
