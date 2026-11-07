# É10 — Período de prueba: diseño

Fecha: 2026-11-07 · Rama: `feat/trial-period` (desde `feat/independent-therapists`) · Depende de: [É9 terapeutas independientes](2026-11-06-independent-therapists-design.md)

## 1. Objetivo

Todo establecimiento que el superadmin aprueba (o crea) recibe **7 días de prueba gratis**. Vencido el plazo, la cuenta se **bloquea**: nadie del establecimiento puede ingresar ni seguir usando la app, hasta que el superadmin extienda la fecha o quite el vencimiento.

Éxito:
- Al aprobar, quedan registradas la fecha de aprobación y la de vencimiento.
- Al día siguiente del vencimiento, el login muestra "Tu período de prueba venció" y una sesión abierta se corta en su próxima acción.
- El superadmin ve el vencimiento de cada establecimiento y lo puede cambiar o quitar.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Alcance | **Todos** los establecimientos: instituciones e independientes. |
| Duración | 7 días, configurable con la variable nueva `TRIAL_DURATION_DAYS` (`.env`). |
| Fecha de aprobación | Se reutiliza `establishment.approved_at` (ya existe desde É9). Solo se agrega la de vencimiento. |
| Desbloqueo | El superadmin fija una nueva fecha o quita el vencimiento (cuenta paga). |
| Momento del bloqueo | Al login **y** en cada request autenticado. |
| Existentes | Los establecimientos que ya existen al migrar quedan **sin vencimiento** (nadie que ya trabaja se bloquea). |

## 3. Historias

- **H10.1 Inicio de la prueba.** Al aprobar una solicitud o crear un establecimiento, el sistema guarda `approved_at = ahora` y `trial_ends_at = ahora + TRIAL_DURATION_DAYS`. Aprobar de nuevo un establecimiento ya aprobado (repetición idempotente) no cambia ninguna de las dos fechas. Una solicitud rechazada nunca fue aprobada, así que al aprobarla recibe sus 7 días.
- **H10.2 Bloqueo.** Si `trial_ends_at` ya pasó, el login responde "Tu período de prueba venció. Contactá al administrador de la plataforma." Una sesión abierta recibe el mismo error en su próximo request; la app cierra la sesión y vuelve al login con ese aviso.
- **H10.3 Gestión del vencimiento.** En Establecimientos, cada fila muestra "Prueba hasta dd/mm/aaaa", el badge "Vencida" o "Sin vencimiento". La acción "Vencimiento" abre un diálogo para elegir una nueva fecha o "Quitar vencimiento".

## 4. Modelo de datos

Migración `V12__trial_period.sql` y reflejo en `supabase/init/01_schema.sql` (idempotente, `ADD COLUMN IF NOT EXISTS`):
- `establishment.trial_ends_at timestamptz NULL`. Sin backfill: los existentes quedan en `NULL`.
- Estados derivados (además de los de É9):
  - sin vencimiento: `trial_ends_at IS NULL`;
  - vigente: `trial_ends_at > now()`;
  - vencida: `trial_ends_at <= now()`.

## 5. Backend

- **Configuración:** `TrialProperties` (`@ConfigurationProperties`, `@Validated`, `@Positive durationDays`) leída de `TRIAL_DURATION_DAYS` en `application.yml`. Se agrega a `.env`, `.env.example` (7), `docker-compose.yml` (`:?` obligatoria), `application-test.yml` y `docs/cloud-run.md`.
- **Enums:**
  - `ErrorCode.TRIAL_EXPIRED` (403, "Tu período de prueba venció. Contactá al administrador de la plataforma.").
  - `ErrorCode.TRIAL_END_IN_PAST` (400, "La fecha de vencimiento no puede ser anterior a hoy.").
- **Entidad `Establishment`:**
  - campo `trialEndsAt` (`Instant`, nullable);
  - `approve(Instant when, Duration trial)`: si es la primera aprobación (`approvedAt == null`) setea `approvedAt = when` y, si `trialEndsAt` es null, `trialEndsAt = when + trial`; siempre reactiva;
  - `changeTrialEnd(Instant endsAt)` (null = sin vencimiento);
  - `isTrialExpired(Instant now)`.
