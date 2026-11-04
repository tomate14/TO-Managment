# É5 Diagnósticos (H5.1–H5.2) — plan

**Historias:** spec §6 É5 (H5.1 crear diagnóstico con tratamiento quirúrgico o conservador, H5.2 verlos del más reciente al más antiguo). Modelo: spec §3.4. Patrones: spec §4 (State para `Treatment`, Factory Method `TreatmentFactory`, herencia JPA `JOINED`).
**Mockups:**
- `h5.1-crear-diagnostico/mobile-quirurgico.png` y `mobile-conservador.png` (`M-DiagnosisNew.dc.html`).
- `h5.2-h6.4-diagnostico-y-seguimientos/mobile.png` (`M-Diagnosis.dc.html`): el historial de seguimientos es de É6.
- Las tarjetas de diagnóstico de la ficha están en `h4.3-ficha-paciente/*` (mobile y desktop).

## Decisiones
- **Zona horaria:** la fecha del diagnóstico la pone el sistema (`LocalDate.now(clock)`). Variable nueva `APP_TIME_ZONE` (`America/Argentina/Buenos_Aires`) en `.env`, `.env.example`, compose y `application.yml`.
  - El `Clock` pasa a esa zona.
  - Los `Instant` no cambian; antes, un diagnóstico cargado de noche quedaba con la fecha del día siguiente (UTC).
- **Sin edición de diagnósticos:** no está en los mockups ni en el spec, que solo pide que el tipo de tratamiento no cambie. No hay endpoint de edición.
- **Fechas:**
  - Retiro de puntos ≥ fecha de cirugía.
  - Se aceptan fechas futuras (el retiro de puntos puede estar programado).
- **Tipo de cirugía:** tiene que estar activo al crear (`SURGERY_TYPE_INACTIVE`).
- **Rutas:** el detalle vive en `/pacientes/:id/diagnosticos/:diagnosisId`, para que la navegación siga marcando "Pacientes".
- **Seguimientos (É6):** el detalle muestra la sección "Seguimientos" con "Todavía no hay seguimientos.", sin botón.
  - Las tarjetas de la ficha muestran "Creado el …" sin el conteo de seguimientos, que se suma en É6.

## Base de datos (`V7__create_diagnoses.sql` + `01_schema.sql`)
- `treatment`: `id`, `treatment_type` (`SURGICAL`/`CONSERVATIVE`).
- `surgical_treatment`: `treatment_id` PK/FK, `surgery_date`, `stitches_removal_date`, `surgery_type_id` FK, con CHECK de retiro ≥ cirugía.
- `conservative_treatment`: `treatment_id` PK/FK, `immobilization_date`.
- `diagnosis`: `id`, `patient_id` FK, `description` TEXT, `created_date` DATE, `treatment_id` FK única.

## Backend (paquete `diagnosis`)
- `TreatmentType` (enum).
- **State:** `Treatment` (abstracta, `@Inheritance(JOINED)`) → `SurgicalTreatment` y `ConservativeTreatment`.
  - Cada una valida sus datos al construirse.
  - Responde `requiresScarType()` (true / false), que usa É6.
- **Factory Method:** `TreatmentFactory` crea el tratamiento desde el `TreatmentRequest` según `TreatmentType` (busca el tipo de cirugía activo).
- `Diagnosis` (paciente, descripción, fecha de creación, tratamiento inmutable).
- `DiagnosisService` + `Impl`.
- API (autenticada):
  - `GET /api/patients/{id}/diagnoses` (más recientes primero).
  - `POST /api/patients/{id}/diagnoses` (201).
  - `GET /api/diagnoses/{id}` (con un resumen del paciente para el encabezado).
- `ErrorCode`: `DIAGNOSIS_NOT_FOUND` (404) y, con 422, `SURGERY_DATA_REQUIRED`, `STITCHES_BEFORE_SURGERY`, `IMMOBILIZATION_DATE_REQUIRED` y `SURGERY_TYPE_INACTIVE`.

## Frontend
- **Ficha del paciente:**
  - Lista de diagnósticos con título y badge "Quirúrgico"/"Conservador".
  - Datos del tratamiento: cirugía con fecha y tipo, o fecha de inmovilización.
  - "Creado el …" con link al detalle.
  - Botón "+ Nuevo" ("Nuevo diagnóstico" en escritorio).
- `/pacientes/:id/diagnosticos/nuevo` (`DiagnosisFormPage`):
  - Descripción y la elección de tratamiento en dos tarjetas, con el aviso de que no se puede cambiar.
  - "Datos de la cirugía": fechas y tipo de los activos, con validación de retiro ≥ cirugía. O, si es conservador, la fecha de inmovilización.
  - "Cancelar" / "Crear diagnóstico".
- `/pacientes/:id/diagnosticos/:diagnosisId` (`DiagnosisPage`): encabezado con "Nombre · DNI", tarjeta con descripción, badge y datos, y la sección "Seguimientos".

## Verificación
Se escriben tests (tratamientos y fábrica, controller con Testcontainers, componentes), pero solo se verifica que compile. `supabase/init` se corre contra un Postgres vacío.
