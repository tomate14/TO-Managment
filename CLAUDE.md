# CLAUDE.md — TO Managment

## 1. Descripción del proyecto

Aplicación de gestión para **terapia ocupacional**, pensada para una terapeuta profesional.
Es una app **web mobile-first y responsive**: tiene que funcionar bien en celular (desde 360px de ancho), tablet y escritorio.

Las funcionalidades concretas (pacientes, sesiones, evaluaciones, etc.) **no están definidas acá**. Se diseñan con el flujo de superpowers (ver sección 2). No inventes funcionalidades que no tengan un spec aprobado.

## 2. Uso OBLIGATORIO de superpowers (skills y agentes)

Todo el desarrollo de este proyecto se hace con el plugin **superpowers**. No es opcional.

### Reglas generales
- **Antes de cualquier tarea** (incluso antes de preguntar algo o explorar archivos), revisá qué skill aplica e invocala con la herramienta `Skill`. Si hay aunque sea una mínima chance de que una skill aplique, usala (`superpowers:using-superpowers`).
- Anunciá qué skill estás usando y para qué ("Usando superpowers:X para Y").
- Si una skill tiene checklist, cada ítem va a la lista de tareas.

### Flujo por tipo de tarea

| Situación | Skill(s) a usar |
|---|---|
| Nueva funcionalidad, componente o cambio de comportamiento | `superpowers:brainstorming` → spec en `docs/superpowers/specs/YYYY-MM-DD-<tema>-design.md` |
| Spec aprobado → plan de implementación | `superpowers:writing-plans` → plan en `docs/superpowers/plans/` |
| Ejecutar un plan | `superpowers:subagent-driven-development` (preferido) o `superpowers:executing-plans` |
| Escribir cualquier código (feature o fix) | `superpowers:test-driven-development` (test primero, siempre) |
| 2+ tareas independientes | `superpowers:dispatching-parallel-agents` (agentes en paralelo) |
| Bug, test fallando o comportamiento inesperado | `superpowers:systematic-debugging` (antes de proponer un fix) |
| Antes de decir "terminado", "funciona" o "pasa" | `superpowers:verification-before-completion` (evidencia antes que afirmaciones) |
| Al terminar una tarea o feature importante | `superpowers:requesting-code-review` |
| Al recibir feedback de revisión | `superpowers:receiving-code-review` |
| Empezar trabajo aislado en una rama | `superpowers:using-git-worktrees` |
| Implementación completa y tests en verde | `superpowers:finishing-a-development-branch` |

### Mockups obligatorios antes de codificar
- **Ningún desarrollo de código arranca sin el mockup/diseño de las pantallas involucradas.** Antes de implementar una historia, pedile al usuario los mockups.
- Si no hay mockups, se puede seguir con brainstorming, specs, historias y planes, pero **no** se escribe código de producto.
- Los mockups recibidos se guardan en `docs/mockups/<historia>/` y se referencian desde el spec o el plan correspondiente.

### Agentes
- Usá **subagentes** para implementar las tareas de los planes (`subagent-driven-development`) y para trabajo independiente en paralelo (`dispatching-parallel-agents`).
- Cada subagente recibe un brief completo: objetivo, archivos involucrados, criterio de terminado y cómo verificarlo.
- El resultado de un subagente se verifica (tests, diff) antes de darlo por bueno.

## 3. Stack técnico

| Capa | Tecnología |
|---|---|
| Frontend | **Angular** (última versión estable), standalone components, TypeScript, SCSS |
| Backend | **Java 21** + **Spring Boot 3.x**, Maven (`mvnw`) |
| Seguridad | **Spring Security + JWT** (contraseñas con BCrypt) |
| Base de datos | **Supabase self-hosted** (Postgres de Supabase: imagen `supabase/postgres`) |
| Administración de BD | Supabase **Studio** + **postgres-meta** (solo uso administrativo) |
| Migraciones | **Flyway** (desde el backend) |
| Contenedores | **Docker** + **docker-compose** |
| Tests backend | JUnit 5, Spring Boot Test, Testcontainers (Postgres) |
| Tests frontend | Runner de tests por defecto de Angular |

## 4. Arquitectura

```
[Angular (nginx)] --HTTP /api/** + JWT--> [Spring Boot] --JDBC/JPA--> [Postgres de Supabase]
                                                                            ^
                                                  [Supabase Studio + postgres-meta] (solo admin)
```

- Angular habla **únicamente** con el backend Spring Boot (REST bajo `/api/**`).
- La autenticación y autorización las resuelve **Spring Security con JWT**. **No** se usan Supabase Auth ni PostgREST.
- Spring Boot accede a Postgres por JDBC/JPA. El esquema se versiona con migraciones Flyway en `backend/src/main/resources/db/migration`.
- Studio no es parte de la app: es solo para inspeccionar y administrar la base.

