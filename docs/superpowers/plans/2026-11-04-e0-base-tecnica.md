# É0 — Base técnica (H0.1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Levantar todo el stack (Angular + Spring Boot + Postgres de Supabase + postgres-meta + Studio) con `docker compose up -d --build`, configurado 100% desde `.env`, con los datos persistidos en un volumen definido en `.env` y la infraestructura común de errores del backend lista para las épicas siguientes.

**Architecture:**
- El navegador habla solo con nginx (frontend).
- nginx sirve la SPA de Angular y hace proxy de `/api/**` al backend Spring Boot (`API_BASE_URL`).
- El backend se conecta por JDBC al Postgres de Supabase, y Flyway crea el esquema `${APP_DB_SCHEMA}`.
- Studio y postgres-meta son solo para administrar la base.
- Todo error de la API sale por un único `GlobalExceptionHandler` con un `ErrorResponse` uniforme.

**Tech Stack:**
- Java 21, Spring Boot 3.5.6 (web, data-jpa, validation, actuator), Flyway, PostgreSQL driver.
- JUnit 5 y Testcontainers.
- Angular (CLI `latest`), nginx 1.27-alpine, Node 22.
- Docker Compose v2, imágenes de Supabase (`supabase/postgres`, `supabase/postgres-meta`, `supabase/studio`).

**Spec:** `docs/superpowers/specs/2026-11-04-gestion-terapia-ocupacional-design.md` (historia H0.1 y sección 4). Reglas del proyecto: `CLAUDE.md`.

## Prerequisites

- **JDK 21** instalado y activo (`java -version` debe mostrar 21). La máquina hoy tiene JDK 17: instalar Eclipse Temurin 21 y apuntar `JAVA_HOME` a él antes de la Task 2.
- Docker Desktop corriendo (Testcontainers lo usa en los tests del backend).
- Node 22+ y npm (ya instalados).
- Esta épica **no tiene pantallas**: la regla de mockups no la bloquea. El frontend queda como un shell vacío (solo `<router-outlet>`), sin diseño de pantallas.

## Global Constraints

- Una sola fuente de verdad: `.env` en la raíz. Prohibido hardcodear credenciales, puertos, hosts, URLs o nombres de volúmenes en código, `application.yml`, Dockerfiles o `docker-compose.yml`.
- Toda variable nueva se agrega a `.env` **y** a `.env.example` en el mismo cambio. `.env` nunca se commitea.
- Las variables obligatorias se interpolan en compose con `${VAR:?VAR is required}`, para que falte lo que falte, falle fuerte.
- Prohibido `import *` (también en `import static`).
- Sin constantes sueltas: los valores fijos van en `enum`. La única excepción aceptada es el `Logger` estándar (`private static final Logger`).
- Excepciones: base abstracta `ApplicationException`, una clase por excepción, código de `enum ErrorCode` (mensaje + estado HTTP), único `GlobalExceptionHandler`. Al cliente nunca le llega un stack trace ni un mensaje interno.
- UI en español; código, identificadores y commits en inglés.
- Paquete raíz del backend: `com.tomanagement`.
- TDD: test que falla → implementación mínima → test en verde → commit.
- Cada commit termina con la línea `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **URL inexistente** (`GET /api/no-existe`) → 404 con `code=ENDPOINT_NOT_FOUND`, nunca 500. Lo cubre el test `unknownEndpointReturns404` (Task 3) y la verificación por proxy en la Task 6.
2. **Request mal formado** (JSON roto, path variable de tipo incorrecto, falta un parámetro, Content-Type no soportado) → 400 o 415 con código propio, nunca 500. Tests en la Task 3.
3. **Excepción inesperada** con datos sensibles en el mensaje → 500 genérico, sin el mensaje original ni el nombre de la clase. Lo cubre el test `unexpectedExceptionIsTranslatedToGenericInternalError` (Task 3).
4. **Refrescar la SPA en una ruta profunda** (`/pacientes/123`) → nginx devuelve `index.html` (200), no un 404. Verificación en la Task 6.
5. **Variable obligatoria vacía o ausente** → `docker compose config` falla con un mensaje claro, en lugar de levantar con valores vacíos. Verificación en la Task 6.

## File Structure

```
.gitignore                         Ignora .env, builds y dependencias
.env                               Valores reales (no se commitea)
.env.example                       Plantilla sin secretos
docker-compose.yml                 db, meta, studio, backend, frontend + volumen
CLAUDE.md                          (modificado) aclaración del volumen y variables nuevas
backend/
  pom.xml
  mvnw, mvnw.cmd, .mvn/            Maven wrapper
  Dockerfile, .dockerignore
  src/main/java/com/tomanagement/
    ToManagementApplication.java
    common/config/ClockConfig.java
    common/exception/ErrorCode.java
    common/exception/ApplicationException.java
    common/exception/ResourceNotFoundException.java
    common/exception/DuplicateResourceException.java
    common/exception/BusinessRuleException.java
    common/exception/AuthenticationFailedException.java
    common/exception/AccessDeniedApplicationException.java
    common/exception/InternalSystemException.java
    common/exception/dto/ErrorResponse.java
    common/exception/dto/FieldErrorResponse.java
    common/exception/handler/GlobalExceptionHandler.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_app_schema.sql
  src/test/java/com/tomanagement/
    ApplicationStartupTest.java
    TestcontainersConfiguration.java
    TestContainerImage.java
    common/exception/handler/GlobalExceptionHandlerTest.java
    common/exception/handler/ExceptionProbeController.java
  src/test/resources/application-test.yml
