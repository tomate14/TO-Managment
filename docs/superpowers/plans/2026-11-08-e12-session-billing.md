# É12 — Plan de implementación: facturación de sesiones por diagnóstico

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para ejecutar este plan tarea por tarea. Los pasos usan checkboxes (`- [ ]`).

**Objetivo:** cada diagnóstico muestra cuántas sesiones (seguimientos) tiene, cuántas están facturadas y cuántas pendientes. La terapeuta registra facturaciones (sesiones, fecha y monto) y las puede anular.

**Arquitectura:**
- **Backend:** paquete nuevo `billing` con las entidades `Billing` y `BillingFollowUp` (con `@TenantId`) y un servicio interfaz + `Impl`. Un índice único parcial en la base impide facturar dos veces un seguimiento.
- **Integraciones** por puertos chicos, como el `FollowUpCounter` que ya existe:
  - el diagnóstico pide el conteo de pendientes a una interfaz `PendingBillingCounter`;
  - los seguimientos piden sus marcas a una interfaz `BilledFollowUpLookup`.
- **Frontend:** componente `billing-section` en el detalle del diagnóstico; formulario `billing-form` reutilizado como página en móvil y como panel en escritorio.

**Stack:** Java 21, Spring Boot 3.5, JPA/Hibernate (`@TenantId`), Flyway, Postgres, JUnit 5 + Testcontainers. Angular 21 (standalone, signals, formularios reactivos), Vitest.

**Spec:** `docs/superpowers/specs/2026-11-08-session-billing-design.md`. **Mockups:** `docs/mockups/h12-facturacion/` (el README describe cada PNG).

**Worktree / rama:** `C:\Users\Maxi\Desktop\TO Managment-billing`, rama `feat/session-billing` (desde `main`).

## Restricciones globales

- **Idioma:** UI y mensajes en español (textos exactos abajo). Código, identificadores, commits y archivos en inglés. Cada commit termina con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Diseño del backend (CLAUDE.md):**
  - nada de `import *`;
  - servicios con interfaz + `Impl`, inyección por constructor;
  - valores del dominio en enums;
  - solo subclases de `ApplicationException`, cada una con su `ErrorCode`;
  - nunca se loguean montos, datos personales ni clínicos;
  - DTOs con `toString` enmascarado si llevan el monto.
- **Esquema:**
  - Migración `backend/src/main/resources/db/migration/V14__session_billing.sql`.
  - Su reflejo, en el mismo cambio, en `supabase/init/01_schema.sql`: `CREATE TABLE IF NOT EXISTS` y `CREATE [UNIQUE] INDEX IF NOT EXISTS`, con el encabezado en "V1..V14".
  - En `02_seed.sql`, el comentario `-- (É12 no define datos iniciales.)`.
- **Tenancy:** cada tabla nueva lleva `establishment_id BIGINT NOT NULL REFERENCES establishment (id)`, y su entidad `@TenantId @Column(name = "establishment_id", nullable = false, updatable = false) private Long establishmentId;`.
- **Textos exactos:**
  - ErrorCodes nuevos, antes de `INTERNAL_ERROR`:
    - `BILLING_NOT_FOUND` (404, "La facturación no existe.");
    - `BILLING_SESSIONS_REQUIRED` (422, "Elegí al menos una sesión.");
    - `BILLING_AMOUNT_INVALID` (422, "El monto tiene que ser mayor a cero.");
    - `BILLING_DATE_IN_FUTURE` (422, "La fecha no puede ser futura.");
    - `BILLING_SESSION_INVALID` (422, "Hay sesiones que no son de este diagnóstico o ya están facturadas.").
  - Validación de bean: "Ingresá el monto." y "Ingresá la fecha de facturación.".
  - UI:
    - "Facturación", "sesiones", "facturadas", "pendientes";
    - "Registrar facturación", "Facturaciones anteriores";
    - "n sesiones · $ 25.000,00 · Facturado el 2 de noviembre de 2026" (en móvil, el monto a la derecha);
    - "Ver sesiones" / "Ocultar sesiones", "Anular";
    - "¿Anular esta facturación? Sus sesiones vuelven a quedar pendientes.", "Sí, anular", "Cancelar", "Anulada";
    - "Todavía no hay sesiones para facturar.";
    - "Sesiones pendientes", "x de y elegidas", "Fecha de facturación", "Monto";
    - "n sesiones pendientes de facturar" (con 1, "1 sesión pendiente de facturar"), "Facturada".
  - Montos en formato es-AR con `$` y 2 decimales. Fechas largas en español ("2 de noviembre de 2026").
