# É11 — ROM por segmento e inmovilización opcional: diseño

Fecha: 2026-11-07 · Rama: `feat/segment-rom` (desde `main`) · Depende de: É3 plantillas (tipos de respuesta), É5 diagnósticos, É6 seguimientos, É8 multi-tenencia

## 1. Objetivo

1. **ROM por segmento.** Hoy cada evaluación de segmento tiene un único ROM numérico (0 a 360), igual para todos los segmentos. Con este cambio, cada establecimiento define en Catálogos qué métricas de ROM se miden en cada segmento: por ejemplo, "Flexión" y "Extensión" en Codo, y "Abducción" y "Rotación externa" en Hombro. Cada métrica tiene un nombre y un tipo (numérico, sí/no, opción o texto). En los seguimientos, el bloque ROM muestra las métricas del segmento evaluado.
2. **Inmovilización opcional.** En el diagnóstico conservador, antes de la fecha de inmovilización se pregunta "¿Lleva inmovilización?". La fecha se pide solo si la respuesta es Sí.

Éxito:
- La terapeuta define las métricas de un segmento en menos de un minuto, igual que las preguntas de una plantilla.
- Al cargar un seguimiento de Codo ve exactamente las métricas activas de Codo, y las obligatorias no la dejan guardar vacías.
- Los seguimientos y diagnósticos ya cargados se siguen viendo igual que antes.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Dónde se definen | Catálogos > Segmentos: cada segmento tiene su lista de métricas ROM. Siempre se muestran al evaluar ese segmento, sin elegir plantilla. |
| ROM fijo actual | Se reemplaza. Los seguimientos nuevos ya no lo piden. Los viejos lo siguen mostrando como historia. Edema, Dolor y Fuerza quedan fijos. |
| Obligatoriedad | Cada métrica tiene la marca "Obligatoria". |
| Tipos | Los mismos que las preguntas de plantillas: `NUMERIC`, `YES_NO`, `OPTION` (con lista de opciones) y `TEXT`. |
| Cambio de tipo | No se permite después de crear la métrica. Para cambiarlo se desactiva y se crea otra. |
| Valores iniciales | Ninguno: los segmentos arrancan sin métricas. |
| Quién las administra | El ADMIN del establecimiento (incluye a la terapeuta independiente). Son datos del establecimiento, aislados con `@TenantId`, como el resto de los catálogos. |
| Inmovilización | Campo nuevo Sí/No. Con Sí, la fecha es obligatoria; con No, no hay fecha. Los diagnósticos existentes quedan en Sí con su fecha. |

## 3. Historias

- **H11.1 Métricas de un segmento.**
  - En Catálogos, pestaña Segmentos, cada segmento tiene la acción "Métricas ROM", que abre `/catalogos/segmentos/:id`.
  - La pantalla muestra el nombre del segmento y la lista ordenada de métricas, cada una con su nombre, tipo, la marca "Obligatoria" si la tiene y el estado activa/inactiva.
  - Acciones de la lista: "Nueva métrica", subir y bajar el orden, y activar o desactivar.
  - Si no hay métricas: "Todavía no hay métricas ROM para este segmento. Creá la primera con “Nueva métrica”."
- **H11.2 Alta y edición de una métrica.**
  - Formulario con nombre (obligatorio, máximo 120 caracteres, único dentro del segmento sin distinguir mayúsculas), tipo y "Obligatoria".
  - Con el tipo Opción aparece la lista de opciones: mínimo 2, cada una con texto obligatorio de máximo 120 caracteres, sin repetir.
  - En la edición, el tipo se muestra como texto y no se puede cambiar. Se pueden cambiar el nombre, la marca y, para Opción, la lista de opciones.
  - En móvil es una página, como el formulario de preguntas: `/catalogos/segmentos/:id/metricas/nueva` y `/catalogos/segmentos/:id/metricas/:metricId`.
- **H11.3 ROM en el seguimiento.**
  - En cada evaluación de segmento, el campo ROM se reemplaza por el bloque "ROM" con las métricas activas del segmento elegido, en su orden.
  - Cada métrica se carga según su tipo: número, Sí/No, chips de opciones (como en las plantillas) o texto.
  - Si el segmento no tiene métricas activas: "Este segmento no tiene métricas ROM definidas." Los administradores ven además el link "Definir en Catálogos".
  - Al cambiar el segmento elegido, el bloque se recarga con las métricas del nuevo segmento y se descartan los valores cargados.
- **H11.4 ROM en la tarjeta del seguimiento.**
  - Seguimientos nuevos: el bloque ROM lista "Nombre: valor" de cada métrica respondida. Sí/No se muestra como "Sí" o "No".
  - Seguimientos viejos (con el ROM fijo y sin valores de métricas): "ROM: 90", como hoy.
  - Una métrica desactivada después de cargar el seguimiento sigue apareciendo con su valor.