- **Creación y aprobación:** `EstablishmentCreatorImpl` (nace aprobado) y `EstablishmentServiceImpl.approve` pasan la duración de `TrialProperties` y el `Clock`.
- **Login:** `AuthServiceImpl.login`, después de inactivo y pendiente: si la prueba venció → 403 `TRIAL_EXPIRED`.
- **Cada request:** filtro nuevo `TrialExpirationFilter`, agregado en `SecurityConfig` después de `JwtAuthenticationFilter`.
  - Si hay un usuario autenticado con rol distinto de `SUPERADMIN` y `establishmentId`, consulta el vencimiento con un componente propio (`TrialStatusChecker` / `Impl`, lectura por clave primaria de una sola columna).
  - Si venció, responde 403 `TRIAL_EXPIRED` con el `ErrorResponse` habitual (`ErrorResponseWriter`) y no sigue la cadena.
  - El superadmin nunca se bloquea, tampoco cuando entra a un establecimiento vencido con "cambiar establecimiento".
- **Superadmin:**
  - `EstablishmentSummaryResponse` suma `trialEndsAt` (nullable) y `trialExpired`.
  - Nuevo `PUT /api/establishments/{id}/trial` (SUPERADMIN) con `ChangeTrialEndRequest { trialEndsOn: LocalDate | null }`.
    - La fecha es el último día de prueba: se guarda como el inicio del día siguiente en `APP_TIME_ZONE` (la cuenta se bloquea a la medianoche).
    - `null` quita el vencimiento.
    - Una fecha anterior a hoy (en `APP_TIME_ZONE`) → 400 `TRIAL_END_IN_PAST`.
    - Responde el `EstablishmentSummaryResponse` actualizado.

## 6. Frontend

- **Interceptor:** un 403 con código `TRIAL_EXPIRED` cierra la sesión y navega a `/login` indicando el motivo, igual que el manejo del 401.
- **Login:** `classify()` suma el tipo `'expired'` con el texto de H10.2 (mismo estilo de banner que `'pending'`).
- **Establecimientos:**
  - cada fila: "Prueba hasta dd/mm/aaaa", badge "Vencida" o "Sin vencimiento";
  - acción "Vencimiento": diálogo con un campo fecha (mínimo hoy), "Guardar" y "Quitar vencimiento"; error `TRIAL_END_IN_PAST` en el campo;
  - la fila se actualiza con la respuesta.
- **Mockups** (requisito para codificar) en `docs/mockups/h10-prueba/`, mobile y desktop:
  - login con el aviso de prueba vencida;
  - Establecimientos con filas vigente, vencida y sin vencimiento;
  - diálogo de vencimiento.

## 7. Pruebas

Se escriben los tests; la verificación es compile-only (instrucción vigente), salvo una corrida de las clases de integración clave, como en É9, para asegurar que el contexto arranca.
- Entidad: la primera aprobación fija el vencimiento; aprobar otra vez no lo cambia; `isTrialExpired` en el borde exacto.
- Creación por superadmin: nace con vencimiento a 7 días (reloj fijo).
- Login: vencida da 403 `TRIAL_EXPIRED`; vigente y sin vencimiento dan 200; inactiva sigue dando `ESTABLISHMENT_INACTIVE`.
- Filtro: un token válido de un establecimiento vencido da 403 `TRIAL_EXPIRED` en un endpoint protegido; el superadmin dentro de ese establecimiento no se bloquea; endpoints públicos no consultan nada.
- Endpoint de vencimiento: solo SUPERADMIN; fecha futura guarda el inicio del día siguiente en la zona; `null` quita; fecha pasada da 400.
- Frontend: interceptor con `TRIAL_EXPIRED`; login tipo `'expired'`; fila con los tres estados; diálogo guardar, quitar y error.

## 8. Flujo de ramas y despliegue

`feat/trial-period` sale de `feat/independent-therapists` porque usa `approved_at`. Se mergea a `test` para la prueba manual; a `main` solo después de que É9 esté en `main` (nunca `test`). V12 no borra datos. Producción necesita `TRIAL_DURATION_DAYS` en Cloud Run.

## 9. Fuera de alcance

Pagos y planes, avisos previos al vencimiento ("te quedan 2 días"), emails, historial de extensiones y bloqueo por request de establecimientos inactivos (sigue siendo solo al login).
