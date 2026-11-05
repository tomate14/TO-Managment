# É6 — Seguimientos (H6.1–H6.4)

Mockups: `docs/mockups/h6.1-h6.3-nuevo-seguimiento/` (mobile, sin férula, desktop) y
`docs/mockups/h5.2-h6.4-diagnostico-y-seguimientos/`. Spec §3.5, §4 y §6 (É6).
Verificación: backend y frontend compilan (los tests se escriben pero no se corren, por pedido del usuario).

## Decisiones
- **Rangos clínicos** (`enum ClinicalRange`, una sola fuente de verdad en el backend): edema 0–50, dolor 0–10 (entero),
  ROM 0–360 (grados), fuerza 0–100. Fuera de rango → 422 `FOLLOW_UP_OUT_OF_RANGE` con un `fieldErrors` por campo
  (`evaluations[i].rom`), que el formulario muestra debajo del campo.
- **Tipo de cicatriz:** obligatorio y activo si `Treatment.requiresScarType()`; si es conservador y viene, se rechaza.
- **Segmentos:** activos y sin repetir dentro del mismo seguimiento (el formulario no ofrece los ya elegidos).
- **Plantilla:** opcional, activa y del segmento. Sin plantilla no se aceptan respuestas.
- **Preguntas visibles:** una pregunta se muestra si está activa y no tiene padre, o si su padre se muestra, tiene
  respuesta y la cumple. La hija de una pregunta inactiva nunca se muestra. Misma regla en backend
  (`VisibleQuestionResolver`, recorre el `QuestionNode`) y frontend (`question-visibility.ts`).
- **Respuestas de hijas ocultas:** se conservan mientras el formulario está abierto y se descartan al guardar.
  El backend rechaza respuestas a preguntas no visibles (`ANSWER_QUESTION_INVALID`) o con valor inválido para su
  tipo/rango/opciones (`ANSWER_VALUE_INVALID`). Ninguna pregunta es obligatoria.
- **Historial:** seguimientos por fecha desc (id desc). Las respuestas se listan en orden de árbol con su
  profundidad y el texto de la pregunta aunque esté inactiva.
- **Conteo:** `DiagnosisResponse.followUpCount`, vía la interfaz `FollowUpCounter` (paquete diagnosis) implementada
  en followup, para no crear un ciclo entre paquetes.
- `NoRecordedAnswersLookup` se reemplaza por `RecordedAnswersLookup` (tabla `answer`): las preguntas con respuestas
  quedan bloqueadas (H3.4).

## Backend (paquete `followup`)
- V8: `follow_up`, `segment_evaluation`, `answer` (una columna por tipo de valor). Reflejado en `supabase/init/01_schema.sql`.
- Entidades `FollowUp` (Builder), `SegmentEvaluation`, `Answer`.
- `FollowUpValidationChain` (Chain of Responsibility): `TreatmentScarTypeStep` → `ClinicalRangeStep` →
  `SegmentStep` → `TemplateAnswersStep`.
- `AnswerTypeRules.read(RawAnswer, Question)` (Strategy existente) convierte y valida el valor de una respuesta.
- `FollowUpRegistrationService` (Facade: valida, arma y guarda en una transacción) y `FollowUpQueryService`.
- `GET/POST /api/diagnoses/{id}/follow-ups`.
- `ApplicationException.getFieldErrors()` + `FieldValidationException` para errores por campo.

## Frontend
- `features/follow-ups/`: modelos, servicio, `question-visibility.ts`, `FollowUpFormPage`
  (`/pacientes/:id/diagnosticos/:diagnosisId/seguimientos/nuevo`), `SegmentEvaluationEditor`, `TemplateQuestions`,
  `FollowUpCard` (historial con respuestas plegables).
- Diagnóstico: tarjeta oscura de resumen (como el mockup), botón "Nuevo seguimiento" e historial.
- Tarjeta de diagnóstico: "Creado el … · N seguimientos".