frontend/                          Generado por Angular CLI
  src/app/app.ts, app.html, app.scss, app.spec.ts   (reemplazados: shell vacío)
  src/styles.scss                  (reemplazado: base mobile-first)
  nginx/default.conf.template
  Dockerfile, .dockerignore
```

---

### Task 1: Repositorio, `.env` y `.env.example`

**Files:**
- Create: `.gitignore`, `.env`, `.env.example`
- Modify: `CLAUDE.md` (tabla de variables de la sección 6)

**Interfaces:**
- Produces: las variables que usan todas las tareas siguientes: `COMPOSE_PROJECT_NAME`, `SUPABASE_POSTGRES_IMAGE`, `SUPABASE_PG_META_IMAGE`, `SUPABASE_STUDIO_IMAGE`, `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_HOST_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_DB_SCHEMA`, `SUPABASE_DB_VOLUME_NAME`, `SUPABASE_DB_DATA_PATH`, `PG_META_PORT`, `STUDIO_PORT`, `STUDIO_CONTAINER_PORT`, `STUDIO_ORGANIZATION_NAME`, `STUDIO_PROJECT_NAME`, `STUDIO_SUPABASE_URL`, `STUDIO_ANON_KEY`, `STUDIO_SERVICE_KEY`, `BACKEND_PORT`, `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`, `FRONTEND_PORT`, `API_BASE_URL`.

**Decisión sobre el volumen:** en Docker Desktop para Windows, un bind mount de una carpeta de Windows sobre los datos de Postgres falla por permisos (`chmod` sobre NTFS). Por eso el volumen es un **volumen nombrado de Docker**:
- `SUPABASE_DB_VOLUME_NAME` define su nombre.
- `SUPABASE_DB_DATA_PATH` define la ruta de montaje dentro del contenedor.

Ambos salen del `.env`, que es lo que exige CLAUDE.md.

- [ ] **Step 1: Inicializar git**

```bash
cd "/c/Users/Maxi/Desktop/TO Managment"
git init
git branch -M main
```

- [ ] **Step 2: Crear `.gitignore`**

```gitignore
# Environment (secrets)
.env

# Backend
backend/target/

# Frontend
frontend/node_modules/
frontend/dist/
frontend/.angular/

# IDE / OS
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
*.log
```

- [ ] **Step 3: Crear `.env.example`**

```dotenv
# ---------- Docker ----------
COMPOSE_PROJECT_NAME=to-management

# ---------- Supabase images ----------
SUPABASE_POSTGRES_IMAGE=supabase/postgres:15.8.1.060
SUPABASE_PG_META_IMAGE=supabase/postgres-meta:v0.91.0
SUPABASE_STUDIO_IMAGE=supabase/studio:2025.06.30-sha-6f5982d

# ---------- Database ----------
POSTGRES_HOST=db
POSTGRES_PORT=5432
POSTGRES_HOST_PORT=5432
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
APP_DB_SCHEMA=app
SUPABASE_DB_VOLUME_NAME=to-management-db-data
SUPABASE_DB_DATA_PATH=/var/lib/postgresql/data

# ---------- Supabase Studio / postgres-meta (solo administración) ----------
PG_META_PORT=8081
STUDIO_PORT=3001
STUDIO_CONTAINER_PORT=3000
STUDIO_ORGANIZATION_NAME=TO Management
STUDIO_PROJECT_NAME=TO Management
STUDIO_SUPABASE_URL=http://localhost
STUDIO_ANON_KEY=not-used
STUDIO_SERVICE_KEY=not-used

# ---------- Backend ----------
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=default
JWT_SECRET=change-me-to-a-64-char-random-hex-string
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:4200
ADMIN_EMAIL=admin@tomanagement.local
ADMIN_PASSWORD=change-me
ADMIN_NAME=Administradora