- **Verificación: solo compilación** (instrucción del usuario). Se escriben los tests, pero no se corren las suites.
  - Backend, desde `backend/`: `MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-billing/backend:/app" -v to-management-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test-compile`
  - Frontend, desde `frontend/`: `npx ng build` (sin errores ni warnings) y `npx tsc -p tsconfig.spec.json --noEmit`.
  - Si falta `frontend/node_modules`, se crea como **junction** a `C:\Users\Maxi\Desktop\TO Managment\frontend\node_modules` (`cmd //c mklink /J ...`). Nunca se borra con `rm -rf`, y no se corre `npm install`.

## Foco de revisión

1. **Dos personas facturando a la vez las mismas sesiones:** la segunda recibe 422 `BILLING_SESSION_INVALID` y no un 500. Se fija en la Tarea 1 (la carrera con el índice único se traduce a ese código).
2. **Volver a facturar una sesión de una facturación anulada:** se permite. Se fija en la Tarea 1.
3. **El monto con coma o punto** ("25.000,50", "25000.5"): se interpreta bien, y un texto inválido muestra un error. Se fija en la Tarea 3.
4. **Fecha de hoy en Argentina a la noche** (UTC ya es mañana): "hoy" se acepta y "mañana" no. Se compara con el `Clock` en `APP_TIME_ZONE`. Se fija en la Tarea 1 con un reloj fijo.
5. **Diagnóstico sin seguimientos:** muestra "Todavía no hay sesiones para facturar." sin el botón. Se fija en la Tarea 3.

---

### Tarea 1: Núcleo de facturación (backend)

**Archivos:**
- Crear la migración `V14__session_billing.sql`.
- Crear el paquete `backend/src/main/java/com/tomanagement/billing/`:
  - `entity/Billing.java` y `entity/BillingFollowUp.java`;
  - `repository/BillingRepository.java` y `repository/BillingFollowUpRepository.java`;
  - `dto/CreateBillingRequest.java`, `dto/BillingSummaryResponse.java`, `dto/BillingResponse.java` y `dto/BilledSessionRef.java`;
  - `service/BillingService.java` y `service/BillingServiceImpl.java`;
  - `controller/BillingController.java`.
- Modificar: `common/exception/ErrorCode.java`, `supabase/init/01_schema.sql` y `supabase/init/02_seed.sql`.
- Tests:
  - crear `backend/src/test/java/com/tomanagement/billing/controller/BillingControllerTest.java`;
  - crear `.../billing/entity/BillingTest.java`;
  - modificar `.../tenant/CrossEstablishmentIsolationTest.java`.

**Interfaces que produce:**
- JSON del resumen:
  - `{ totalSessions, billedSessions, pendingSessions, pending: [BilledSessionRef], billings: [BillingResponse] }`;
  - `BilledSessionRef` es `{ followUpId, date, segments: string[] }`, en orden por fecha ascendente;
  - `BillingResponse` es `{ id, billedOn, amount, sessionCount, voided, voidedAt, createdAt, sessions: [BilledSessionRef] }`;
  - las facturaciones van ordenadas por `billedOn` descendente y después por `id` descendente.
- Endpoints, con usuario autenticado y sin `@PreAuthorize` de rol:
  - `GET /api/diagnoses/{diagnosisId}/billing`;
  - `POST /api/diagnoses/{diagnosisId}/billings` (201), que recibe `{ billedOn, amount, followUpIds }`;
  - `POST /api/billings/{id}/void`.

  Los tres devuelven el resumen.

- [ ] **Paso 1: Migración.**

