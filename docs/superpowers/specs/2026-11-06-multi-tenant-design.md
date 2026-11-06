# É8 — Multi-tenencia por establecimiento (diseño)

Fecha: 2026-11-06. Amplía el spec general `2026-11-04-gestion-terapia-ocupacional-design.md` y el de la agenda `2026-11-05-agenda-design.md`.
Mockups: `docs/mockups/h8-establecimientos/` (se generan y aprueban antes de codificar).

## 1. Objetivo

Que la app sirva a varios **establecimientos** (consultorios, clínicas). Cada establecimiento tiene un nombre, sus usuarios y sus datos.
Dentro de un establecimiento todo se comparte (pacientes, segmentos, tipos de cirugía y de cicatriz, plantillas y preguntas,
diagnósticos, seguimientos y agenda). Entre establecimientos no se comparte ni se ve nada.

**Éxito:** dos establecimientos usan la app al mismo tiempo sin ver datos del otro, y cada admin gestiona solo el suyo.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Alta de establecimientos | Un rol nuevo de plataforma, **SUPERADMIN**, crea establecimientos con su nombre y su primer admin. El admin inicial del `.env` (`ADMIN_*`) pasa a ser el superadmin. |
| Superadmin y datos clínicos | El superadmin **puede entrar a cualquier establecimiento** y ver/gestionar todo como su admin (soporte). Fuera de un establecimiento solo administra establecimientos. |
| Admin | Gestiona solo su establecimiento: su nombre y sus usuarios. Los usuarios que crea quedan en su establecimiento. |
| Usuarios | Cada usuario (ADMIN o TERAPEUTA) pertenece a **un** establecimiento. El email sigue siendo único en todo el sistema (el login es por email). |
| Datos existentes | **Se empieza de cero**: la migración vacía las tablas de la app. En producción se ejecuta solo con backup previo y confirmación explícita del usuario. |
| Catálogos de un establecimiento nuevo | Arranca con una **copia** de los valores por defecto: segmentos Hombro, Codo, Muñeca, Mano, Dedos; tipos de cirugía Osteosíntesis con placa, Tenorrafia, Liberación del túnel carpiano, Artrodesis; tipos de cicatriz Normotrófica, Hipertrófica, Queloide, Retráctil. Después cada establecimiento los edita sin afectar a otros. |
| DNI | Único **por establecimiento** (el mismo paciente puede tener ficha en dos establecimientos, independientes). |
| Establecimiento inactivo | El superadmin puede desactivarlo: sus usuarios no pueden iniciar sesión. No se borra. |

## 3. Historias

**H8.1 Superadmin crea un establecimiento**
- Ingresa el nombre del establecimiento y los datos del primer admin (nombre, email, contraseña inicial).
- Se crea todo en una sola operación: establecimiento, admin y copia de los catálogos por defecto.
- El nombre es único ignorando mayúsculas; el email del admin, único en el sistema.

**H8.2 Superadmin administra establecimientos**
- Lista de establecimientos con nombre, admin(s), cantidad de usuarios y estado; puede renombrar, activar y desactivar.

**H8.3 Superadmin entra a un establecimiento**
- "Entrar" lo lleva a la app de ese establecimiento con permisos de admin; un aviso fijo indica "Estás en ‹nombre›" con "Salir" para volver a la lista.

**H8.4 Admin ve y renombra su establecimiento**
- El nombre se ve en el menú; en Administración hay una pantalla "Establecimiento" para cambiarlo.

**H8.5 Aislamiento**
- Todo lo que se lista, busca, crea, edita o borra queda dentro del establecimiento de la sesión. Un recurso de otro establecimiento responde "no existe" (404).
- Las relaciones también: no se puede reservar un turno con un terapeuta o paciente de otro establecimiento, ni usar un segmento, plantilla, tipo de cirugía o de cicatriz ajeno.

## 4. Modelo de datos