# ---------- Frontend ----------
FRONTEND_PORT=8090
API_BASE_URL=http://backend:${BACKEND_PORT}
```

- [ ] **Step 4: Crear `.env` con secretos generados**

Copiar `.env.example` a `.env` y reemplazar `POSTGRES_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD` por valores aleatorios:

```bash
cp .env.example .env
sed -i "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(openssl rand -hex 16)|" .env
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -hex 32)|" .env
sed -i "s|^ADMIN_PASSWORD=.*|ADMIN_PASSWORD=$(openssl rand -hex 12)|" .env
grep -c "change-me" .env
```

Expected: `0` (no quedan valores de ejemplo). No imprimir los secretos en el chat.

- [ ] **Step 5: Verificar que las imágenes de Supabase existen**

```bash
docker pull supabase/postgres:15.8.1.060
docker pull supabase/postgres-meta:v0.91.0
docker pull supabase/studio:2025.06.30-sha-6f5982d
```

Expected: las tres descargan. Si alguna falla con `manifest unknown`, tomar el tag vigente del `docker-compose.yml` oficial (`https://github.com/supabase/supabase/blob/master/docker/docker-compose.yml`), actualizarlo en `.env` y `.env.example`, y repetir el pull.

- [ ] **Step 6: Actualizar la tabla de variables en `CLAUDE.md` (sección 6)**

Reemplazar la fila:

```markdown
| `SUPABASE_DB_DATA_PATH` | Ruta del volumen en el host |
```

por:

```markdown
| `SUPABASE_DB_DATA_PATH` | Ruta de montaje del volumen dentro del contenedor de Postgres (el volumen es nombrado: en Windows un bind mount rompe los permisos de Postgres) |
| `SUPABASE_POSTGRES_IMAGE`, `SUPABASE_PG_META_IMAGE`, `SUPABASE_STUDIO_IMAGE` | Imágenes (con tag fijo) del stack de Supabase |
| `POSTGRES_HOST_PORT` | Puerto de Postgres expuesto en el host |
| `APP_DB_SCHEMA` | Esquema de la aplicación (lo crea Flyway) |
| `STUDIO_CONTAINER_PORT`, `STUDIO_ORGANIZATION_NAME`, `STUDIO_PROJECT_NAME`, `STUDIO_SUPABASE_URL`, `STUDIO_ANON_KEY`, `STUDIO_SERVICE_KEY` | Configuración de Supabase Studio (solo administración) |
```

- [ ] **Step 7: Commit**

```bash
git add .gitignore .env.example CLAUDE.md docs/
git status --short   # confirmar que .env NO aparece
git commit -m "chore: init repo with env configuration and project docs" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Esqueleto del backend (Spring Boot + Flyway + health)

**Files:**
- Create: `backend/pom.xml`, Maven wrapper, `backend/src/main/java/com/tomanagement/ToManagementApplication.java`, `backend/src/main/java/com/tomanagement/common/config/ClockConfig.java`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/db/migration/V1__init_app_schema.sql`
- Test: `backend/src/test/java/com/tomanagement/ApplicationStartupTest.java`, `TestcontainersConfiguration.java`, `TestContainerImage.java`, `backend/src/test/resources/application-test.yml`

**Interfaces:**
- Consumes: variables `BACKEND_PORT`, `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_DB_SCHEMA` (Task 1).
- Produces:
  - Bean `java.time.Clock` (UTC).
  - `TestcontainersConfiguration` (`@TestConfiguration`, se importa con `@Import`) para tests de integración.
  - Perfil de test `test`.
  - `GET /actuator/health` → `{"status":"UP"}`.
  - Esquema `${APP_DB_SCHEMA}` creado por Flyway.

- [ ] **Step 1: Crear `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.tomanagement</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>to-management-backend</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>backend</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Generar el Maven wrapper**

```bash
cd backend
mvn -N wrapper:wrapper -Dmaven=3.9.9
java -version   # debe ser 21
```

Expected: se crean `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`; `java -version` muestra 21.

- [ ] **Step 3: Escribir el test de arranque (falla)**

`backend/src/test/java/com/tomanagement/TestContainerImage.java`:

```java
package com.tomanagement;

/**
 * Docker images used by Testcontainers in integration tests.
 */
public enum TestContainerImage {

    POSTGRES("postgres:15-alpine");

    private final String imageName;