```sql
-- É12: billing of follow-up sessions per diagnosis. A follow-up is billed while it has an active link.
CREATE TABLE billing (
    id               BIGSERIAL      PRIMARY KEY,
    establishment_id BIGINT         NOT NULL REFERENCES establishment (id),
    diagnosis_id     BIGINT         NOT NULL REFERENCES diagnosis (id),
    billed_on        DATE           NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at       TIMESTAMPTZ    NOT NULL,
    voided_at        TIMESTAMPTZ
);
CREATE INDEX ix_billing_diagnosis ON billing (diagnosis_id, billed_on DESC);

CREATE TABLE billing_follow_up (
    billing_id       BIGINT  NOT NULL REFERENCES billing (id),
    follow_up_id     BIGINT  NOT NULL REFERENCES follow_up (id),
    establishment_id BIGINT  NOT NULL REFERENCES establishment (id),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (billing_id, follow_up_id)
);
CREATE UNIQUE INDEX ux_billing_follow_up_active ON billing_follow_up (follow_up_id) WHERE active;
```

  Lo mismo va en `01_schema.sql`, con `IF NOT EXISTS` y bajo el comentario `-- Facturación de sesiones (É12)`, después de los seguimientos.

- [ ] **Paso 2: Tests.**
  - **`BillingTest` (unitario):**
    - `void(Instant)` pone `voidedAt` y desactiva todos los vínculos;
    - anular otra vez no cambia `voidedAt`.
  - **`BillingControllerTest`:**
    - Setup:
      - copiar el de `FollowUpControllerTest` (truncado, `tenancy.create`, terapeuta, paciente, diagnóstico conservador con `new ConservativeTreatment(true, ...)`);
      - sembrar 4 seguimientos del diagnóstico con fechas distintas, más 1 de otro diagnóstico;
      - el `Clock` se fija con una clase `@TestConfiguration` y un `@Primary Clock` en `2026-11-08T23:30:00-03:00`, como hacen otros tests del proyecto con reloj fijo.
    - Casos:
      - `summaryCountsSessions`: total 4, facturadas 0, pendientes 4 en orden por fecha, y sin facturaciones.
      - `registersABillingOfPendingSessions`: POST con 3 ids, `billedOn` 2026-11-08 y `amount` 25000.50 → 201, facturadas 3 y pendientes 1. La facturación trae `sessionCount` 3, `amount` 25000.5 y `voided` false.
      - `needsAtLeastOneSession`: `followUpIds: []` → 422 `BILLING_SESSIONS_REQUIRED`.
      - `amountMustBePositive`: 0 → 422 `BILLING_AMOUNT_INVALID`; null → 400 `VALIDATION_ERROR`.
      - `dateCannotBeInTheFuture`: con el reloj fijo, `billedOn` 2026-11-08 (hoy en Argentina) → 201; 2026-11-09 → 422 `BILLING_DATE_IN_FUTURE`.
      - `rejectsSessionsOfAnotherDiagnosisRepeatedOrAlreadyBilled`: las tres situaciones → 422 `BILLING_SESSION_INVALID`.
      - `voidingReturnsSessionsToPending`: anular → pendientes vuelve a 4 y la facturación queda `voided` true. Anular otra vez → 200 con el mismo resultado.
      - `aSessionOfAVoidedBillingCanBeBilledAgain`.
      - `unknownBillingIs404`: anular un id inexistente → 404 `BILLING_NOT_FOUND`.
      - `unknownDiagnosisIs404`: GET de un diagnóstico inexistente → 404 `DIAGNOSIS_NOT_FOUND`.
  - **`CrossEstablishmentIsolationTest`:** con el token de A, el `GET /api/diagnoses/{b.diagnosisId}/billing` → 404 `DIAGNOSIS_NOT_FOUND`, y el `POST /api/billings/{idDeB}/void` → 404 `BILLING_NOT_FOUND`. Antes hay que crear la facturación de B por API con el token de B, usando el seguimiento `b.followUpId()`.