Migración Flyway **V10**, reflejada en `supabase/init/01_schema.sql` y `02_seed.sql`.

```
establishment
  id          BIGSERIAL PK
  name        VARCHAR(120) NOT NULL   -- único: lower(name)
  active      BOOLEAN NOT NULL DEFAULT true
  created_at  TIMESTAMPTZ NOT NULL
```

- V10 empieza con `TRUNCATE` de todas las tablas de la app (incluido `app_user`, `RESTART IDENTITY CASCADE`). El superadmin se recrea al arrancar.
- `establishment_id BIGINT NOT NULL REFERENCES establishment(id)` en: `body_segment`, `surgery_type`, `scar_type`, `evaluation_template`,
  `question`, `question_option`, `patient`, `diagnosis`, `treatment` (tabla base de la herencia), `follow_up`, `segment_evaluation`, `answer`, `appointment`.
- `app_user.establishment_id BIGINT NULL REFERENCES establishment(id)` (NULL solo para SUPERADMIN, `CHECK` que lo garantiza según el rol).
- Rol `SUPERADMIN` agregado al `CHECK` de roles de `app_user`.
- Unicidades: `(establishment_id, lower(name))` en los tres catálogos (reemplazan a las globales); `(establishment_id, dni)` en paciente (reemplaza a la global);
  plantilla por `(body_segment_id, lower(name))` queda dentro del establecimiento porque el segmento lo es; email sigue global.
- Índices por `establishment_id` en las tablas raíz consultadas directamente (paciente, turno, catálogos, plantilla).
- `02_seed.sql` deja de sembrar catálogos globales: los catálogos se crean por establecimiento desde el backend. `V4` no se edita (ya aplicada); V10 elimina sus filas.

## 5. Backend

**Aislamiento con Hibernate 6 `@TenantId`.** Todas las entidades de datos listadas tienen `@TenantId Long establishmentId`. Hibernate:
- agrega `establishment_id = :tenant` a todas las consultas HQL/JPQL/Criteria/Specification (incluidos `findById`, `exists…`, conteos y las consultas de solapamiento de turnos);
- completa el valor al insertar.

`CurrentTenantIdentifierResolver` (bean de Spring) toma el establecimiento del `AuthenticatedUser` del `SecurityContext`. Para procesos de sistema
(bootstrap del superadmin, siembra de catálogos al crear un establecimiento) se usa un contexto de tenant explícito y acotado (`TenantContext`),
nunca un "ver todo" implícito en una petición de usuario. Una petición de datos sin establecimiento en la sesión responde `ESTABLISHMENT_REQUIRED` (403).

**Usuarios** (`app_user`) no llevan `@TenantId` (el login busca por email sin tenant). Se filtran explícitamente por el establecimiento de la sesión en
la gestión de usuarios, en la lista de terapeutas de la agenda y al validar el terapeuta de un turno (un terapeuta de otro establecimiento → "no existe").

**Auth**
- `Role`: `SUPERADMIN`, `ADMIN`, `TERAPEUTA`.
- JWT: claim nuevo `establishment` (id; ausente para el superadmin fuera de un establecimiento). `AuthenticatedUser(id, email, fullName, role, establishmentId)`.
- Login rechaza usuarios de un establecimiento inactivo (`ESTABLISHMENT_INACTIVE`).
- `POST /api/auth/establishment` `{ establishmentId }` / `{ establishmentId: null }` — solo SUPERADMIN: reemite el token entrando/saliendo de un establecimiento (activo).
- Dentro de un establecimiento, el SUPERADMIN tiene los permisos de ADMIN (`@PreAuthorize` acepta ambos; la política de visibilidad de la agenda lo trata como admin).
- `GET /api/auth/me` y la respuesta del login incluyen `establishment { id, name } | null`.