    TestContainerImage(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
```

`backend/src/test/java/com/tomanagement/TestcontainersConfiguration.java`:

```java
package com.tomanagement;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(TestContainerImage.POSTGRES.getImageName()));
    }
}
```

`backend/src/test/resources/application-test.yml` (valores solo para tests; la conexión real la reemplaza `@ServiceConnection`):

```yaml
BACKEND_PORT: 0
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: test
POSTGRES_USER: test
POSTGRES_PASSWORD: test
APP_DB_SCHEMA: app
```

`backend/src/test/java/com/tomanagement/ApplicationStartupTest.java`:

```java
package com.tomanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApplicationStartupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${APP_DB_SCHEMA}")
    private String appSchema;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void flywayCreatesApplicationSchemaAndAppliesFirstMigration() {
        Integer schemaCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = ?",
                Integer.class,
                appSchema);
        assertThat(schemaCount).isEqualTo(1);

        Boolean firstMigrationApplied = jdbcTemplate.queryForObject(
                "select success from " + appSchema + ".flyway_schema_history where version = '1'",
                Boolean.class);
        assertThat(firstMigrationApplied).isTrue();
    }
}
```

- [ ] **Step 4: Correr el test y verificar que falla**

```bash
cd backend && ./mvnw -q test -Dtest=ApplicationStartupTest
```

Expected: FAIL. No compila porque falta la clase `@SpringBootApplication` (`Unable to find a @SpringBootConfiguration`).

- [ ] **Step 5: Implementación mínima**

`backend/src/main/java/com/tomanagement/ToManagementApplication.java`:

```java
package com.tomanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ToManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToManagementApplication.class, args);
    }
}
```

`backend/src/main/java/com/tomanagement/common/config/ClockConfig.java`:

```java
package com.tomanagement.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a UTC clock so time-dependent code can be tested with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

`backend/src/main/resources/application.yml`:

```yaml
server:
  port: ${BACKEND_PORT}

spring:
  application:
    name: to-management-backend
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: ${APP_DB_SCHEMA}
  flyway:
    schemas: ${APP_DB_SCHEMA}
    default-schema: ${APP_DB_SCHEMA}
    create-schemas: true

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

`backend/src/main/resources/db/migration/V1__init_app_schema.sql`:

```sql
COMMENT ON SCHEMA ${flyway:defaultSchema} IS 'TO Management application schema';
```

- [ ] **Step 6: Correr el test y verificar que pasa**

```bash
cd backend && ./mvnw -q test -Dtest=ApplicationStartupTest
```

Expected: PASS (2 tests). Requiere Docker Desktop corriendo.

- [ ] **Step 7: Commit**

```bash
git add backend
git commit -m "feat(backend): add Spring Boot skeleton with Flyway schema and health endpoint" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Infraestructura de errores (excepciones custom + handler global)

**Files:**
- Create: `backend/src/main/java/com/tomanagement/common/exception/ErrorCode.java`, `ApplicationException.java`, `ResourceNotFoundException.java`, `DuplicateResourceException.java`, `BusinessRuleException.java`, `AuthenticationFailedException.java`, `AccessDeniedApplicationException.java`, `InternalSystemException.java`, `dto/ErrorResponse.java`, `dto/FieldErrorResponse.java`, `handler/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/tomanagement/common/exception/handler/GlobalExceptionHandlerTest.java`, `ExceptionProbeController.java`

**Interfaces:**
- Consumes: bean `java.time.Clock` (Task 2).
- Produces (las épicas siguientes dependen de esto):
  - `enum ErrorCode` con `HttpStatus getHttpStatus()` y `String getMessage()`. Las épicas siguientes agregan códigos nuevos (ej. `PATIENT_DNI_DUPLICATED`) como constantes nuevas del enum.
  - `abstract class ApplicationException extends RuntimeException` con `ErrorCode getErrorCode()`.
  - `ResourceNotFoundException()`, `ResourceNotFoundException(ErrorCode)`, y lo mismo para `DuplicateResourceException`, `BusinessRuleException`, `AuthenticationFailedException` y `AccessDeniedApplicationException`. `InternalSystemException(Throwable cause)`.
  - `ErrorResponse` (JSON `{code, message, timestamp, fieldErrors?}`) con `ErrorResponse.builder()`.
  - `FieldErrorResponse(String field, String message)`.
- Nota para É1: Spring Security lanza `AuthenticationException` y `AccessDeniedException` fuera del dispatcher. É1 tiene que mapearlas a `AUTHENTICATION_FAILED` y `ACCESS_DENIED` (entry point y access-denied handler), o terminan en 500.

- [ ] **Step 1: Escribir el controller de prueba y el test (falla)**

`backend/src/test/java/com/tomanagement/common/exception/handler/ExceptionProbeController.java`:

```java
package com.tomanagement.common.exception.handler;

import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller that triggers each error path handled by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/test/errors")
class ExceptionProbeController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException();
    }

    @GetMapping("/duplicate")
    void duplicate() {
        throw new DuplicateResourceException();
    }

    @GetMapping("/business")
    void business() {
        throw new BusinessRuleException();
    }

    @GetMapping("/authentication")
    void authentication() {
        throw new AuthenticationFailedException();
    }

    @GetMapping("/access-denied")
    void accessDenied() {
        throw new AccessDeniedApplicationException();
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("db password=secret");
    }

    @PostMapping("/validated")
    void validated(@Valid @RequestBody ProbeRequest request) {
        // Body validation is the behavior under test.
    }

    @GetMapping("/typed/{id}")
    void typed(@PathVariable Long id) {
        // Path variable conversion is the behavior under test.
    }

    @GetMapping("/param")
    void param(@RequestParam String required) {
        // Required parameter presence is the behavior under test.
    }

    record ProbeRequest(@NotBlank String name) {
    }
}
```

