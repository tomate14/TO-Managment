# É3 Plantillas de evaluación (H3.1–H3.4) — plan

**Historias:** spec §6 É3 (H3.1 crear plantilla, H3.2 preguntas, H3.3 encadenar, H3.4 desactivar). Modelo: spec §3.2.
**Mockups:**
- `h3.1-plantillas/mobile-listado.png` (fuente `M-Templates.dc.html`).
- `h3.2-h3.4-editar-plantilla/mobile.png` (`M-TemplateEdit.dc.html`).
- `h3.2-h3.3-nueva-pregunta/*.png` (`M-QuestionNew.dc.html`): numérica encadenada, de opción y error de operador incompatible.

## Decisiones (lo que los mockups no resuelven)
- **Permisos:** cualquier usuario logueado (terapeuta o admin) gestiona plantillas, según "Como terapeuta…" del spec.
- **Orden:** `displayOrder` se asigna solo (al final) al crear. No hay reordenamiento porque no está en los mockups.
  - El editor muestra el árbol: cada hija va debajo de su padre, con sangría, y se numera según ese recorrido.
- **Padre válido:** una pregunta activa de la misma plantilla, creada antes (`displayOrder` menor). Así no hay ciclos ni al crear ni al editar.
- **Valor esperado** (texto):
  - Sí/No: `true`/`false`.
  - Numérica: número.
  - Opción: el texto de una de las opciones del padre.
  - Texto: libre.
- **Preguntas sin respuestas:** se editan y se pueden eliminar desde el formulario ("Eliminar pregunta"), salvo que otras dependan de ellas.
  - Si tiene hijas, no se puede cambiar su tipo ni sus opciones (las condiciones de las hijas quedarían inválidas).
- **Preguntas con respuestas:** quedan bloqueadas y solo se activan o desactivan con el switch del mockup.
  - Las respuestas recién existen con É6. Hasta entonces, la interfaz `QuestionAnswerLookup` responde "sin respuestas" y É6 la reemplaza por la consulta real.
- **Plantilla nueva:** se crea con nombre y segmento ("Guardar") y se pasa a su edición, donde se agregan las preguntas.
  - El switch "Plantilla activa" se guarda con "Guardar".
  - El de cada pregunta bloqueada se aplica al instante.
- **Filtro del listado:** "Todos" más los segmentos activos. El listado muestra también las plantillas inactivas.

## Base de datos (`V5__create_templates.sql` + `01_schema.sql`)
- `evaluation_template` (`id`, `name` 120, `body_segment_id` FK, `active`), con único (`body_segment_id`, `lower(name)`).
- `question`:
  - `id`, `template_id` FK, `text` 255, `answer_type`, `display_order`, `active`.
  - `min_value` / `max_value` NUMERIC(12,3).
  - `parent_question_id` FK propio, `condition_operator` y `condition_expected_value` 120.
  - CHECK: hay condición si y solo si hay padre.
- `question_option` (`id`, `question_id` FK cascade, `label` 120, `display_order`), con único (`question_id`, `lower(label)`).

## Backend (paquete `template`)
- Enums `AnswerType` y `ConditionOperator` (con `supports(AnswerType)`: mayor/menor solo para numéricas).
- **Strategy + Factory:** `AnswerTypeRules` por tipo (forma de la pregunta: rango, opciones; valor esperado válido para un padre de ese tipo), con `AnswerTypeRulesFactory`.
- **Specification:** `ActivationCondition.isSatisfiedBy(AnswerValue)` con una clase por operador, creadas por `ActivationConditionFactory`. Se testea ya y la usa É6.
- **Composite:** `QuestionNode` arma el árbol de la plantilla y lo recorre en orden (número y profundidad para el editor).
- Servicios: `TemplateService` y `QuestionService` (interfaz + `Impl`).
- API (autenticada):
  - `GET /api/templates?segmentId=`
  - `POST /api/templates`
  - `GET`, `PUT /api/templates/{id}`
  - `PATCH /api/templates/{id}/active`
  - `POST /api/templates/{id}/questions`
  - `PUT`, `DELETE /api/templates/{id}/questions/{qid}`
  - `PATCH …/{qid}/active`
- `ErrorCode` nuevos:
  - 404: `TEMPLATE_NOT_FOUND`, `QUESTION_NOT_FOUND`.
  - 409: `TEMPLATE_DUPLICATE`.
  - 422: `SEGMENT_INACTIVE`, `QUESTION_RANGE_INVALID`, `QUESTION_OPTIONS_REQUIRED`, `QUESTION_OPTIONS_DUPLICATE`, `CONDITION_PARENT_INVALID`, `CONDITION_OPERATOR_INCOMPATIBLE`, `CONDITION_VALUE_INVALID`, `QUESTION_LOCKED`, `QUESTION_HAS_DEPENDENTS`.

## Frontend
- `/plantillas` (`TemplatesPage`):
  - Título y subtítulo.
  - Chips de segmento.
  - Tarjetas con nombre, segmento, estado y "n preguntas · m encadenadas".
  - Botón flotante "Nueva plantilla".
- `/plantillas/nueva` y `/plantillas/:id` (`TemplateEditPage`):
  - Nombre, segmento (select) y "Plantilla activa".
  - Las preguntas en árbol, con chips de tipo, condición e "Inactiva", y lápiz o candado con switch.
  - "+ Agregar pregunta".
- `/plantillas/:id/preguntas/nueva` y `/plantillas/:id/preguntas/:qid` (`QuestionFormPage`):
  - Texto y chips de tipo.
  - Mín./máx. u opciones (mínimo 2, "+ Agregar opción").
  - "Mostrar solo si…" con pregunta padre, condición y valor esperado (según el tipo del padre), más la vista previa.
  - El error de operador incompatible del mockup aparece también del lado del cliente.

## Verificación
Se escriben tests (reglas, condiciones, árbol, servicios con Mockito, controllers con MockMvc + Testcontainers, componentes con Vitest), pero solo se verifica que compile. `supabase/init` se corre contra un Postgres vacío.