## 5. Estructura de carpetas

```
/frontend                 Angular (Dockerfile multi-stage: build con Node → servir con nginx)
/backend                  Spring Boot (Dockerfile multi-stage: build con Maven → JRE 21)
/supabase                 SQL de inicialización y configuración del stack de Supabase
/supabase/init            01_schema.sql (esquema completo) y 02_seed.sql (datos iniciales), siempre al día con las migraciones
/docs/superpowers/specs   Specs de diseño (salida de brainstorming)
/docs/superpowers/plans   Planes de implementación (salida de writing-plans)
/docs/mockups             Mockups de pantallas, por historia (requisito para codificar)
docker-compose.yml
.env                      Variables reales (NO se commitea)
.env.example              Plantilla sin secretos (SÍ se commitea)
CLAUDE.md
```

## 6. Docker y variables de entorno (reglas estrictas)

- **Una sola fuente de verdad: el archivo `.env` en la raíz del proyecto.** Todas las variables de entorno del proyecto (base de datos, backend, frontend, puertos, secretos, volúmenes) se definen ahí.
- **Prohibido hardcodear** credenciales, puertos, hosts, URLs o nombres de volúmenes en código, `application.yml`, Dockerfiles o `docker-compose.yml`. Siempre se leen de variables.
- `docker-compose.yml` consume el `.env`: interpolación `${VAR}` y `env_file: .env` en los servicios que lo necesiten.
- Toda variable nueva se agrega **en el mismo cambio** a `.env` (con su valor) y a `.env.example` (con un valor de ejemplo, sin secretos reales).
- `.env` va en `.gitignore`; `.env.example` se commitea.
- Spring Boot lee la configuración con placeholders (`${POSTGRES_HOST}`, etc.) en `application.yml`.
- Angular recibe `API_BASE_URL` en tiempo de build o de arranque del contenedor (nunca escrita a mano en el código).

### Volumen de la base de datos
- Los datos de Postgres persisten en un **volumen Docker definido en el `.env`** (`SUPABASE_DB_VOLUME_NAME` y `SUPABASE_DB_DATA_PATH`).
- Se monta en `/var/lib/postgresql/data` del servicio de base de datos y es un volumen compartido, declarado a nivel raíz del compose, para los servicios del stack que lo necesiten.
- `docker compose down` **no** debe borrar datos. Nunca uses `docker compose down -v` sin pedir confirmación.

### Variables iniciales (mínimo)

| Variable | Uso |
|---|---|
| `COMPOSE_PROJECT_NAME` | Nombre del proyecto en Docker |
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Conexión a Postgres |
| `SUPABASE_DB_VOLUME_NAME` | Nombre del volumen de datos |
| `SUPABASE_DB_DATA_PATH` | Ruta de montaje del volumen dentro del contenedor de Postgres (el volumen es nombrado: en Windows un bind mount rompe los permisos de Postgres) |
| `SUPABASE_POSTGRES_IMAGE`, `SUPABASE_PG_META_IMAGE`, `SUPABASE_STUDIO_IMAGE` | Imágenes (con tag fijo) del stack de Supabase |
| `POSTGRES_HOST_PORT` | Puerto de Postgres expuesto en el host |
| `APP_DB_SCHEMA` | Esquema de la aplicación (lo crea Flyway) |
| `STUDIO_CONTAINER_PORT`, `STUDIO_ORGANIZATION_NAME`, `STUDIO_PROJECT_NAME`, `STUDIO_SUPABASE_URL`, `STUDIO_ANON_KEY`, `STUDIO_SERVICE_KEY` | Configuración de Supabase Studio (solo administración) |
| `STUDIO_PORT`, `PG_META_PORT` | Puertos de Supabase Studio y postgres-meta |
| `STUDIO_JWT_SECRET` | Secreto JWT propio de Studio (aleatorio, distinto de `JWT_SECRET`; nunca reutilizar el de la app) |
| `ADMIN_BIND_ADDRESS` | Interfaz del host donde se publican Studio, Postgres y backend (`127.0.0.1`: solo local) |
| `BACKEND_PORT`, `SPRING_PROFILES_ACTIVE` | Backend |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | Firma y expiración de los JWT |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | Usuario admin inicial (se crea al primer arranque) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para el frontend |
| `FRONTEND_PORT`, `API_BASE_URL` | Frontend |

### Seguridad del stack
- Studio **no tiene autenticación**: su puerto se publica solo en `ADMIN_BIND_ADDRESS` (loopback) y nunca debe exponerse. Studio nunca recibe `JWT_SECRET` (usa `STUDIO_JWT_SECRET`).
- `POSTGRES_USER` debe seguir siendo `postgres` (los roles de la imagen de Supabase son fijos).
- Cambiar `POSTGRES_PASSWORD` después de inicializado el volumen **no** rota la contraseña de la base: hay que cambiarla dentro de Postgres.

