# Patient Appointment Billing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Facturar turnos confirmados por paciente, independientemente del diagnóstico y de los seguimientos clínicos, conservando el historial anterior.

**Architecture:** La confirmación se guarda en el turno. Las facturaciones nuevas pertenecen a pacientes y vinculan turnos mediante una tabla con unicidad de facturación vigente; los vínculos anteriores con seguimientos permanecen solo como historial. Se reutiliza el formulario de facturación en la ficha del paciente.

**Tech Stack:** Java 21, Spring Boot 3.5, Hibernate con `@TenantId`, Flyway, PostgreSQL, JUnit/Testcontainers; Angular 21, signals, formularios reactivos y Vitest.

**Spec:** `docs/superpowers/specs/2026-11-09-patient-appointment-billing-design.md`, aprobado el 2026-11-09. Sus diseños textuales y los mockups existentes son la referencia visual aprobada.

## Global Constraints

- Confirmar un turno genera una sesión pendiente de facturar para su paciente.
- Crear un seguimiento clínico nunca genera pendientes de facturación.
- Los seguimientos anteriores sin facturar dejan de aparecer como pendientes; no se borran.
- La facturación pertenece al paciente, no a un diagnóstico.
- Las facturaciones anteriores y sus sesiones se conservan como historial.
- Un turno confirmado no se puede cancelar ni reprogramar. No agregar reversión de confirmaciones ni restricción por fecha.
- Respetar CLAUDE.md: interfaz española, código inglés, DTOs, excepciones propias, enums, interfaces de servicio, imports explícitos, aislamiento por establecimiento, mobile-first desde 360 px.
- No usar datos clínicos reales en tests ni logs; no leer ni copiar `.env.hosted` a artefactos o commits.
- No desplegar, migrar producción ni hacer merge a `main` como parte de la implementación.
- El requisito vigente de TDD proviene de la solicitud y CLAUDE.md. La excepción de solo compilación anotada en el plan anterior no se traslada a este cambio.

## Review Focus

1. Dos confirmaciones concurrentes conservan un único instante y una única sesión pendiente (tareas 1 y 5).
2. Anular una facturación antigua no convierte sus seguimientos en nuevas pendientes (tareas 2 y 5).
3. Formularios abiertos con datos obsoletos no facturan dos veces y muestran un error recuperable (tareas 2 y 4).
4. IDs de pacientes o turnos de otro establecimiento no revelan ni modifican sus datos (tareas 1, 2 y 5).
5. Turnos sin diagnóstico y sesiones históricas no provocan errores de renderizado ni confusión entre IDs (tareas 2 y 4).

## Contratos compartidos

La API conserva los nombres de fecha y hora que ya usa agenda. Instantes de auditoría en ISO UTC; fecha/hora del turno conserva su semántica local actual. No convertir `date` mediante parsing UTC en el navegador.

```typescript
type BillingSessionOrigin = 'APPOINTMENT' | 'LEGACY_FOLLOW_UP';
interface BilledSessionRef {
  origin: BillingSessionOrigin;
  appointmentId: number | null;
  followUpId: number | null;
  date: string;
  startTime: string | null;
  confirmedAt: string | null;
  diagnosis: { id: number; description: string } | null;
  segments: string[]; // Solo para preservar el detalle histórico.
}
interface CreateBillingPayload {
  billedOn: string;
  amount: number;
  appointmentIds: number[];
}
```

`BillingResponse` conserva sus campos actuales y suma `origin`. Una facturación no mezcla orígenes. El enum Java `BillingSessionOrigin` usa los mismos valores. `BillingSummaryResponse` conserva sus campos: sus tres contadores solo cuentan turnos, `pending` solo contiene APPOINTMENT y `billings` incluye todo el historial del paciente. `AppointmentResponse` suma `Instant confirmedAt` nullable y `boolean billed`.

Interfaces de servicios:

```java
AppointmentResponse confirm(AuthenticatedUser actor, Long id);
BillingSummaryResponse summary(Long patientId);
BillingSummaryResponse register(Long patientId, CreateBillingRequest request);
BillingSummaryResponse voidBilling(Long billingId);
```

Rutas: POST `/api/appointments/{id}/confirm`, GET `/api/patients/{patientId}/billing`, POST `/api/patients/{patientId}/billings`, POST `/api/billings/{id}/void`.

## Preparación del controlador

- [ ] Crear rama/worktree `feat/patient-appointment-billing` según using-git-worktrees; la spec aprobada ya establece trabajo aislado. Llevar únicamente spec/plan a ese checkout; preservar archivos ajenos.
- [ ] Verificar `git status`, JDK 21, Docker y dependencias del frontend. Registrar baseline de compilación y tests relevantes antes de editar código.
- [ ] Guardar una referencia del diseño aprobado en `docs/mockups/h15-confirmed-appointment-billing/README.md`, enlazando la spec y mockups h7/h12. No diseñar pantallas nuevas fuera de lo aprobado.
- [ ] Ejecutar tareas 1 y 2 secuencialmente por compartir entidades; tras fijar sus contratos, las tareas 3 y 4 pueden delegarse por separado. Cada entrega pasa revisión de spec y código antes de integración.