- [ ] **Paso 3: Entidades.**
  - **`Billing`:** `id`, `establishmentId` (`@TenantId`), `@ManyToOne(fetch = LAZY, optional = false) Diagnosis diagnosis` (`diagnosis_id`, no actualizable), `billedOn` (`LocalDate`), `amount` (`BigDecimal`, precision 12, scale 2), `createdAt`, `voidedAt` (nullable).
    - Colección: `@OneToMany(mappedBy = "billing", cascade = ALL) List<BillingFollowUp> links`.
    - Constructor: `Billing(Diagnosis diagnosis, LocalDate billedOn, BigDecimal amount, Instant createdAt, List<FollowUp> followUps)`, que crea los vínculos.
    - Métodos: `void(Instant when)` (idempotente: solo la primera vez pone `voidedAt` y desactiva los vínculos), `isVoided()` y `getFollowUps()`.
  - **`BillingFollowUp`:**
    - clave compuesta con `@EmbeddedId BillingFollowUpId(billingId, followUpId)`, con `@MapsId` hacia `billing` y `followUp`;
    - campos `establishmentId` (`@TenantId`) y `active`;
    - método `deactivate()`.

- [ ] **Paso 4: Servicio (interfaz + `Impl`, `@Transactional`).**
  - `summary(Long diagnosisId)`:
    1. busca el diagnóstico con el `DiagnosisRepository` filtrado por tenant (si no existe → `ResourceNotFoundException(DIAGNOSIS_NOT_FOUND)`);
    2. carga los seguimientos del diagnóstico;
    3. carga los ids facturados con un repositorio de vínculos activos;
    4. carga las facturaciones con sus vínculos.

    Los segmentos de cada sesión son los nombres de los segmentos de sus evaluaciones, en orden.
  - `register(Long diagnosisId, CreateBillingRequest request)`:
    - Validaciones, en este orden:
      1. lista vacía → `BILLING_SESSIONS_REQUIRED`;
      2. monto ≤ 0 → `BILLING_AMOUNT_INVALID`;
      3. `billedOn.isAfter(LocalDate.now(clock))` → `BILLING_DATE_IN_FUTURE`;
      4. algún id repetido, que no sea del diagnóstico o que ya esté facturado → `BILLING_SESSION_INVALID`.
    - Guarda con `saveAndFlush`. Una `DataIntegrityViolationException` (carrera con el índice parcial) se traduce a `BILLING_SESSION_INVALID`.
  - `voidBilling(Long billingId)`: busca la facturación (si no existe → `BILLING_NOT_FOUND`), llama a `void(Instant.now(clock))` y devuelve el resumen de su diagnóstico.
  - DTO:

```java
public record CreateBillingRequest(
        @NotNull(message = "Ingresá la fecha de facturación.") LocalDate billedOn,
        @NotNull(message = "Ingresá el monto.") BigDecimal amount,
        List<@NotNull Long> followUpIds) {
    public List<Long> followUpIdsOrEmpty() { return followUpIds == null ? List.of() : followUpIds; }
    @Override public String toString() { return "CreateBillingRequest[billedOn=" + billedOn + ", ***]"; }
}
```

- [ ] **Paso 5: Controlador y ErrorCodes**, con los textos de las restricciones globales.

- [ ] **Paso 6: Compilar el backend** y hacer commit.

```bash
git add -A backend supabase
git commit -m "feat: session billing per diagnosis (E12)"
```

---

### Tarea 2: Integraciones con diagnóstico y seguimientos (backend)

**Archivos:**
- Crear:
  - `backend/src/main/java/com/tomanagement/diagnosis/service/PendingBillingCounter.java` (interfaz);
  - `.../followup/service/BilledFollowUpLookup.java` (interfaz);
  - `.../billing/service/BillingCounterAdapter.java` (`@Service`, implementa las dos).
- Modificar:
  - `.../diagnosis/dto/DiagnosisResponse.java` (suma `long pendingBillingCount` al final), `DiagnosisMapper` y `DiagnosisServiceImpl` (lo calcula con el puerto, igual que `followUpCount`);
  - `.../followup/dto/FollowUpResponse.java` (suma `boolean billed` al final), `FollowUpMapper` y los servicios que lo llaman (registro y consulta).
- Tests: `DiagnosisControllerTest` (un caso) y `FollowUpControllerTest` (un caso).

**Interfaces:**