## 7. Convenciones

### Generales
- **UI en español**; código, identificadores, commits y nombres de archivos en **inglés**.
- **TDD siempre** (ver sección 2): primero el test que falla, después el código.
- Archivos chicos y con una sola responsabilidad.
- Los datos de pacientes son sensibles: **nunca** se loguean datos personales ni clínicos, y ningún secreto va al repo.

### Frontend (Angular)
- Standalone components, rutas con lazy loading.
- Mobile-first: estilos base para celular y media queries hacia arriba (breakpoints desde 360px).
- Llamadas HTTP en servicios; un interceptor agrega el JWT y maneja los 401.
- Formularios reactivos con validación.

### Backend (Spring Boot)
- Paquetes por feature (`<feature>/controller`, `service`, `repository`, `dto`, `entity`).
- DTOs en la API (no exponer entidades JPA), validados con `jakarta.validation`.
- Cambios de esquema **solo** con migraciones Flyway nuevas (nunca editar una ya aplicada).
- **Scripts de inicialización consolidados:** además de la migración, toda tabla nueva o modificada se refleja **en el mismo cambio** en `supabase/init/01_schema.sql` (estado final del esquema completo, idempotente) y todo dato inicializador (catálogos, valores por defecto) en `supabase/init/02_seed.sql`. Con esos dos scripts se tiene que poder crear la base desde cero cuando el proyecto esté terminado. Se verifican corriéndolos sobre un Postgres vacío.
- Tests de integración con Testcontainers contra Postgres real.

### Diseño orientado a objetos del backend (reglas obligatorias)
Actuá como un buen diseñador de objetos: aplicá buenas prácticas de Java (SOLID, inmutabilidad cuando se pueda, composición antes que herencia, nombres claros).

- **Prohibido `import *`.** Cada import es explícito, clase por clase (también los `import static`).
- **Patrones de diseño**: usá patrones **creacionales** (p. ej. Builder, Factory Method, Abstract Factory), **estructurales** (p. ej. Adapter, Decorator, Facade, Composite) y **de comportamiento** (p. ej. Strategy, Template Method, Observer, Chain of Responsibility, State), **siempre que resuelvan un problema real**. No fuerces un patrón donde no aporta: si lo usás, el nombre de las clases o un comentario breve tiene que dejar claro cuál es y por qué.
- **Abstracciones**: programá contra interfaces y clases abstractas, no contra implementaciones concretas, para que el código sea fácil de mantener y extender. Las dependencias se inyectan por constructor.
- **Servicios = interfaz + implementación.** Todo servicio tiene una interfaz (`PatientService`) y su implementación (`PatientServiceImpl`). Controllers y otros servicios dependen **solo** de la interfaz.
- **Sin constantes sueltas en el código.** Valores fijos del dominio (estados, tipos, roles, códigos de error, mensajes, etc.) se modelan como **`enum`**. No hay `static final` mágicos ni literales repetidos. La configuración de entorno (puertos, credenciales, URLs) sigue viniendo del `.env` (sección 6), no de enums.
- **Excepciones custom y handler global**:
  - Hay una jerarquía de excepciones propia con una base abstracta (p. ej. `ApplicationException extends RuntimeException`) y subclases específicas, **cada una en su propia clase** (p. ej. `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedException`, `InternalSystemException`).
  - Cada excepción lleva un código de un `enum` (p. ej. `ErrorCode`) que define su mensaje y su estado HTTP.
  - Lanzar solo subclases de `ApplicationException`; no usar `ResponseStatusException` ni excepciones de estado de Spring.
  - Un único `GlobalExceptionHandler` (`@RestControllerAdvice`) traduce las excepciones a una respuesta de error consistente (DTO `ErrorResponse`).
  - Toda excepción crítica o inesperada del sistema (incluidas las de terceros o `Exception` genérica) se **envuelve o traduce a una excepción custom** antes de responder. Al cliente nunca le llega un stack trace ni una excepción cruda.

## 8. Comandos

```bash
docker compose up -d --build     # levantar todo el stack
docker compose down              # bajar el stack (conserva los datos del volumen)
docker compose logs -f backend   # ver logs de un servicio
cd backend && ./mvnw test        # tests del backend
cd frontend && npm test          # tests del frontend
```

> En Git Bash **no** hagas `source .env` antes de `docker compose`: las variables exportadas pisan al `.env` y MSYS reescribe rutas. Si hace falta, usá `MSYS_NO_PATHCONV=1`.