### Task 1: Persistencia y confirmación de turnos

**Archivos:** crear `backend/src/main/resources/db/migration/V15__patient_appointment_billing.sql`; actualizar `supabase/init/01_schema.sql`; modificar entidad/repository/service/controller/mapper/DTO de `backend/src/main/java/com/tomanagement/appointment/`, `common/exception/ErrorCode.java`, y tests existentes de appointment. Crear `backend/src/test/java/com/tomanagement/billing/PatientBillingMigrationTest.java`. Actualizar `TestTable`/`TestTables` para limpiar nuevos vínculos antes de sus padres.

**Produce:** columna y DTO `confirmedAt`, endpoint confirm, consultas de turnos confirmados por paciente y carga bloqueada por ID; esquema preparado para tarea 2.

- [ ] Escribir primero pruebas: POST confirm responde 200 y guarda el instante del Clock; repetir devuelve el mismo instante; sin diagnóstico funciona; terapeuta ajeno no puede confirmar; otro tenant recibe el tratamiento de recurso inexistente; cancelar/reprogramar confirmado produce 422 `APPOINTMENT_ALREADY_CONFIRMED` y no modifica el turno. Ejecutar las pruebas para observar el fallo.
- [ ] Añadir migración y SQL consolidado. Base de la migración:

```sql
ALTER TABLE appointment ADD COLUMN confirmed_at TIMESTAMPTZ;
ALTER TABLE billing ADD COLUMN patient_id BIGINT REFERENCES patient(id);
UPDATE billing b SET patient_id = d.patient_id FROM diagnosis d WHERE d.id = b.diagnosis_id;
ALTER TABLE billing ALTER COLUMN patient_id SET NOT NULL;
ALTER TABLE billing ALTER COLUMN diagnosis_id DROP NOT NULL;
CREATE INDEX ix_billing_patient ON billing(patient_id, billed_on DESC);
CREATE INDEX ix_appointment_confirmed_patient ON appointment(patient_id, start_at, id)
  WHERE confirmed_at IS NOT NULL;
CREATE TABLE billing_appointment (
  billing_id BIGINT NOT NULL REFERENCES billing(id),
  appointment_id BIGINT NOT NULL REFERENCES appointment(id),
  establishment_id BIGINT NOT NULL REFERENCES establishment(id),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (billing_id, appointment_id)
);
CREATE UNIQUE INDEX ux_billing_appointment_active ON billing_appointment(appointment_id) WHERE active;
```

- [ ] En el consolidado representar el estado final con las mismas restricciones e índices, usando las convenciones idempotentes del archivo. Mantener `billing_follow_up` y no cambiar seeds.
- [ ] Implementar `Appointment.confirm(Instant when)` con `if (confirmedAt == null) confirmedAt = Objects.requireNonNull(when);`. Repository con `@Lock(PESSIMISTIC_WRITE)` y JPQL por ID, sin fetch join nullable para no bloquear el lado opcional del diagnóstico. Confirmar/cancelar/reprogramar cargan bajo el mismo bloqueo y aplican primero la política existente de permisos. Usar `Instant.now(clock)`.
- [ ] Error `APPOINTMENT_ALREADY_CONFIRMED`: 422, «El turno ya está confirmado y no se puede cancelar ni reprogramar.». Confirmar dos veces no es error. Mapper expone confirmación; el lookup real de facturado se integra en tarea 2.
- [ ] Probar migración con una base efímera llevada a V14: facturación vigente y anulada, varias sesiones, turno sin diagnóstico y seguimiento nunca facturado. Migrar a V15 y comprobar conteos y relaciones originales intactos, patient_id correcto y ningún turno confirmado automáticamente.
- [ ] Ejecutar tests de appointment y migración; revisar y commitear solo archivos de esta tarea.

### Task 2: Facturación por paciente e historial compatible

**Archivos:** `backend/src/main/java/com/tomanagement/billing/` (entity, repository, DTO, service, controller); crear `BillingAppointment.java`, `BillingAppointmentId.java`, `BillingAppointmentRepository.java`, `BillingSessionOrigin.java`; modificar diagnóstico para retirar `PendingBillingCounter` y `pendingBillingCount`; mantener el puerto `BilledFollowUpLookup` solo para marcas históricas. Integrar `billed` de agenda mediante consulta en lote por IDs sin dependencia circular de servicios. Tests: BillingTest, BillingControllerTest, BillingIntegrityTranslatorTest, DiagnosisControllerTest y CrossEstablishmentIsolationTest.