`backend/src/test/java/com/tomanagement/common/exception/handler/GlobalExceptionHandlerTest.java`:

```java
package com.tomanagement.common.exception.handler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.tomanagement.common.exception.ErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExceptionProbeController.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    private static final String FIXED_INSTANT = "2026-11-04T12:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse(FIXED_INSTANT), ZoneOffset.UTC);
        }
    }

    @Test
    void resourceNotFoundMapsTo404WithUniformBody() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void duplicateResourceMapsTo409() throws Exception {
        mockMvc.perform(get("/test/errors/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_RESOURCE.name()));
    }

    @Test
    void businessRuleMapsTo422() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()));
    }

    @Test
    void authenticationFailureMapsTo401WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/errors/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.name()))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos."));
    }

    @Test
    void accessDeniedMapsTo403() throws Exception {
        mockMvc.perform(get("/test/errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void unexpectedExceptionIsTranslatedToGenericInternalError() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void wrongPathVariableTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @Test
    void unknownEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/no/such/path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ENDPOINT_NOT_FOUND.name()));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(delete("/test/errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));
    }
}
```

`FIXED_INSTANT` es un valor de fixture de test, no una constante de dominio. Se acepta como el resto de los literales de test.

- [ ] **Step 2: Correr el test y verificar que falla**

```bash
cd backend && ./mvnw -q test -Dtest=GlobalExceptionHandlerTest
```

Expected: FAIL de compilación. Faltan `ErrorCode`, `ResourceNotFoundException` y las demás clases.

- [ ] **Step 3: Implementar `ErrorCode`**

`backend/src/main/java/com/tomanagement/common/exception/ErrorCode.java`:

```java
package com.tomanagement.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalog of every error the API can return: its HTTP status and the user-facing message (Spanish UI).
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso solicitado no existe."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "El recurso ya existe."),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "La operación no cumple las reglas del negocio."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "La solicitud está mal formada."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El formato de la solicitud no está soportado."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "La ruta solicitada no existe."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá nuevamente.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 4: Implementar la jerarquía de excepciones (una clase por archivo)**

`ApplicationException.java`:

```java
package com.tomanagement.common.exception;

/**
 * Base of every custom exception. The ErrorCode decides the HTTP status and the message sent to the client.
 */
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

`ResourceNotFoundException.java`:

```java
package com.tomanagement.common.exception;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

`DuplicateResourceException.java`:

```java
package com.tomanagement.common.exception;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException() {
        super(ErrorCode.DUPLICATE_RESOURCE);
    }

    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

`BusinessRuleException.java`:

```java
package com.tomanagement.common.exception;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException() {
        super(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    public BusinessRuleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

`AuthenticationFailedException.java`:

```java
package com.tomanagement.common.exception;

public class AuthenticationFailedException extends ApplicationException {

    public AuthenticationFailedException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }

    public AuthenticationFailedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

`AccessDeniedApplicationException.java`:

```java
package com.tomanagement.common.exception;

public class AccessDeniedApplicationException extends ApplicationException {

    public AccessDeniedApplicationException() {
        super(ErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedApplicationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

`InternalSystemException.java`:

```java
package com.tomanagement.common.exception;

/**
 * Wraps any unexpected or third-party failure so the client only ever sees a custom, generic error.
 */
public class InternalSystemException extends ApplicationException {

    public InternalSystemException(Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, cause);
    }
}
```

- [ ] **Step 5: Implementar los DTOs de error (Builder)**

`dto/FieldErrorResponse.java`:

```java
package com.tomanagement.common.exception.dto;

public record FieldErrorResponse(String field, String message) {
}
```

`dto/ErrorResponse.java`:

```java
package com.tomanagement.common.exception.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tomanagement.common.exception.ErrorCode;