**API nueva**
| Método y ruta | Quién | Descripción |
|---|---|---|
| `GET /api/establishments` | SUPERADMIN | Lista: id, nombre, activo, cantidad de usuarios, admins (nombre y email). |
| `POST /api/establishments` | SUPERADMIN | `{ name, admin { fullName, email, password } }` → crea establecimiento + admin + catálogos por defecto (una transacción). 201. |
| `PUT /api/establishments/{id}` | SUPERADMIN | Renombrar. |
| `PATCH /api/establishments/{id}/active` | SUPERADMIN | Activar/desactivar. |
| `GET /api/establishment` | ADMIN / SUPERADMIN dentro | El establecimiento de la sesión. |
| `PUT /api/establishment` | ADMIN / SUPERADMIN dentro | Renombrar el propio. |

- Catálogos por defecto modelados como `enum DefaultCatalogEntry` (tipo de catálogo + nombre), sin constantes sueltas.
- Servicios interfaz + implementación (`EstablishmentService`, `CurrentEstablishmentService`), DTOs validados, excepciones custom.
- Errores nuevos: `ESTABLISHMENT_NOT_FOUND` (404), `ESTABLISHMENT_NAME_TAKEN` (409), `ESTABLISHMENT_INACTIVE` (403 en login / 422 al entrar), `ESTABLISHMENT_REQUIRED` (403).
- `InitialAdminService`: si no hay ningún SUPERADMIN, crea el del `.env` con rol SUPERADMIN y sin establecimiento.
- La gestión de usuarios del admin solo ofrece los roles ADMIN y TERAPEUTA, y crea los usuarios en el establecimiento de la sesión.

## 6. Frontend

- Superadmin **fuera** de un establecimiento: navegación reducida a **Establecimientos** (lista + "Nuevo establecimiento" + "Entrar"), más Cambiar contraseña y Cerrar sesión.
- Superadmin **dentro**: navegación completa de admin y un aviso fijo "Estás en ‹nombre› · Salir".
- Admin y terapeutas: el nombre del establecimiento se muestra en el encabezado del menú lateral y en "Más".
- Admin: pantalla **Establecimiento** (renombrar) en la sección Administración.
- `Role` suma `SUPERADMIN`; guards: `superadminGuard` (lista de establecimientos), `adminGuard` acepta SUPERADMIN dentro de un establecimiento, `establishmentGuard` para las rutas de datos (sin establecimiento en sesión → `/establecimientos`).
- Mockups (mobile y desktop): lista de establecimientos, nuevo establecimiento con primer admin, aviso de establecimiento activo, pantalla Establecimiento del admin.

## 7. Pruebas

- Integración (Testcontainers): aislamiento por recurso (pacientes, diagnósticos, seguimientos, turnos, catálogos, plantillas, usuarios y terapeutas de la agenda) entre dos establecimientos; unicidad por establecimiento (DNI y nombres de catálogo repetidos en A y B); alta de establecimiento con siembra de catálogos y admin; entrar/salir del superadmin; login bloqueado con establecimiento inactivo; bootstrap del superadmin.
- Unitarias: resolver de tenant, `DefaultCatalogEntry`, emisión/lectura del claim.
- Frontend: guards, navegación del superadmin, aviso de establecimiento, pantallas nuevas.
- Verificación por compilación (instrucción vigente del usuario) y scripts `supabase/init` corridos dos veces sobre un Postgres vacío.

## 8. Flujo de ramas y despliegue

`feat/multi-tenant` sale de `main` → merge a `test` para la prueba manual → con aprobación, merge de la feat a `main` (nunca `test`).
Antes de desplegar en producción: backup de Supabase y confirmación explícita (V10 borra los datos), luego Cloud Run y Vercel.

## 9. Fuera de alcance

Usuarios en más de un establecimiento, registro público de establecimientos, facturación/planes, transferir pacientes entre establecimientos,
auditoría de lo que hace el superadmin dentro de un establecimiento, y borrado de establecimientos.