```java
/** Port: pending (unbilled) follow-ups per diagnosis, implemented by the billing feature. */
public interface PendingBillingCounter {
    Map<Long, Long> pendingByDiagnosis(Collection<Long> diagnosisIds);
}

/** Port: which of these follow-ups are in an active billing. */
public interface BilledFollowUpLookup {
    Set<Long> billedAmong(Collection<Long> followUpIds);
}
```

  `pendingByDiagnosis` cuenta en una sola consulta: los seguimientos de esos diagnósticos sin vínculo activo. Copiar el estilo de proyección de `FollowUpRepository.countByDiagnosisIds`. Los diagnósticos sin pendientes no aparecen en el mapa (0).

- [ ] **Paso 1: Tests.**
  - `DiagnosisControllerTest`, caso `listShowsPendingBillingCount`: con 2 seguimientos y uno facturado (sembrado con `BillingRepository` dentro de `tenancy.runAs`), la lista del paciente trae `pendingBillingCount` 1.
  - `FollowUpControllerTest`, caso `followUpsCarryTheBilledMark`: el seguimiento facturado trae `billed` true y el otro, false.
- [ ] **Paso 2: Implementar.**
  - Ajustar cada constructor de `DiagnosisResponse` y `FollowUpResponse`. Para encontrarlos: `grep -rn "new DiagnosisResponse\|new FollowUpResponse" backend/src`.
  - El mapper de seguimientos recibe el `Set<Long>` de facturados, que calcula el servicio con el puerto antes de mapear.
- [ ] **Paso 3: Compilar el backend** y hacer commit: `feat: pending billing count on diagnoses and billed mark on follow-ups (E12)`.

---

### Tarea 3: Sección Facturación y formulario (frontend)

**Archivos:**
- Crear en `frontend/src/app/features/billing/`:
  - `billing.models.ts` y `billing.service.ts` (+ `.spec.ts`);
  - `billing-format.ts` (+ `.spec.ts`), con `formatAmount(n)` → "$ 25.000,00", `formatLongDate(iso)` → "2 de noviembre de 2026" y `parseAmount(raw)`, que acepta "25.000,50", "25000,5" y "25000.5", o devuelve null;
  - `billing-section/billing-section.{ts,html,scss,spec.ts}`;
  - `billing-form/billing-form.{ts,html,scss,spec.ts}`;
  - `billing-form-page/billing-form-page.{ts,html,scss,spec.ts}`.
- Modificar:
  - `frontend/src/app/app.routes.ts`: la ruta `:id/diagnosticos/:diagnosisId/facturacion/nueva` con `BillingFormPage` y el título `Registrar facturación · ${APP_NAME}`;
  - `features/diagnoses/diagnosis-page/diagnosis-page.{ts,html,spec.ts}`: inserta `<app-billing-section>` antes de "Seguimientos".

**Interfaces:**

```ts
export interface BilledSessionRef { followUpId: number; date: string; segments: string[]; }
export interface Billing { id: number; billedOn: string; amount: number; sessionCount: number; voided: boolean; voidedAt: string | null; createdAt: string; sessions: BilledSessionRef[]; }
export interface BillingSummary { totalSessions: number; billedSessions: number; pendingSessions: number; pending: BilledSessionRef[]; billings: Billing[]; }
export interface CreateBillingPayload { billedOn: string; amount: number; followUpIds: number[]; }
// BillingService (providedIn root)
summary(diagnosisId: number): Observable<BillingSummary>
register(diagnosisId: number, payload: CreateBillingPayload): Observable<BillingSummary>
voidBilling(billingId: number): Observable<BillingSummary>
```

- **`BillingForm`:**
  - entradas: `diagnosisId` y `pending: BilledSessionRef[]`;
  - salidas: `saved: BillingSummary` y `cancelled`;
  - el formulario: casillas (todas marcadas al iniciar), `billedOn` (por defecto `todayIso()`, con `max` igual a hoy) y `amount` (texto con prefijo `$`, `inputmode="decimal"`).
- **`BillingSection`:**
  - entrada `diagnosisId`; carga el resumen;
  - en escritorio abre el formulario en un panel (patrón de `EstablishmentsPage`: `injectIsDesktop`, `<aside role="dialog" aria-modal="true">`, re-creación por target con `@for … track`); en móvil navega a la página;
  - "Ver sesiones" alterna cada facturación;
  - "Anular" pide confirmación en línea y llama a `voidBilling`;
  - salida `changed` para que el detalle refresque los seguimientos y sus marcas.