**Consume:** esquema y confirmación de tarea 1. **Produce:** contratos compartidos, lookup de turnos facturados y resumen independiente de diagnósticos.

- [ ] Adaptar fixture de BillingControllerTest: un paciente con tres turnos confirmados (dos diagnósticos distintos y uno sin diagnóstico), uno no confirmado y dos seguimientos clínicos. Resumen esperado: total 3, billed 0, pending 3. Tests con el helper `perform` existente:

```java
perform(get("/api/patients/{id}/billing", patient.getId()))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.totalSessions").value(3))
    .andExpect(jsonPath("$.pendingSessions").value(3))
    .andExpect(jsonPath("$.pending.length()").value(3));
```

- [ ] Observar fallos. Añadir casos para payload vacío, monto inválido, fecha futura según Clock, ID repetido/no confirmado/de otro paciente/de otro tenant/ya facturado. Conservar límites actuales de precisión del monto.
- [ ] Reorientar `Billing` a `Patient`, diagnóstico nullable histórico y dos colecciones de vínculos; nuevo constructor para turnos y lectura de registros antiguos. `BillingAppointment` usa `@TenantId`, `@IdClass` y desactivación equivalentes a BillingFollowUp. No borrar enlaces al anular.
- [ ] Registrar con carga bloqueada de turnos por ID ascendente antes de validar y persistir. Rechazar cualquier selección inválida con `BILLING_SESSION_INVALID`, texto «Hay sesiones que no son de este paciente, no están confirmadas o ya están facturadas.». El índice parcial resuelve doble escritura y `BillingIntegrityTranslator` reconoce ambas restricciones histórica/nueva. Usar bloqueo de facturación al anular y bloquear sus turnos en orden ascendente; anulación repetida idempotente.
- [ ] Construir resumen desde turnos confirmados y vínculos activos; ordenar pendientes por fecha/hora/ID. Consultar historial por `patient_id`, con origen derivado de la asociación histórica `diagnosis_id` (LEGACY si no nulo, APPOINTMENT si nulo), conservando sesiones de facturaciones anuladas. Anulación antigua desactiva enlaces históricos, sin añadir pendientes.
- [ ] Retirar endpoints de diagnóstico y sus contadores; mantener marca histórica `billed` de seguimientos. No crear una sesión al guardar seguimiento. Consultas Hibernate filtradas por tenant; no introducir consultas nativas sin filtro de establecimiento.
- [ ] Verificar registro parcial (3 → 2 pendientes + 1 facturada), anulación (vuelve a 3 pendientes), re-facturación, historial mixto y carreras de dos registros con el mismo turno. Prueba de carrera con dos transacciones y barrera: solo una facturación vigente; no aceptar dos respuestas exitosas con dos vínculos activos.
- [ ] Ejecutar pruebas backend afectadas, revisar y commitear.

### Task 3: Confirmación y estados en agenda

**Archivos:** `frontend/src/app/features/agenda/agenda.models.ts`, `agenda.service.ts`, `appointment-card/`, `appointments-list/`, `agenda-page/` y sus specs.

**Consume:** `Appointment.confirmedAt: string | null`, `billed: boolean`, POST confirm. **Produce:** acción de confirmación de un solo clic y agenda actualizada.

- [ ] Agregar test HTTP antes del método:

```typescript
it('confirms an appointment', () => {
  service.confirm(3).subscribe();
  const request = http.expectOne('/api/appointments/3/confirm');
  expect(request.request.method).toBe('POST');
  expect(request.request.body).toEqual({});
});
```

- [ ] Implementar `confirm(id: number): Observable<Appointment>` con `http.post<Appointment>(`/api/appointments/${id}/confirm`, {})`. Actualizar fixtures para incluir ambos campos.
- [ ] Agregar output `confirmSession` en card/list; agenda-page gestiona petición, IDs en curso y error por turno, usando limpieza en finalize para no dejar botones bloqueados. No marcar éxito optimista antes de respuesta; reemplazar turno y actualizar la fuente de calendario/listado.
- [ ] Dibujar ✓ antes de ×, `aria-label` y `title` «Confirmar sesión», área táctil accesible, disabled/busy durante petición. Confirmado muestra estado pendiente/facturado y solo Ver ficha; ocultar reprogramación/cancelación también en cualquier otra entrada de agenda que administre el mismo turno.
- [ ] Tests de doble clic (una petición), fallo (conserva acciones y mensaje), éxito (etiqueta y acciones correctas), falta de diagnóstico (sin línea extra), aislamiento de carga entre tarjetas. Validar 360 px sin desborde y teclado.
- [ ] Ejecutar tests de agenda, revisar y commitear.

### Task 4: Facturación en ficha del paciente