/**
 * Uniform error body returned by the API. Built with the Builder pattern.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class ErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final List<FieldErrorResponse> fieldErrors;

    private ErrorResponse(Builder builder) {
        this.code = builder.errorCode.name();
        this.message = builder.errorCode.getMessage();
        this.timestamp = builder.timestamp;
        this.fieldErrors = List.copyOf(builder.fieldErrors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }

    public static final class Builder {

        private ErrorCode errorCode;
        private Instant timestamp;
        private final List<FieldErrorResponse> fieldErrors = new ArrayList<>();

        private Builder() {
        }

        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder fieldErrors(List<FieldErrorResponse> fieldErrors) {
            this.fieldErrors.addAll(fieldErrors);
            return this;
        }

        public ErrorResponse build() {
            Objects.requireNonNull(errorCode, "errorCode is required");
            Objects.requireNonNull(timestamp, "timestamp is required");
            return new ErrorResponse(this);
        }
    }
}
```

- [ ] **Step 6: Implementar `GlobalExceptionHandler`**

`handler/GlobalExceptionHandler.java`:

```java
package com.tomanagement.common.exception.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.InternalSystemException;
import com.tomanagement.common.exception.dto.ErrorResponse;
import com.tomanagement.common.exception.dto.FieldErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single translation point from any exception to the uniform ErrorResponse.
 * Unexpected exceptions are wrapped in InternalSystemException so internal details never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException exception) {
        LOGGER.warn("Application error: {}", exception.getErrorCode());
        return respond(exception.getErrorCode(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return respond(ErrorCode.MALFORMED_REQUEST, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownEndpoint(NoResourceFoundException exception) {
        return respond(ErrorCode.ENDPOINT_NOT_FOUND, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        InternalSystemException wrapped = new InternalSystemException(exception);
        LOGGER.error("Unexpected error", wrapped);
        return respond(wrapped.getErrorCode(), List.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .timestamp(Instant.now(clock))
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }
}
```

- [ ] **Step 7: Correr todos los tests del backend**

```bash
cd backend && ./mvnw -q test
```

Expected: PASS. Son 13 tests en `GlobalExceptionHandlerTest` y 2 en `ApplicationStartupTest`.

- [ ] **Step 8: Verificar que no hay imports con comodín**

```bash
grep -rn "^import .*\*;" backend/src || echo "OK: no wildcard imports"
```

Expected: `OK: no wildcard imports`.

- [ ] **Step 9: Commit**

```bash
git add backend
git commit -m "feat(backend): add custom exception hierarchy and global exception handler" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Imagen Docker del backend

**Files:**
- Create: `backend/Dockerfile`, `backend/.dockerignore`

**Interfaces:**
- Consumes: `backend/pom.xml` con `finalName` `backend` (Task 2).
- Produces: imagen que arranca con `java -jar /app/app.jar` y lee toda su configuración de variables de entorno.

- [ ] **Step 1: Crear `backend/.dockerignore`**

```
target/
.idea/
*.iml
```

- [ ] **Step 2: Crear `backend/Dockerfile`**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Los tests corren con `./mvnw test` (Tasks 2 y 3) y no dentro del build de la imagen: necesitan Docker para Testcontainers.

- [ ] **Step 3: Construir la imagen**

```bash
docker build -t to-management-backend:local backend
```

Expected: `naming to docker.io/library/to-management-backend:local` sin errores.

- [ ] **Step 4: Commit**

```bash
git add backend/Dockerfile backend/.dockerignore
git commit -m "build(backend): add multi-stage Dockerfile" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Esqueleto del frontend (Angular + nginx)

**Files:**
- Create: `frontend/` (Angular CLI), `frontend/nginx/default.conf.template`, `frontend/Dockerfile`, `frontend/.dockerignore`
- Modify (reemplazo completo): `frontend/src/app/app.ts`, `frontend/src/app/app.html`, `frontend/src/app/app.scss`, `frontend/src/app/app.spec.ts`, `frontend/src/styles.scss`

**Interfaces:**
- Consumes: `FRONTEND_PORT` y `API_BASE_URL` como variables de entorno del contenedor nginx (Task 6).
- Produces:
  - Shell de Angular con `<router-outlet />` y rutas vacías, donde las épicas siguientes agregan las suyas en `src/app/app.routes.ts`.
  - Proxy `/api/` → `API_BASE_URL`, así que el código Angular usa rutas relativas `/api/...` y nunca una URL fija.

- [ ] **Step 1: Generar el proyecto**

```bash
cd "/c/Users/Maxi/Desktop/TO Managment"
npx -y @angular/cli@latest new frontend --routing --style=scss --ssr=false --skip-git --interactive=false
```

Expected: se crea `frontend/` con `src/app/app.ts`, `app.html` y `app.routes.ts`. Si la versión de CLI genera `app.component.ts` en lugar de `app.ts`, usar esos nombres de archivo (y `AppComponent` como clase) en los pasos siguientes.

- [ ] **Step 2: Escribir el test del shell (falla)**

Reemplazar `frontend/src/app/app.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('creates the shell', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders only the router outlet, without placeholder content', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('router-outlet')).toBeTruthy();
    expect(element.querySelector('h1')).toBeNull();
  });
});
```

- [ ] **Step 3: Correr el test y verificar que falla**

```bash
cd frontend && npx ng test --watch=false
```

Expected: FAIL en `renders only the router outlet...`, porque la plantilla por defecto tiene un `<h1>`. Si el runner es Karma y pide navegador, agregar `--browsers=ChromeHeadless`.

- [ ] **Step 4: Reemplazar el shell**

`frontend/src/app/app.ts`:

```ts
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
```

`frontend/src/app/app.html`:

```html
<router-outlet />
```

`frontend/src/app/app.scss`:

```scss
:host {
  display: block;
  min-height: 100dvh;
}
```

`frontend/src/styles.scss`:

```scss
/* Mobile-first base: styles target phones (>= 360px); larger screens add media queries upward. */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  padding: 0;
}