- [ ] **Paso 1: Specs.**
  - Service: las URLs, los métodos y los cuerpos.
  - `billing-format`: los casos de monto y de fecha larga.
  - Form:
    - todas las pendientes marcadas al abrir;
    - "x de y elegidas";
    - sin sesiones → "Elegí al menos una sesión.";
    - monto vacío → "Ingresá el monto.";
    - monto "abc" o 0 → "El monto tiene que ser mayor a cero.";
    - el payload convierte "25.000,50" en 25000.5;
    - un error del servidor `BILLING_SESSION_INVALID` muestra su mensaje arriba.
  - Section:
    - los contadores;
    - el botón solo con pendientes;
    - sin sesiones → "Todavía no hay sesiones para facturar.";
    - la fila de cada facturación con el monto formateado y la fecha larga;
    - "Ver sesiones" / "Ocultar sesiones";
    - anular con confirmación;
    - la anulada con "Anulada" y sin "Anular";
    - en escritorio el botón abre el panel y en móvil navega.
  - Diagnosis page: renderiza la sección.
- [ ] **Paso 2: Implementar** según `mobile-facturacion.png`, `mobile-registrar.png`, `mobile-anular.png`, `desktop-facturacion.png` y `desktop-registrar-panel.png`. Los estilos van en el scss propio de cada componente, mobile-first.
- [ ] **Paso 3: Compilar el frontend** y hacer commit: `feat: billing section and form on the diagnosis (E12)`.

---

### Tarea 4: Marcas en las tarjetas (frontend)

**Archivos:**
- Modificar:
  - `features/diagnoses/diagnoses.models.ts`: `Diagnosis.pendingBillingCount: number`;
  - `features/diagnoses/diagnosis-card/diagnosis-card.{ts,html,scss,spec.ts}`: muestra "n sesiones pendientes de facturar" si es mayor a 0, o "1 sesión pendiente de facturar";
  - `features/follow-ups/follow-ups.models.ts`: `FollowUp.billed: boolean`;
  - `features/follow-ups/follow-up-card/follow-up-card.{html,scss,spec.ts}`: el badge "Facturada" si `billed`.
  - Los fixtures de specs que construyen `Diagnosis` o `FollowUp`. Para encontrarlos: `grep -rn "followUpCount\|scarType:" frontend/src/app --include=*.spec.ts`.
- [ ] **Paso 1: Specs:** el texto en singular y en plural, sin texto con 0, y el badge solo cuando está facturado.
- [ ] **Paso 2: Implementar** según `mobile-tarjetas.png`.
- [ ] **Paso 3: Compilar el frontend** y hacer commit: `feat: billing marks on diagnosis and follow-up cards (E12)`.

---

### Tarea 5: Chequeo de runtime, merge a test y prueba local (la hace el controlador)

- [ ] Correr una vez las clases de integración clave:

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-billing/backend:/app" -v to-management-m2:/root/.m2 -v //var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test -Dtest='BillingTest,BillingControllerTest,DiagnosisControllerTest,FollowUpControllerTest,CrossEstablishmentIsolationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

- [ ] Correr `01_schema.sql` y `02_seed.sql` dos veces sobre un `postgres:15-alpine` vacío, con `-v app_schema=app`.
- [ ] Mergear a `test` en la carpeta principal y hacer `docker compose up -d --build`. Confirmar que V14 se aplicó y que el backend arrancó (esperar unos 15 s: reconstruir el frontend también reinicia el backend).
- [ ] Prueba manual por API, con un consultorio de prueba local:
  1. resumen;
  2. registrar con 2 sesiones;
  3. repetir una sesión → 422;
  4. anular;
  5. volver a facturar.

## Flujo de ramas

`feat/session-billing` → `test` para la prueba manual. A `main` llega solo la feat, nunca `test`. A producción va con los comandos de `docs/cloud-run.md` (backend y frontend). No hay variables de entorno nuevas.