**Archivos:** `frontend/src/app/features/billing/`, `features/patients/patient-page/`, `features/diagnoses/diagnosis-page/`, `diagnosis-card/`, modelos de diagnósticos, `frontend/src/app/app.routes.ts`; actualizar specs correspondientes.

**Consume:** contratos compartidos y endpoints de paciente de tarea 2. **Produce:** registro de facturación por paciente y navegación compatible.

- [ ] Adaptar tests HTTP a `/api/patients/7/billing` y `/api/patients/7/billings`, payload `appointmentIds`. Tests de render para turno sin diagnóstico y origen histórico antes de cambiar componentes.
- [ ] Cambiar inputs de componentes/formulario y parámetros de servicio de diagnosisId a patientId. El formulario solo acepta referencias APPOINTMENT con appointmentId no nulo; selección por ese ID, nunca por followUpId. Aplicar el contrato discriminado definido arriba al historial y las claves de render.
- [ ] En la ficha del paciente, contenedor de contenido para Facturación antes de Diagnósticos; mantener tarjeta de datos personales y grilla móvil/escritorio. Resumen «turnos confirmados», «facturados», «pendientes». Empty: «Todavía no hay turnos confirmados para facturar.».
- [ ] Lista de pendientes muestra fecha/hora y «✓ Confirmado», diagnóstico solo si no nulo. Mantener selección inicial, fecha, monto, confirmación de anulación, avisos de selección obsoleta y recarga de pendientes existentes.
- [ ] Historial: etiqueta «Registro anterior» para LEGACY_FOLLOW_UP, fechas y segmentos de seguimientos preservados; registros nuevos muestran turnos. No mezclar conteo del historial antiguo con contadores nuevos.
- [ ] Retirar sección y contador del diagnóstico. Mantener marcas históricas en seguimiento. Página móvil `/pacientes/:id/facturacion/nueva` toma patientId; redirección de ruta antigua `:id/diagnosticos/:diagnosisId/facturacion/nueva` a la nueva usando RedirectFunction y UrlTree que preserve `id` (no usar sustituciones textuales de IDs).
- [ ] Tests de formulario mixto entre diagnósticos/sin diagnóstico, selección parcial, anulación, errores y recarga; comprobación de ubicación y redirect antiguo. Ejecutar tests frontend y build producción, revisar y commitear.

### Task 5: Integración, migración y revisión final

**Archivos:** tests de integración/concurrencia/migración y documentación de esta función; no modificar credenciales ni infraestructura remota.

- [ ] Integrar entregas en la misma rama respetando contratos; ejecutar compile y tests afectados en Java 21. Si Maven local usa Java 17, usar la imagen Java 21 existente con volumen de caché y socket Docker para Testcontainers, sin conectar a Supabase real:

```powershell
docker run --rm --mount "type=bind,source=$((Get-Location).Path)/backend,target=/app" --mount type=volume,source=to-management-m2,target=/root/.m2 --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test '-Dtest=AppointmentServiceImplTest,AppointmentControllerTest,AppointmentRepositoryTest,BillingTest,BillingControllerTest,BillingIntegrityTranslatorTest,PatientBillingMigrationTest,DiagnosisControllerTest,FollowUpControllerTest,CrossEstablishmentIsolationTest'
```

- [ ] Desde frontend ejecutar `npm.cmd test -- --watch=false` y `npm.cmd run build`. Corregir cualquier regresión causada por el cambio. Verificar `git diff --check`.
- [ ] En PostgreSQL efímero, ejecutar el SQL consolidado dos veces y comprobar 0 errores y estructura equivalente a Flyway. No ejecutar scripts de prueba ni limpieza sobre la base configurada en `.env`.
- [ ] Verificación funcional con datos sintéticos: crear turno sin diagnóstico → confirmar → abrir ficha → seleccionar y facturar → volver a agenda (facturado) → anular → volver a pendiente. Crear seguimiento no altera ningún contador. Comprobar historial antiguo y links de ruta anterior.
- [ ] Revisión independiente de todo el diff, especialmente migración, tenant, carreras y preservación del historial; corregir y repetir únicamente checks afectados.
- [ ] Entregar resumen de archivos, pruebas con resultados reales y limitaciones. Preparar integración de rama según finishing-a-development-branch; no publicar cambios remotos por inferencia.

## Auto-revisión del plan

- Contratos de confirmación: tareas 1/3; facturación: 2/4; migración/historial: 1/2/5.
- Los cinco riesgos de Review Focus tienen pruebas asignadas. No se crean relaciones arbitrarias entre turnos y seguimientos.
- Los campos de sesiones y los payloads coinciden entre Java y TypeScript; los IDs de historial no son seleccionables para nuevas facturaciones.
- Diseño aprobado, plan pendiente de revisión antes de ejecutar. Método de ejecución: subagentes según CLAUDE.md.