- **H11.5 Inmovilización.**
  - En el diagnóstico conservador, la pregunta "¿Lleva inmovilización?" con Sí/No. Por defecto no hay nada elegido y es obligatorio responder.
  - Con Sí aparece "Fecha de inmovilización", obligatoria. Con No, la fecha se oculta y no se envía.
  - En la tarjeta y en el detalle del diagnóstico: "Inmovilización: dd/mm/aaaa" o "Sin inmovilización".

## 4. Modelo de datos

Migración `V13__segment_rom.sql`. Es V13 porque V11 y V12 ya existen en ramas que todavía no llegaron a `main` (É9 y É10). Se refleja en el mismo cambio en `supabase/init/01_schema.sql`, de forma idempotente.

- `segment_metric`:
  - `id bigserial`;
  - `establishment_id bigint not null` (tenant);
  - `body_segment_id bigint not null references body_segment`;
  - `name varchar(120) not null`;
  - `answer_type varchar(20) not null`, con un CHECK que limita a los 4 tipos;
  - `required boolean not null default false`;
  - `active boolean not null default true`;
  - `display_order integer not null`;
  - un índice único sobre `(body_segment_id, lower(name))`.
- `segment_metric_option`:
  - `id`;
  - `segment_metric_id references segment_metric`;
  - `label varchar(120) not null`;
  - `display_order`;
  - un índice único sobre `(segment_metric_id, lower(label))`.
- `metric_value`:
  - `id`;
  - `segment_evaluation_id references segment_evaluation`;
  - `segment_metric_id references segment_metric`;
  - `numeric_value numeric(12,3)`, `boolean_value boolean`, `option_value varchar(120)`, `text_value text`;
  - un UNIQUE sobre `(segment_evaluation_id, segment_metric_id)`;
  - un CHECK con `num_nonnulls(...) = 1`, igual que `answer`.
- `segment_evaluation.rom`: pasa a admitir NULL. Los seguimientos nuevos lo guardan en NULL.
- `conservative_treatment`:
  - nuevo `has_immobilization boolean not null default true`;
  - `immobilization_date` pasa a admitir NULL;
  - un CHECK `(has_immobilization AND immobilization_date IS NOT NULL) OR (NOT has_immobilization AND immobilization_date IS NULL)`.
- No hay datos iniciales en `02_seed.sql`, porque los segmentos arrancan sin métricas.

## 5. Backend

- **Paquete nuevo `segmentmetric`**, por feature: `controller`, `service` (interfaz + `Impl`), `repository`, `dto`, `entity`.
  - Entidades `SegmentMetric` y `SegmentMetricOption`. Reutilizan el enum `AnswerType` de plantillas.
  - El tipo es inmutable: no tiene setter, y el DTO de edición no lo incluye.
- **Endpoints** (rol ADMIN o SUPERADMIN dentro de un establecimiento, igual que los catálogos):
  - `GET /api/segments/{segmentId}/metrics`: lista todas las métricas del segmento, activas e inactivas, en orden. La pueden leer también los terapeutas, porque el formulario de seguimiento la necesita.
  - `POST /api/segments/{segmentId}/metrics`: crea una métrica al final del orden.
  - `PUT /api/segments/{segmentId}/metrics/{id}`: cambia el nombre, la marca y las opciones.
  - `PATCH /api/segments/{segmentId}/metrics/{id}/active`: activa o desactiva.
  - `PUT /api/segments/{segmentId}/metrics/order`: recibe `{ ids: [...] }` con el orden completo.
- **Errores (`ErrorCode`):**
  - `SEGMENT_METRIC_NOT_FOUND` (404);
  - `SEGMENT_METRIC_NAME_TAKEN` (409, "Ya hay una métrica con ese nombre en este segmento.");
  - `SEGMENT_METRIC_OPTIONS_INVALID` (422, "Las opciones tienen que ser al menos 2 y no repetirse.");
  - `METRIC_VALUE_REQUIRED` (422, "Completá las métricas obligatorias.");
  - `METRIC_VALUE_INVALID` (422, "Hay métricas con un valor que no es válido para su tipo.");
  - `METRIC_NOT_OF_SEGMENT` (422, "Hay métricas que no corresponden al segmento evaluado.");
  - `IMMOBILIZATION_ANSWER_REQUIRED` (422, "Indicá si lleva inmovilización.").
- **Seguimientos:**
  - `SegmentEvaluationRequest` pierde `rom` y suma `metrics: List<MetricValueRequest>`, con `{ metricId, numericValue, booleanValue, optionValue, textValue }` y `toString` enmascarado, igual que `AnswerRequest`.
  - Nuevo paso de la cadena de validación, `SegmentMetricsStep` (Chain of Responsibility existente). Exige que:
    - cada métrica pertenezca al segmento y esté activa;
    - las obligatorias estén presentes;
    - cada valor sea único y del tipo correcto;
    - en Opción, el valor esté entre las opciones.
  - El valor `RawAnswer` y la validación por tipo se reutilizan de las respuestas de plantillas, si encajan sin forzar. Si no, se extrae un validador común.
  - `ClinicalRange.ROM` y `ClinicalRangeStep` dejan de validar ROM.
  - `ClinicalMetrics.rom` pasa a ser nullable, solo para leer la historia.
  - La respuesta de la evaluación suma `metrics: [{ metricId, name, answerType, value… }]`, con el nombre tomado de la métrica aunque esté inactiva, y conserva `rom` (nullable) para los viejos.