body {
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  line-height: 1.5;
  -webkit-text-size-adjust: 100%;
}

img {
  max-width: 100%;
  display: block;
}
```

Verificar que `frontend/src/index.html` tiene `<meta name="viewport" content="width=device-width, initial-scale=1">` (el CLI lo genera) y que el `<html>` tiene `lang="es"`. Si dice `lang="en"`, cambiarlo a `lang="es"`.

- [ ] **Step 5: Correr el test y verificar que pasa**

```bash
cd frontend && npx ng test --watch=false
```

Expected: PASS (2 tests).

- [ ] **Step 6: Crear la configuración de nginx**

`frontend/nginx/default.conf.template` (la imagen oficial de nginx reemplaza las variables `${...}` definidas en el entorno; `$uri` queda intacto):

```nginx
server {
    listen ${FRONTEND_PORT};
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass ${API_BASE_URL};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- [ ] **Step 7: Crear `frontend/.dockerignore` y `frontend/Dockerfile`**

`frontend/.dockerignore`:

```
node_modules/
dist/
.angular/
```

`frontend/Dockerfile`:

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
```

- [ ] **Step 8: Construir la imagen**

```bash
docker build -t to-management-frontend:local frontend
```

Expected: build OK. Si falla en el `COPY --from=build`, revisar la carpeta de salida con `ls frontend/dist/frontend`. En las versiones recientes es `browser/`.

- [ ] **Step 9: Commit**

```bash
git add frontend
git commit -m "feat(frontend): add Angular shell served by nginx with /api proxy" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: `docker-compose.yml` y verificación end-to-end

**Files:**
- Create: `docker-compose.yml`

**Interfaces:**
- Consumes: todas las variables de la Task 1, la imagen del backend (Task 4) y la del frontend (Task 5).
- Produces: el stack completo levantado con `docker compose up -d --build`, y el volumen `${SUPABASE_DB_VOLUME_NAME}` con los datos de Postgres.

- [ ] **Step 1: Crear `docker-compose.yml`**

```yaml
name: ${COMPOSE_PROJECT_NAME:?COMPOSE_PROJECT_NAME is required}

services:
  db:
    image: ${SUPABASE_POSTGRES_IMAGE:?SUPABASE_POSTGRES_IMAGE is required}
    restart: unless-stopped
    command: ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
    environment:
      PGPORT: ${POSTGRES_PORT:?POSTGRES_PORT is required}
      POSTGRES_PORT: ${POSTGRES_PORT}
      POSTGRES_DB: ${POSTGRES_DB:?POSTGRES_DB is required}
      PGDATABASE: ${POSTGRES_DB}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}
      PGPASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_HOST_PORT:?POSTGRES_HOST_PORT is required}:${POSTGRES_PORT}"
    volumes:
      - db-data:${SUPABASE_DB_DATA_PATH:?SUPABASE_DB_DATA_PATH is required}
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "${POSTGRES_USER:?POSTGRES_USER is required}", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 20

  meta:
    image: ${SUPABASE_PG_META_IMAGE:?SUPABASE_PG_META_IMAGE is required}
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      PG_META_PORT: ${PG_META_PORT:?PG_META_PORT is required}
      PG_META_DB_HOST: ${POSTGRES_HOST:?POSTGRES_HOST is required}
      PG_META_DB_PORT: ${POSTGRES_PORT}
      PG_META_DB_NAME: ${POSTGRES_DB}
      PG_META_DB_USER: ${POSTGRES_USER}
      PG_META_DB_PASSWORD: ${POSTGRES_PASSWORD}

  studio:
    image: ${SUPABASE_STUDIO_IMAGE:?SUPABASE_STUDIO_IMAGE is required}
    restart: unless-stopped
    depends_on:
      - meta
    environment:
      STUDIO_PG_META_URL: http://meta:${PG_META_PORT}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      DEFAULT_ORGANIZATION_NAME: ${STUDIO_ORGANIZATION_NAME:?STUDIO_ORGANIZATION_NAME is required}
      DEFAULT_PROJECT_NAME: ${STUDIO_PROJECT_NAME:?STUDIO_PROJECT_NAME is required}
      SUPABASE_URL: ${STUDIO_SUPABASE_URL:?STUDIO_SUPABASE_URL is required}
      SUPABASE_PUBLIC_URL: ${STUDIO_SUPABASE_URL}
      SUPABASE_ANON_KEY: ${STUDIO_ANON_KEY:?STUDIO_ANON_KEY is required}
      SUPABASE_SERVICE_KEY: ${STUDIO_SERVICE_KEY:?STUDIO_SERVICE_KEY is required}
      AUTH_JWT_SECRET: ${JWT_SECRET:?JWT_SECRET is required}
    ports:
      - "${STUDIO_PORT:?STUDIO_PORT is required}:${STUDIO_CONTAINER_PORT:?STUDIO_CONTAINER_PORT is required}"

  backend:
    build: ./backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      BACKEND_PORT: ${BACKEND_PORT:?BACKEND_PORT is required}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:?SPRING_PROFILES_ACTIVE is required}
      POSTGRES_HOST: ${POSTGRES_HOST}
      POSTGRES_PORT: ${POSTGRES_PORT}
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      APP_DB_SCHEMA: ${APP_DB_SCHEMA:?APP_DB_SCHEMA is required}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS:?JWT_EXPIRATION_MS is required}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS is required}
      ADMIN_EMAIL: ${ADMIN_EMAIL:?ADMIN_EMAIL is required}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:?ADMIN_PASSWORD is required}
      ADMIN_NAME: ${ADMIN_NAME:?ADMIN_NAME is required}
    ports:
      - "${BACKEND_PORT}:${BACKEND_PORT}"

  frontend:
    build: ./frontend
    restart: unless-stopped
    depends_on:
      - backend
    environment:
      FRONTEND_PORT: ${FRONTEND_PORT:?FRONTEND_PORT is required}
      API_BASE_URL: ${API_BASE_URL:?API_BASE_URL is required}
    ports:
      - "${FRONTEND_PORT}:${FRONTEND_PORT}"

volumes:
  db-data:
    name: ${SUPABASE_DB_VOLUME_NAME:?SUPABASE_DB_VOLUME_NAME is required}
```

- [ ] **Step 2: Validar la configuración**

```bash
docker compose config --quiet && echo "compose OK"
```

Expected: `compose OK`.

- [ ] **Step 3: Review Focus 5 — una variable obligatoria vacía hace fallar a compose**

```bash
POSTGRES_PASSWORD= docker compose config --quiet; echo "exit=$?"
```

Expected: mensaje `POSTGRES_PASSWORD is required` y `exit=1`.

- [ ] **Step 4: Levantar el stack**

```bash
docker compose up -d --build
docker compose ps
```

Expected: `db` en estado `healthy`; `meta`, `studio`, `backend` y `frontend` en `running`. Si alguno se reinicia en bucle, revisarlo con `docker compose logs <servicio>`.

- [ ] **Step 5: Verificar backend, proxy y SPA (Review Focus 1 y 4)**

```bash
set -a; source .env; set +a
curl -s "http://localhost:${BACKEND_PORT}/actuator/health"
curl -s -w "\nHTTP %{http_code}\n" "http://localhost:${FRONTEND_PORT}/api/no-existe"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "http://localhost:${FRONTEND_PORT}/pacientes/123"
```

Expected:
1. `{"status":"UP"}`
2. JSON con `"code":"ENDPOINT_NOT_FOUND"` y `HTTP 404`. Eso prueba el proxy de nginx al backend y el handler.
3. `HTTP 200`: la ruta profunda de la SPA devuelve `index.html`.

- [ ] **Step 6: Verificar Studio**

Abrir `http://localhost:${STUDIO_PORT}` en el navegador → Table Editor → esquema `app`. Expected: aparece la tabla `flyway_schema_history`. Si Studio carga pero el Table Editor da error, revisar `docker compose logs studio meta` y confirmar que `STUDIO_PG_META_URL` apunta a `http://meta:${PG_META_PORT}`.

- [ ] **Step 7: Verificar la persistencia del volumen**

```bash
docker compose exec db psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "create table app.persistence_check(id int); insert into app.persistence_check values (1);"
docker compose down
docker volume ls --filter "name=${SUPABASE_DB_VOLUME_NAME}"
docker compose up -d
docker compose exec db psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "select id from app.persistence_check;"
docker compose exec db psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "drop table app.persistence_check;"
```

Expected:
- `docker volume ls` muestra `${SUPABASE_DB_VOLUME_NAME}`.
- Después del `down`/`up`, el `select` devuelve la fila `1`.
- Al final la tabla de prueba se elimina.

No usar `docker compose down -v`, porque borra el volumen.

- [ ] **Step 8: Commit**

```bash
git add docker-compose.yml
git commit -m "build: add docker-compose stack with Supabase, backend and frontend" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