- **Diagnóstico:**
  - `TreatmentRequest` suma `hasImmobilization: Boolean`.
  - `ConservativeTreatment` suma `hasImmobilization`:
    - si es null → `IMMOBILIZATION_ANSWER_REQUIRED`;
    - si es true sin fecha → el `IMMOBILIZATION_DATE_REQUIRED` que ya existe;
    - si es false, la fecha se ignora y se guarda null.
  - `TreatmentResponse` suma `hasImmobilization`.
- Nunca se loguean valores clínicos (las métricas son datos clínicos).

## 6. Frontend

- **Catálogos > Segmentos:**
  - Cada fila suma la acción "Métricas ROM".
  - Pantalla nueva `segment-metrics-page`, con la lista, el orden y la activación.
  - Formulario `segment-metric-form`: en móvil es una página; en escritorio es un panel lateral, como el alta de establecimiento.
  - Rutas lazy bajo `catalogos`.
- **Formulario de seguimiento** (`segment-evaluation-editor`):
  - Se quita el campo ROM.
  - Se suma el componente `segment-metrics-fields`, que recibe las métricas del segmento elegido y arma los controles reactivos según el tipo.
  - Los errores del backend se muestran por métrica.
  - Las métricas se piden al elegir el segmento (`GET /api/segments/{id}/metrics`, filtrando las activas).
- **Tarjeta de seguimiento:** el bloque ROM según H11.4.
- **Diagnóstico** (`diagnosis-form-page`): el radio Sí/No y la fecha condicional (H11.5). La tarjeta y el detalle muestran "Sin inmovilización".
- **Mockups** (requisito para codificar), en `docs/mockups/h11-rom-segmento/`, para móvil y escritorio:
  - métricas de un segmento: lista vacía y lista con métricas;
  - formulario de métrica, con el tipo Opción;
  - bloque ROM en el seguimiento: con métricas y con el segmento sin métricas;
  - tarjeta de seguimiento con métricas;
  - diagnóstico conservador con Sí/No: Sí con la fecha y No sin la fecha.

## 7. Pruebas

Se escriben los tests; la verificación es solo de compilación (instrucción vigente), más una corrida única de las clases de integración clave antes del merge a `test`, como en É9 y É10.

- **Métricas:**
  - crear, editar y desactivar;
  - nombre repetido en el mismo segmento → 409; el mismo nombre en otro segmento se permite;
  - Opción con menos de 2 opciones o con opciones repetidas → 422;
  - el tipo no cambia;
  - el reorden respeta los ids;
  - un TERAPEUTA lee pero no escribe (403);
  - aislamiento entre establecimientos (se extiende `CrossEstablishmentIsolationTest`).
- **Seguimiento:**
  - falta una obligatoria → 422 `METRIC_VALUE_REQUIRED`;
  - valor de otro tipo u opción inexistente → 422 `METRIC_VALUE_INVALID`;
  - una métrica de otro segmento o inactiva → 422 `METRIC_NOT_OF_SEGMENT`;
  - se guarda sin `rom`;
  - la respuesta trae las métricas con su nombre;
  - un seguimiento viejo sigue trayendo `rom`.
- **Diagnóstico:**
  - Sí sin fecha → 422;
  - No con fecha → se guarda sin fecha;
  - sin respuesta → 422 `IMMOBILIZATION_ANSWER_REQUIRED`;
  - un diagnóstico existente se lee con `hasImmobilization = true`.
- **Frontend:**
  - pantalla y formulario de métricas: validaciones, opciones y tipo bloqueado al editar;
  - bloque ROM por tipo, obligatorias, recarga al cambiar de segmento y estado sin métricas;
  - tarjeta con métricas y tarjeta vieja;
  - diagnóstico: la fecha aparece y se oculta según Sí/No.

## 8. Flujo de ramas y despliegue

Rama desde `main` → merge a `test` → prueba manual → merge de la feat a `main` (nunca `test`).

La migración es V13. Si É9 y É10 llegan a `main` antes, el orden V11 → V12 → V13 queda correcto. Si esta rama llegara primero, Flyway aplicaría V13 antes que V11 y V12, y rechazaría las versiones más bajas al desplegarlas después. Por eso esta rama se mergea a `main` **después** de É9 y É10, o se renumera en ese momento.

V13 no borra datos. No hay variables de entorno nuevas.

## 9. Fuera de alcance

Gráficos de evolución de las métricas, copiar métricas entre segmentos, rangos mínimo y máximo o unidades para las métricas numéricas, convertir el ROM viejo en métricas, y cambiar el tipo de una métrica existente.
