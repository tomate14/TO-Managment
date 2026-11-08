# É11 — Plan de implementación: ROM por segmento e inmovilización opcional

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para ejecutar este plan tarea por tarea. Los pasos usan checkboxes (`- [ ]`).

**Objetivo:** cada establecimiento define en Catálogos las métricas ROM de cada segmento (con nombre, tipo y si es obligatoria). Esas métricas reemplazan al ROM fijo en los seguimientos. Además, el diagnóstico conservador pregunta "¿Lleva inmovilización?" antes de pedir la fecha.

**Arquitectura:**
- **Backend:**
  - Paquete nuevo `segmentmetric`, con dos entidades aisladas por `@TenantId`, `SegmentMetric` y `SegmentMetricOption`, y un servicio con interfaz + `Impl`.
  - En los seguimientos, la entidad nueva `MetricValue` cuelga de `SegmentEvaluation`, y un paso nuevo de la cadena de validación (`SegmentMetricsStep`) controla los valores.
  - La validación por tipo de las plantillas se generaliza con la interfaz `AnswerConstraints`, para que preguntas y métricas compartan las reglas.
- **Frontend:**
  - Pantalla de métricas por segmento bajo `/catalogos`.
  - Componente `segment-metrics-fields` en el editor de evaluaciones.
  - Control Sí/No y fecha condicional en el diagnóstico.

**Stack:** Java 21, Spring Boot 3.5, JPA/Hibernate (`@TenantId`), Flyway, Postgres, JUnit 5 + Testcontainers + Mockito. Angular 21 (standalone, signals, formularios reactivos), Vitest.

**Spec:** `docs/superpowers/specs/2026-11-07-segment-rom-design.md`. **Mockups:** `docs/mockups/h11-rom-segmento/` (el README describe cada PNG).

**Worktree / rama:** `C:\Users\Maxi\Desktop\TO Managment-rom`, rama `feat/segment-rom` (desde `main`).

## Restricciones globales

- **Idioma:** la UI y los mensajes en español (textos exactos abajo). El código, los identificadores, los commits y los nombres de archivo, en inglés. Cada commit termina con la línea `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Diseño del backend (CLAUDE.md):**
  - nada de `import *`, tampoco en los `import static`;
  - servicios con interfaz + `Impl`, inyección por constructor;
  - los valores fijos del dominio van en enums;
  - solo se lanzan subclases de `ApplicationException`, cada una con su `ErrorCode`;
  - nunca se loguean valores clínicos ni datos personales.
- **Esquema:**
  - Una sola migración nueva, `backend/src/main/resources/db/migration/V13__segment_rom.sql`. La crea la Tarea 2 y la amplían las Tareas 3 y 4 (todavía no está aplicada en ningún lado).
  - En el mismo cambio se edita `supabase/init/01_schema.sql`. Ese archivo describe el estado final: se editan los `CREATE TABLE` en el lugar, se agregan `CREATE TABLE IF NOT EXISTS` y `CREATE [UNIQUE] INDEX IF NOT EXISTS`, y el encabezado pasa a decir "V1..V13".
  - En `supabase/init/02_seed.sql` se agrega el comentario `-- (É11 no define datos iniciales.)`.
- **Tenancy:** cada tabla nueva lleva `establishment_id BIGINT NOT NULL REFERENCES establishment (id)`, y su entidad, `@TenantId @Column(name = "establishment_id", nullable = false, updatable = false) private Long establishmentId;`, igual que `Question`, `QuestionOption` y `Answer`. Los repositorios no reciben el tenant.
- **Textos exactos:**
  - ErrorCodes nuevos (se agregan antes de `INTERNAL_ERROR`):
    - `SEGMENT_METRIC_NOT_FOUND` (404, "La métrica no existe.");
    - `SEGMENT_METRIC_NAME_TAKEN` (409, "Ya hay una métrica con ese nombre en este segmento.");
    - `SEGMENT_METRIC_OPTIONS_INVALID` (422, "Las opciones tienen que ser al menos 2 y no repetirse.");
    - `SEGMENT_METRIC_ORDER_INVALID` (422, "El orden tiene que incluir todas las métricas del segmento.");
    - `METRIC_VALUE_REQUIRED` (422, "Completá las métricas obligatorias.");
    - `METRIC_VALUE_INVALID` (422, "Hay métricas con un valor que no es válido para su tipo.");
    - `METRIC_NOT_OF_SEGMENT` (422, "Hay métricas que no corresponden al segmento evaluado.");
    - `IMMOBILIZATION_ANSWER_REQUIRED` (422, "Indicá si lleva inmovilización.").
  - Se cambia el mensaje de `IMMOBILIZATION_DATE_REQUIRED` a "Con inmovilización, ingresá la fecha de inmovilización.".
  - Mensajes por campo de las métricas:
    - en una métrica obligatoria vacía, "Completá esta métrica.";
    - en un valor inválido, "El valor no es válido para esta métrica.".
  - Copias de la UI: las de las historias H11.1 a H11.5 del spec y de los mockups. Por ejemplo, "Métricas ROM", "Nueva métrica", "Nombre de la métrica", "Obligatoria", "El tipo no se puede cambiar después de crear la métrica.", "Este segmento no tiene métricas ROM definidas.", "Definir en Catálogos", "¿Lleva inmovilización?", "Sin inmovilización" e "Indicá si lleva inmovilización.".
- **Verificación: solo compilación** (instrucción vigente del usuario). Se escriben los tests, pero no se corren las suites.
  - Backend, desde `backend/` en Git Bash: `MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-rom/backend:/app" -v to-management-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test-compile`
  - Frontend, desde `frontend/`: `npx ng build` (sin errores ni warnings) y `npx tsc -p tsconfig.spec.json --noEmit`.
  - Si `frontend/node_modules` no existe en el worktree, se crea como **junction** a `C:\Users\Maxi\Desktop\TO Managment\frontend\node_modules` (`cmd /c mklink /J`). Nunca se borra con `rm -rf`, y no se corre `npm install`.
- **Rulings del plan (decisiones sobre el spec):**
  1. Las opciones de una métrica **no** llevan índice único en la base. La unicidad (sin distinguir mayúsculas) la controla la aplicación, como en `question_option`, porque las opciones se editan en el lugar por posición.
  2. El conteo "Métricas ROM · n" sale de `GET /api/segment-metrics/counts`.
  3. Los errores por métrica viajan como `fieldErrors` con la ruta `evaluations[<i>].metrics[<metricId>]`.

## Foco de revisión

1. **Cambiar el segmento después de cargar métricas:** el bloque se recarga, los valores viejos se descartan y el payload nunca manda métricas del segmento anterior. Se fija en la Tarea 6, en el spec del editor.
2. **Métrica opcional vacía:** no se envía en el payload, y el backend la acepta sin error. Se fija en la Tarea 3 (test de integración) y en la Tarea 6 (spec del payload).
3. **Métrica desactivada después de cargar seguimientos:** el seguimiento viejo la sigue mostrando con su nombre, y los seguimientos nuevos la rechazan con `METRIC_NOT_OF_SEGMENT`. Se fija en la Tarea 3.
4. **Diagnóstico conservador con No y una fecha cargada antes en el formulario:** la fecha no se envía y se guarda null. Se fija en la Tarea 4 (backend: No con fecha da fecha null) y en la Tarea 7 (el payload no lleva fecha).
5. **Opciones con mayúsculas distintas** ("Firme" y "firme"): se rechazan al definirlas. Un valor enviado como "firme" se guarda con la grafía canónica "Firme". Se fija en la Tarea 2 (422) y en la Tarea 3 (valor canónico).

---

### Tarea 1: Reglas por tipo reutilizables (`AnswerConstraints`)

**Archivos:**
- Crear: `backend/src/main/java/com/tomanagement/template/rules/AnswerConstraints.java`
- Modificar:
  - `backend/src/main/java/com/tomanagement/template/rules/AnswerTypeRules.java`;
  - `NumericRules.java`, `YesNoRules.java`, `OptionRules.java` y `TextRules.java` (mismo paquete);
  - `backend/src/main/java/com/tomanagement/template/entity/Question.java` (pasa a implementar `AnswerConstraints`);
  - todo llamador de `read(...)` o `canonicalExpectedValue(...)`. Para encontrarlos: `grep -rn "canonicalExpectedValue\|\.read(" backend/src/main`.
- Tests: `backend/src/test/java/com/tomanagement/template/rules/AnswerTypeRulesTest.java`.

**Interfaces:**
- Produce: `public interface AnswerConstraints { BigDecimal getMinValue(); BigDecimal getMaxValue(); List<String> getOptionLabels(); }`.
- `AnswerTypeRules` cambia:
  - `Optional<AnswerValue> read(RawAnswer raw, AnswerConstraints constraints)`;
  - `Optional<String> canonicalExpectedValue(String rawValue, AnswerConstraints constraints)`.

- [ ] **Paso 1: Test.** En `AnswerTypeRulesTest`, agregar casos que llamen a las reglas con un `AnswerConstraints` que no sea `Question`. Usar un record de test:

```java
private record Limits(BigDecimal getMinValue, BigDecimal getMaxValue, List<String> getOptionLabels)
        implements AnswerConstraints {
}

@Test
void optionRulesAcceptAnyConstraintsAndReturnTheCanonicalLabel() {
    Limits limits = new Limits(null, null, List.of("Firme", "Blanda"));
    assertThat(optionRules.read(new RawAnswer(null, null, "firme", null), limits))
            .contains(AnswerValue.ofOption("Firme"));
}

@Test
void numericRulesWithoutLimitsAcceptAnyNumber() {
    Limits limits = new Limits(null, null, List.of());
    assertThat(numericRules.read(new RawAnswer(new BigDecimal("-10"), null, null, null), limits)).isPresent();
}
```

  Si `AnswerValue` no implementa `equals`, comparar con `.map(value -> value.text().orElseThrow())`.

- [ ] **Paso 2: Implementar.**
  - Crear la interfaz, con un Javadoc: "Limits a typed answer is checked against (template questions and segment metrics)."
  - `Question implements AnswerConstraints`: ya tiene `getMinValue`, `getMaxValue` y `getOptionLabels`, así que solo se agrega `@Override` donde corresponda.
  - Cambiar las firmas en `AnswerTypeRules` y en las 4 reglas, reemplazando `Question` por `AnswerConstraints`. La lógica no cambia.
  - Ajustar los llamadores; con `Question` siguen compilando sin cambios de lógica.

- [ ] **Paso 3: Compilar el backend** con el comando de las restricciones globales. Esperado: sin salida.

- [ ] **Paso 4: Commit.**

```bash
git add -A backend
git commit -m "refactor: answer type rules read against AnswerConstraints (E11)"
```

---

### Tarea 2: Catálogo de métricas por segmento (backend)

**Archivos:**
- Crear la migración `backend/src/main/resources/db/migration/V13__segment_rom.sql` (con la parte de métricas).
- Crear el paquete `backend/src/main/java/com/tomanagement/segmentmetric/`:
  - `entity/SegmentMetric.java` y `entity/SegmentMetricOption.java`;
  - `repository/SegmentMetricRepository.java`;
  - `dto/SegmentMetricRequest.java`, `dto/SegmentMetricResponse.java`, `dto/SegmentMetricOrderRequest.java`, `dto/ChangeSegmentMetricActiveRequest.java` y `dto/SegmentMetricCountResponse.java`;
  - `service/SegmentMetricService.java` y `service/SegmentMetricServiceImpl.java`;
  - `controller/SegmentMetricController.java`.
- Modificar:
  - `backend/src/main/java/com/tomanagement/common/exception/ErrorCode.java` (los 4 códigos `SEGMENT_METRIC_*`);
  - `supabase/init/01_schema.sql` (tablas nuevas y encabezado);
  - `supabase/init/02_seed.sql` (el comentario de É11).
- Tests:
  - crear `backend/src/test/java/com/tomanagement/segmentmetric/controller/SegmentMetricControllerTest.java`;
  - crear `backend/src/test/java/com/tomanagement/segmentmetric/entity/SegmentMetricTest.java`;
  - modificar `backend/src/test/java/com/tomanagement/tenant/CrossEstablishmentIsolationTest.java`.

**Interfaces:**
- Consume: `AnswerType` (`com.tomanagement.template.entity`) y `AnswerConstraints` (Tarea 1).
- Produce:
  - `SegmentMetric implements AnswerConstraints`, con:
    - `getId()`, `getSegment()` (`BodySegment`), `getName()`, `getAnswerType()`, `isRequired()`, `isActive()`, `getDisplayOrder()`;
    - `getOptionLabels()` (`List<String>`); `getMinValue()` y `getMaxValue()` devuelven null.
  - `SegmentMetricRepository`:
    - `List<SegmentMetric> findBySegment_IdOrderByDisplayOrderAsc(Long segmentId)`;
    - `List<SegmentMetric> findBySegment_IdAndActiveTrueOrderByDisplayOrderAsc(Long segmentId)`;
    - `List<SegmentMetric> findByIdIn(Collection<Long> ids)`;
    - `boolean existsBySegment_IdAndNameIgnoreCase(Long segmentId, String name)`;
    - `boolean existsBySegment_IdAndNameIgnoreCaseAndIdNot(Long segmentId, String name, Long id)`;
    - `int maxDisplayOrder(Long segmentId)` (JPQL `coalesce(max(m.displayOrder), 0)`);
    - una consulta de conteo para `/counts`.
  - JSON `SegmentMetricResponse`: `{ id, segmentId, name, answerType, required, active, displayOrder, options: string[] }`.
  - Endpoints:
    - `GET /api/segments/{segmentId}/metrics` (cualquier usuario autenticado);
    - `POST /api/segments/{segmentId}/metrics` (201, ADMIN o SUPERADMIN);
    - `PUT /api/segments/{segmentId}/metrics/{id}` (ADMIN o SUPERADMIN);
    - `PATCH /api/segments/{segmentId}/metrics/{id}/active` con `{active}` (ADMIN o SUPERADMIN);
    - `PUT /api/segments/{segmentId}/metrics/order` con `{ids: number[]}`, que devuelve la lista (ADMIN o SUPERADMIN);
    - `GET /api/segment-metrics/counts`, que devuelve `[{segmentId, count}]` contando solo las activas (cualquier usuario autenticado).

- [ ] **Paso 1: Migración (parte de métricas).**

```sql
-- É11: métricas ROM por segmento, valores por evaluación e inmovilización opcional.
CREATE TABLE segment_metric (
    id               BIGSERIAL    PRIMARY KEY,
    establishment_id BIGINT       NOT NULL REFERENCES establishment (id),
    body_segment_id  BIGINT       NOT NULL REFERENCES body_segment (id),
    name             VARCHAR(120) NOT NULL,
    answer_type      VARCHAR(20)  NOT NULL CHECK (answer_type IN ('NUMERIC', 'YES_NO', 'OPTION', 'TEXT')),
    required         BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INTEGER      NOT NULL
);
CREATE UNIQUE INDEX ux_segment_metric_segment_name ON segment_metric (body_segment_id, lower(name));
CREATE INDEX ix_segment_metric_segment ON segment_metric (body_segment_id, display_order);

-- Labels are unique per metric (case-insensitive), enforced by the app so options can be edited in place.
CREATE TABLE segment_metric_option (
    id                BIGSERIAL    PRIMARY KEY,
    establishment_id  BIGINT       NOT NULL REFERENCES establishment (id),
    segment_metric_id BIGINT       NOT NULL REFERENCES segment_metric (id) ON DELETE CASCADE,
    label             VARCHAR(120) NOT NULL,
    display_order     INTEGER      NOT NULL
);
CREATE INDEX ix_segment_metric_option_metric ON segment_metric_option (segment_metric_id, display_order);
```

  Lo mismo va en `01_schema.sql`, con `IF NOT EXISTS` y bajo el comentario `-- Métricas ROM por segmento (É11)`, después de la sección de plantillas.

- [ ] **Paso 2: Tests de entidad y de integración.**
  - `SegmentMetricTest` (unitario):
    - `replaceOptions` reetiqueta en el lugar y conserva los ids de las opciones que quedan;
    - `moveTo(order)` cambia el orden;
    - `getOptionLabels()` devuelve las opciones en orden.
  - `SegmentMetricControllerTest`. Usa el setup de `CatalogControllerTest`: truncar `BODY_SEGMENT`, `ESTABLISHMENT`; tokens de admin y terapeuta; segmentos Codo y Hombro sembrados dentro de `tenancy.runAs`. Tests:
    - `adminCreatesAMetricAtTheEndOfTheOrder`: con dos POST a Codo, el segundo queda con `displayOrder` 2. Respuesta 201 con `options: []`.
    - `optionMetricNeedsTwoDistinctOptions`:
      - `options: ["Firme"]` da 422 `SEGMENT_METRIC_OPTIONS_INVALID`;
      - `["Firme", "firme"]` da 422;
      - `["Firme", "Blanda"]` da 201.
    - `nameIsUniquePerSegmentIgnoringCase`:
      - "Flexión" en Codo dos veces (la segunda como "flexión") da 409 `SEGMENT_METRIC_NAME_TAKEN`;
      - "Flexión" en Hombro da 201.
    - `editingKeepsTheType`: el PUT con nombre nuevo y `required: true` devuelve el mismo `answerType` (el DTO de edición no lo trae).
    - `deactivateAndReactivate`: el PATCH active false y luego true.
    - `reorderFollowsTheGivenIds`:
      - con 3 métricas, `PUT /order` con `[c, a, b]` las devuelve en ese orden con `displayOrder` 1, 2 y 3;
      - una lista incompleta o con un id de otro segmento da 422 `SEGMENT_METRIC_ORDER_INVALID`.
    - `therapistReadsButCannotWrite`: el GET da 200 y el POST, 403.
    - `countsOnlyActiveMetricsPerSegment`: con 2 activas y 1 inactiva en Codo, `/counts` devuelve `{segmentId: codo, count: 2}`.
    - `unknownMetricIs404`: el PUT de un id inexistente da 404 `SEGMENT_METRIC_NOT_FOUND`.
  - `CrossEstablishmentIsolationTest`, en `readsNeverCrossEstablishments`:
    - crear por API una métrica en el segmento Muñeca de B (`b.wristId()`) con el token de admin de B;
    - con el token de A, el `GET /api/segments/{b.wristId}/metrics` da lista vacía (o 404 `CATALOG_ITEM_NOT_FOUND` si el servicio valida el segmento: verificar el segmento con `bodySegmentRepository.findById` y aceptar el 404);
    - el `PUT` de esa métrica con el token de A da 404.

- [ ] **Paso 3: Entidades.**
  - `SegmentMetric`:
    - `@Entity @Table(name = "segment_metric")`;
    - campos: `id`, `establishmentId` (`@TenantId`), `@ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name = "body_segment_id", nullable = false, updatable = false) BodySegment segment`, `name` (`length = 120`), `@Enumerated(EnumType.STRING) @Column(name = "answer_type", nullable = false, updatable = false, length = 20) AnswerType answerType`, `required`, `active`, `displayOrder` (`display_order`);
    - `@OneToMany(mappedBy = "metric", cascade = ALL, orphanRemoval = true) @OrderBy("displayOrder ASC") List<SegmentMetricOption> options`;
    - constructor `SegmentMetric(BodySegment segment, String name, AnswerType answerType, boolean required, int displayOrder, List<String> optionLabels)`;
    - métodos `rename(String)`, `changeRequired(boolean)`, `changeActive(boolean)`, `moveTo(int)` y `replaceOptions(List<String>)`. Este último copia el esquema de `Question` (reetiqueta por índice, agrega las nuevas y quita la cola);
    - no tiene setter de `answerType`.
  - `SegmentMetricOption`: `id`, `establishmentId` (`@TenantId`), `@ManyToOne` metric (`segment_metric_id`), `label` (120) y `displayOrder`.

- [ ] **Paso 4: DTOs.**

```java
public record SegmentMetricRequest(
        @NotBlank(message = "Ingresá el nombre de la métrica.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String name,
        AnswerType answerType,          // required on create, ignored on update
        boolean required,
        List<@NotBlank(message = "Las opciones no pueden quedar vacías.") @Size(max = 120) String> options) {
    public SegmentMetricRequest {
        name = name == null ? null : name.trim();
        options = options == null ? List.of() : options.stream().map(String::trim).toList();
    }
}
public record SegmentMetricOrderRequest(@NotEmpty List<@NotNull Long> ids) { }
public record ChangeSegmentMetricActiveRequest(@NotNull Boolean active) { }
public record SegmentMetricCountResponse(Long segmentId, long count) { }
public record SegmentMetricResponse(Long id, Long segmentId, String name, AnswerType answerType, boolean required,
        boolean active, int displayOrder, List<String> options) {
    public static SegmentMetricResponse from(SegmentMetric metric) { ... }
}
```

  - En el alta, si `answerType` es null → `FieldValidationException(ErrorCode.VALIDATION_ERROR, List.of(new FieldErrorResponse("answerType", "Elegí el tipo de la métrica.")))`.
  - Las opciones solo se aceptan con el tipo `OPTION`; con otro tipo se ignoran y se guardan vacías.

- [ ] **Paso 5: Servicio (interfaz + `Impl`, `@Transactional`).**
  - Métodos: `list(segmentId)`, `create(segmentId, request)`, `update(segmentId, id, request)`, `changeActive(segmentId, id, active)`, `reorder(segmentId, ids)` y `counts()`.
  - El segmento se busca con `BodySegmentRepository.findById`, y si no existe → `ResourceNotFoundException(CATALOG_ITEM_NOT_FOUND)`.
  - Una métrica cuyo `segment.id` no coincide → `SEGMENT_METRIC_NOT_FOUND`.
  - Las opciones se validan con un helper privado: al menos `QuestionLimit.MINIMUM_OPTIONS`, y únicas con `toLowerCase(Locale.ROOT)`. Si no, `SEGMENT_METRIC_OPTIONS_INVALID`.
  - Nombres repetidos: `existsBy...` → `DuplicateResourceException(SEGMENT_METRIC_NAME_TAKEN)`. La carrera con el índice único se traduce en `saveAndFlush` con un catch de `DataIntegrityViolationException`, igual que en `AbstractCatalogService.save`.
  - El reorden exige que el conjunto de ids sea igual al de todas las métricas del segmento. Asigna `moveTo(i + 1)`.

- [ ] **Paso 6: Controlador** (`@RestController`, sin `@PreAuthorize` de clase; `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")` en las escrituras). Dos mappings:
  - `@RequestMapping("/api/segments/{segmentId}/metrics")`;
  - el conteo, en el mismo controlador, con la ruta absoluta `@GetMapping("/api/segment-metrics/counts")`. Como la clase tiene un prefijo, poner el conteo en un controlador aparte y chico, `SegmentMetricCountController`.

- [ ] **Paso 7: ErrorCodes** de esta tarea: `SEGMENT_METRIC_NOT_FOUND`, `SEGMENT_METRIC_NAME_TAKEN`, `SEGMENT_METRIC_OPTIONS_INVALID` y `SEGMENT_METRIC_ORDER_INVALID`, con los textos de las restricciones globales.

- [ ] **Paso 8: Compilar el backend.** Esperado: sin salida.

- [ ] **Paso 9: Commit.**

```bash
git add -A backend supabase
git commit -m "feat: ROM metrics per body segment (E11)"
```

---

### Tarea 3: Valores de métricas en los seguimientos (backend)

**Archivos:**
- Modificar:
  - `V13__segment_rom.sql` y `supabase/init/01_schema.sql` (tabla `metric_value` y `segment_evaluation.rom` admite NULL);
  - `backend/src/main/java/com/tomanagement/followup/entity/ClinicalMetrics.java` (rom nullable);
  - `.../followup/entity/SegmentEvaluation.java` (colección `metricValues` y método `record(...)`);
  - `.../followup/dto/SegmentEvaluationRequest.java` (sin `rom`, con `metrics`);
  - `.../followup/dto/FollowUpResponse.java` (`metrics` en la evaluación; `rom` nullable);
  - `.../followup/mapper/FollowUpMapper.java`;
  - `.../followup/service/FollowUpRegistrationServiceImpl.java`;
  - `.../followup/validation/ClinicalRange.java` (se quita `ROM`) y `ClinicalRangeStep.java` (no valida rom);
  - `.../followup/validation/FollowUpValidationChain.java` (suma el paso nuevo);
  - `ErrorCode.java` (`METRIC_VALUE_REQUIRED`, `METRIC_VALUE_INVALID` y `METRIC_NOT_OF_SEGMENT`).
- Crear:
  - `.../followup/entity/MetricValue.java`;
  - `.../followup/dto/MetricValueRequest.java`;
  - `.../followup/validation/SegmentMetricsStep.java`;
  - `.../followup/service/SegmentMetricValueReader.java`. Lee y valida los valores de una evaluación, y lo usan el paso y el servicio de registro, igual que `TemplateAnswerReader`.
- Tests:
  - `backend/src/test/java/com/tomanagement/followup/controller/FollowUpControllerTest.java`;
  - `backend/src/test/java/com/tomanagement/tenant/CrossEstablishmentIsolationTest.java` (su `followUp()` manda `rom`: quitarlo).

**Interfaces:**
- Consume: `SegmentMetric` y `SegmentMetricRepository` (Tarea 2); `AnswerTypeRulesFactory.forType(type).read(RawAnswer, AnswerConstraints)` (Tarea 1).
- Produce:
  - Request:
    - `SegmentEvaluationRequest(segmentId, edema, pain, strength, templateId, answers, List<@NotNull @Valid MetricValueRequest> metrics)`, con `metricsOrEmpty()`;
    - `MetricValueRequest(@NotNull Long metricId, BigDecimal numericValue, Boolean booleanValue, String optionValue, String textValue)`, con `toRawAnswer()` y un `toString` enmascarado.
  - Respuesta de la evaluación: `{ id, segment, edema, pain, rom (nullable), strength, template, answers, metrics: [{ metricId, name, answerType, numericValue, booleanValue, optionValue, textValue }] }`, con las métricas en el orden `displayOrder` de la métrica.
  - Rutas de los `fieldErrors`: `evaluations[<i>].metrics[<metricId>]`.

- [ ] **Paso 1: Migración (parte de valores).** Se agrega a `V13__segment_rom.sql`:

```sql
ALTER TABLE segment_evaluation ALTER COLUMN rom DROP NOT NULL;

CREATE TABLE metric_value (
    id                    BIGSERIAL      PRIMARY KEY,
    establishment_id      BIGINT         NOT NULL REFERENCES establishment (id),
    segment_evaluation_id BIGINT         NOT NULL REFERENCES segment_evaluation (id),
    segment_metric_id     BIGINT         NOT NULL REFERENCES segment_metric (id),
    numeric_value         NUMERIC(12, 3),
    boolean_value         BOOLEAN,
    option_value          VARCHAR(120),
    text_value            TEXT,
    CONSTRAINT uq_metric_value_metric UNIQUE (segment_evaluation_id, segment_metric_id),
    CONSTRAINT ck_metric_value_single_value CHECK (num_nonnulls(numeric_value, boolean_value, option_value, text_value) = 1)
);
CREATE INDEX ix_metric_value_metric ON metric_value (segment_metric_id);
```

  En `01_schema.sql`, la columna `rom` de `segment_evaluation` pierde el `NOT NULL`, y `metric_value` se agrega con `IF NOT EXISTS` después de `answer`.

- [ ] **Paso 2: Tests de integración** en `FollowUpControllerTest`:
  - Cambiar el helper `evaluation(...)` para que no reciba `rom` y acepte una lista `metrics`.
  - Agregar un helper `metric(Long id, Object value)` que arme `{metricId, numericValue|booleanValue|optionValue|textValue}` según el tipo Java del valor: `BigDecimal` o `String` numérica → `numericValue`; `Boolean` → `booleanValue`; los demás `String` → `optionValue` o `textValue`, con un parámetro explícito.
  - Sembrar en `seedData`, en Muñeca, estas métricas:
    - `flexion` (NUMERIC, obligatoria);
    - `complete` (YES_NO, opcional);
    - `feel` (OPTION, opcional, con `["Firme", "Blanda"]`);
    - `notes` (TEXT, opcional);
    - `old` (NUMERIC, inactiva).
  - Tests nuevos:
    - `savesMetricValuesWithoutRom`: con `flexion` 130, `complete` true y `feel` "firme" → 201. La respuesta trae `rom` null y `metrics` en orden, y `feel` vuelve como "Firme" (canónico).
    - `optionalMetricsMayBeOmitted`: solo `flexion` → 201 con 1 métrica.
    - `missingRequiredMetricIsAFieldError`: sin `flexion` → 422 `METRIC_VALUE_REQUIRED`, con `fieldErrors[?(@.field == 'evaluations[0].metrics[<flexionId>]')].message` == "Completá esta métrica.".
    - `wrongTypeOrUnknownOptionIsInvalid`:
      - `flexion` con `booleanValue` → 422 `METRIC_VALUE_INVALID` y el mensaje "El valor no es válido para esta métrica.";
      - `feel` "Vacía" → 422 `METRIC_VALUE_INVALID`.
    - `metricOfAnotherSegmentOrInactiveIsRejected`:
      - una métrica de Mano en la evaluación de Muñeca → 422 `METRIC_NOT_OF_SEGMENT`;
      - la métrica `old` (inactiva) → 422 `METRIC_NOT_OF_SEGMENT`.
    - `repeatedMetricIsRejected`: `flexion` dos veces → 422 `METRIC_VALUE_INVALID`.
    - `deactivatedMetricStillShowsInOldFollowUps`: guardar un seguimiento con `complete`, desactivar `complete` dentro de `tenancy.runAs`, y el GET de la lista lo sigue trayendo con su nombre.
  - Ajustar los tests existentes:
    - `outOfRangeMetricsComeBackOnePerField` deja de esperar el error de rom;
    - `needsAtLeastOneEvaluationAndTheRequiredMetrics` pasa a esperar `fieldErrors.length() == 1` (solo fuerza).
  - Agregar el caso `legacyFollowUpKeepsItsRom`: sembrar directo con el repositorio una evaluación con `new ClinicalMetrics(edema, pain, new BigDecimal("90"), strength)` → la respuesta trae `rom` 90 y `metrics` vacío.

- [ ] **Paso 3: Entidades.**
  - `ClinicalMetrics`: `@Column(precision = 6, scale = 2) private BigDecimal rom;`, nullable, sin `requireNonNull`. Javadoc: "rom: legacy fixed ROM (before É11); null for new evaluations".
  - Constructores:
    - se agrega `ClinicalMetrics(BigDecimal edema, int pain, BigDecimal strength)`, que deja rom en null;
    - se mantiene `ClinicalMetrics(BigDecimal edema, int pain, BigDecimal rom, BigDecimal strength)` para los tests de historia.
  - `MetricValue`: copia de `Answer`, con `@ManyToOne SegmentMetric metric` (`segment_metric_id`). El constructor es `MetricValue(SegmentEvaluation, SegmentMetric, AnswerValue)`, con el mismo switch por tipo.
  - `SegmentEvaluation`:
    - `@OneToMany(mappedBy = "segmentEvaluation", cascade = ALL, orphanRemoval = true) @OrderBy("id ASC") @BatchSize(size = 50) List<MetricValue> metricValues`;
    - `public void record(SegmentMetric metric, AnswerValue value)`;
    - `getMetricValues()`.

- [ ] **Paso 4: Lector y paso de validación.**
  - `SegmentMetricValueReader` (`@Component`) tiene un método:

    ```java
    public Map<SegmentMetric, AnswerValue> read(int evaluationIndex, Long segmentId, List<MetricValueRequest> values)
    ```

    1. Cargar `metricRepository.findBySegment_IdAndActiveTrueOrderByDisplayOrderAsc(segmentId)`.
    2. Si algún `metricId` no está entre esas métricas → `BusinessRuleException(METRIC_NOT_OF_SEGMENT)`.
    3. Si algún `metricId` está repetido → `BusinessRuleException(METRIC_VALUE_INVALID)`.
    4. Para cada valor, `rulesFactory.forType(metric.getAnswerType()).read(value.toRawAnswer(), metric)`. Si viene vacío, agregar el `FieldErrorResponse("evaluations[" + i + "].metrics[" + metricId + "]", "El valor no es válido para esta métrica.")`.
    5. Para cada métrica obligatoria sin valor, agregar el error de campo "Completá esta métrica.".
    6. Si hay errores, lanzar `FieldValidationException`: con `METRIC_VALUE_REQUIRED` si alguno es de obligatoria; si no, con `METRIC_VALUE_INVALID`.
    7. Devolver un `LinkedHashMap` en el orden de las métricas.
  - `SegmentMetricsStep extends FollowUpValidationStep` recorre las evaluaciones y llama a `reader.read(index, ...)`.
  - En la cadena, va después de `SegmentStep` y antes de `TemplateAnswersStep`: `treatment.then(clinicalRanges).then(segments).then(metrics).then(templateAnswers)`.
  - `ClinicalRange`: se quita `ROM`. `ClinicalRangeStep`: se quita la línea de rom.

- [ ] **Paso 5: Registro y respuesta.**
  - `FollowUpRegistrationServiceImpl.addEvaluation`:
    - `new ClinicalMetrics(request.edema(), request.pain(), request.strength())`;
    - después de `followUp.evaluate(...)`, `reader.read(index, segmentId, request.metricsOrEmpty()).forEach(evaluation::record)`. Pasar el índice: cambiar el `forEach` por un for con índice.
  - `FollowUpResponse.SegmentEvaluationResponse` suma `List<MetricValueResponse> metrics`, con el record anidado `MetricValueResponse(Long metricId, String name, AnswerType answerType, BigDecimal numericValue, Boolean booleanValue, String optionValue, String textValue)` y `toString` enmascarado.
  - `FollowUpMapper` arma las métricas desde `evaluation.getMetricValues()`, ordenadas por `metric.getDisplayOrder()`. El nombre sale de la métrica aunque esté inactiva.

- [ ] **Paso 6: ErrorCodes** de esta tarea (los tres `METRIC_*`, con los textos globales).

- [ ] **Paso 7: Isolation test.** En `CrossEstablishmentIsolationTest.followUp()`, quitar `evaluation.put("rom", 55)`. Muñeca no tiene métricas en ese fixture, así que no se envía ninguna.

- [ ] **Paso 8: Compilar el backend.**

- [ ] **Paso 9: Commit.**

```bash
git add -A backend supabase
git commit -m "feat: segment metric values replace the fixed ROM in follow-ups (E11)"
```

---

### Tarea 4: Inmovilización opcional (backend)

**Archivos:**
- Modificar:
  - `V13__segment_rom.sql` y `supabase/init/01_schema.sql`;
  - `backend/src/main/java/com/tomanagement/diagnosis/entity/ConservativeTreatment.java`;
  - `.../diagnosis/service/TreatmentFactory.java`;
  - `.../diagnosis/dto/TreatmentRequest.java` y `TreatmentResponse.java`;
  - `.../diagnosis/mapper/DiagnosisMapper.java`;
  - `ErrorCode.java` (`IMMOBILIZATION_ANSWER_REQUIRED` nuevo y mensaje nuevo para `IMMOBILIZATION_DATE_REQUIRED`).
- Tests:
  - `backend/src/test/java/com/tomanagement/diagnosis/service/TreatmentFactoryTest.java`;
  - `.../diagnosis/controller/DiagnosisControllerTest.java`;
  - las siembras de `FollowUpControllerTest` y `AppointmentControllerTest` (`new ConservativeTreatment(LocalDate)` pasa a `new ConservativeTreatment(true, LocalDate)`).

**Interfaces:**
- Produce:
  - `TreatmentRequest(type, surgeryDate, stitchesRemovalDate, surgeryTypeId, immobilizationDate, Boolean hasImmobilization)`, con el campo nuevo **al final**;
  - `TreatmentResponse(type, surgeryDate, stitchesRemovalDate, surgeryType, immobilizationDate, Boolean hasImmobilization, requiresScarType)`, donde `hasImmobilization` es null en los quirúrgicos;
  - `ConservativeTreatment(Boolean hasImmobilization, LocalDate immobilizationDate)`, más `hasImmobilization()` y `getImmobilizationDate()` (nullable).

- [ ] **Paso 1: Migración (parte de inmovilización).** Se agrega a `V13`:

```sql
ALTER TABLE conservative_treatment ADD COLUMN has_immobilization BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE conservative_treatment ALTER COLUMN immobilization_date DROP NOT NULL;
ALTER TABLE conservative_treatment ADD CONSTRAINT ck_conservative_immobilization
    CHECK ((has_immobilization AND immobilization_date IS NOT NULL)
        OR (NOT has_immobilization AND immobilization_date IS NULL));
```

  En `01_schema.sql`, el `CREATE TABLE conservative_treatment` queda con `has_immobilization BOOLEAN NOT NULL DEFAULT TRUE`, `immobilization_date DATE` y el mismo CHECK como `CONSTRAINT`.

- [ ] **Paso 2: Tests.**
  - `TreatmentFactoryTest`: actualizar las ~8 construcciones de `TreatmentRequest` con el sexto argumento (`true` en los conservadores, null en los quirúrgicos). Agregar:
    - `conservativeWithoutAnswerIsRejected`: `hasImmobilization` null → `IMMOBILIZATION_ANSWER_REQUIRED`;
    - `conservativeWithImmobilizationNeedsTheDate`: true sin fecha → `IMMOBILIZATION_DATE_REQUIRED`;
    - `conservativeWithoutImmobilizationIgnoresTheDate`: false con fecha → `getImmobilizationDate()` null y `hasImmobilization()` false.
  - `DiagnosisControllerTest`: el helper `conservative(...)` suma el parámetro `Boolean hasImmobilization`. Agregar:
    - `conservativeWithoutImmobilization`: false → 201 con `$.treatment.hasImmobilization == false` y `$.treatment.immobilizationDate` null;
    - `conservativeWithoutAnswerIs422`;
    - ajustar los existentes para que manden `true`.

- [ ] **Paso 3: Implementar.**

```java
@Column(name = "has_immobilization", nullable = false, updatable = false)
private boolean hasImmobilization;

@Column(name = "immobilization_date", updatable = false)
private LocalDate immobilizationDate;

/**
 * @throws BusinessRuleException IMMOBILIZATION_ANSWER_REQUIRED when the yes/no answer is missing,
 *         IMMOBILIZATION_DATE_REQUIRED when it has immobilization but no date
 */
public ConservativeTreatment(Boolean hasImmobilization, LocalDate immobilizationDate) {
    if (hasImmobilization == null) {
        throw new BusinessRuleException(ErrorCode.IMMOBILIZATION_ANSWER_REQUIRED);
    }
    if (hasImmobilization && immobilizationDate == null) {
        throw new BusinessRuleException(ErrorCode.IMMOBILIZATION_DATE_REQUIRED);
    }
    this.hasImmobilization = hasImmobilization;
    this.immobilizationDate = hasImmobilization ? immobilizationDate : null;
}
```

  - `TreatmentFactory`: `case CONSERVATIVE -> new ConservativeTreatment(request.hasImmobilization(), request.immobilizationDate());`.
  - `DiagnosisMapper`: los quirúrgicos llevan `hasImmobilization` null; los conservadores, `treatment.hasImmobilization()`.

- [ ] **Paso 4: Compilar el backend.**

- [ ] **Paso 5: Commit.**

```bash
git add -A backend supabase
git commit -m "feat: conservative treatment asks whether it has immobilization (E11)"
```

---

### Tarea 5: Pantalla de métricas por segmento (frontend)

**Archivos:**
- Crear en `frontend/src/app/features/segment-metrics/`:
  - `segment-metrics.models.ts` y `segment-metrics.service.ts` (+ `.spec.ts`);
  - `segment-metrics-page/segment-metrics-page.{ts,html,scss,spec.ts}` (lista, orden y activación, con el panel de escritorio);
  - `segment-metric-form/segment-metric-form.{ts,html,scss,spec.ts}` (componente de formulario reutilizable);
  - `segment-metric-form-page/segment-metric-form-page.{ts,html,scss,spec.ts}` (envoltura de página en móvil).
- Modificar:
  - `frontend/src/app/app.routes.ts`: `catalogos` pasa a `children`;
  - `frontend/src/app/features/catalogs/catalogs-page/catalogs-page.{ts,html,scss,spec.ts}`: botón "Métricas ROM · n" solo en Segmentos.

**Interfaces:**
- Consume la API de la Tarea 2.
- Produce:

```ts
// segment-metrics.models.ts
import { AnswerType } from '../templates/templates.models';
export interface SegmentMetric { id: number; segmentId: number; name: string; answerType: AnswerType; required: boolean; active: boolean; displayOrder: number; options: string[]; }
export interface SegmentMetricPayload { name: string; answerType?: AnswerType; required: boolean; options: string[]; }
export interface SegmentMetricCount { segmentId: number; count: number; }
export const SEGMENT_METRIC_ERROR_CODES = { nameTaken: 'SEGMENT_METRIC_NAME_TAKEN', optionsInvalid: 'SEGMENT_METRIC_OPTIONS_INVALID' } as const;

// segment-metrics.service.ts (providedIn: 'root')
list(segmentId: number): Observable<SegmentMetric[]>                         // GET /api/segments/{id}/metrics
create(segmentId: number, payload: SegmentMetricPayload): Observable<SegmentMetric>
update(segmentId: number, id: number, payload: SegmentMetricPayload): Observable<SegmentMetric>
setActive(segmentId: number, id: number, active: boolean): Observable<SegmentMetric>
reorder(segmentId: number, ids: number[]): Observable<SegmentMetric[]>     // PUT .../order {ids}
counts(): Observable<SegmentMetricCount[]>                                   // GET /api/segment-metrics/counts
```

- Rutas, dentro de `catalogos`, con `canActivate: [establishmentGuard, adminGuard]`:
  - `''` → `CatalogsPage`;
  - `segmentos/:segmentId` → `SegmentMetricsPage`, con el título `Métricas ROM · ${APP_NAME}`;
  - `segmentos/:segmentId/metricas/nueva` y `segmentos/:segmentId/metricas/:metricId` → `SegmentMetricFormPage`.

- [ ] **Paso 1: Specs.**
  - `segment-metrics.service.spec.ts`: las URLs y los cuerpos de los 6 métodos.
  - `segment-metric-form.spec.ts`:
    - nombre vacío → "Ingresá el nombre de la métrica.";
    - con el tipo Opción y una sola opción → "Agregá al menos 2 opciones.";
    - opciones repetidas ignorando mayúsculas → "Las opciones no se pueden repetir.";
    - en la edición el tipo se muestra como texto bloqueado y el payload no trae `answerType`;
    - un 409 `SEGMENT_METRIC_NAME_TAKEN` muestra "Ya hay una métrica con ese nombre en este segmento." en el campo nombre;
    - emite `saved` con la métrica devuelta.
  - `segment-metrics-page.spec.ts`:
    - muestra el estado vacío "Todavía no hay métricas ROM para este segmento.";
    - lista con tipo, "Obligatoria" e "Inactiva";
    - la flecha de bajar llama a `reorder` con el orden nuevo, y la de subir de la primera está deshabilitada;
    - el switch llama a `setActive`;
    - en escritorio (`matchMedia` stub) "Nueva métrica" abre el panel; en móvil navega a `.../metricas/nueva`.
  - `catalogs-page.spec.ts`: en Segmentos cada fila muestra "Métricas ROM · n" (de `counts()`) con el link a `/catalogos/segmentos/<id>`; en Cirugías no.

- [ ] **Paso 2: Implementar.** Se siguen los patrones existentes:
  - el formulario copia la estructura y las validaciones de `templates/question-form-page` (`type-chips`, opciones con `FormArray`, `MIN_OPTIONS = 2`), sin las condiciones de activación. Los estilos van en el scss propio del componente;
  - el panel de escritorio copia el patrón de `EstablishmentsPage` + `EstablishmentForm` (`injectIsDesktop`, `panelOpen`, `<aside role="dialog" aria-modal="true">`), con salidas `saved` y `cancelled`;
  - la vuelta a Catálogos usa `routerLink="/catalogos"` con `[queryParams]="{ tipo: 'segmentos' }"`;
  - el reorden mueve la métrica en la lista local y llama a `reorder`. Si falla, restaura el orden y muestra el error;
  - textos e íconos según los mockups `mobile-metricas.png`, `mobile-metricas-vacio.png`, `mobile-nueva-metrica-opcion.png`, `mobile-editar-metrica.png` y `desktop-metricas-panel.png`.

- [ ] **Paso 3: Compilar el frontend** (`npx ng build` y `npx tsc -p tsconfig.spec.json --noEmit`).

- [ ] **Paso 4: Commit.**

```bash
git add -A frontend/src
git commit -m "feat: ROM metrics screen per segment in Catálogos (E11)"
```

---

### Tarea 6: Bloque ROM en el seguimiento y en la tarjeta (frontend)

**Archivos:**
- Crear: `frontend/src/app/features/follow-ups/segment-metrics-fields/segment-metrics-fields.{ts,html,scss,spec.ts}`.
- Modificar:
  - `frontend/src/app/features/follow-ups/follow-ups.models.ts`;
  - `.../follow-ups/follow-up-form.ts` (+ `follow-up-form.spec.ts`);
  - `.../follow-ups/segment-evaluation-editor/segment-evaluation-editor.{ts,html,scss,spec.ts}`;
  - `.../follow-ups/follow-up-form-page/follow-up-form-page.{ts,html,spec.ts}`;
  - `.../follow-ups/follow-up-card/follow-up-card.{ts,html,scss,spec.ts}`;
  - los fixtures con `rom` en `features/diagnoses/diagnosis-page/diagnosis-page.spec.ts`.

**Interfaces:**
- Consume: `SegmentMetricsService.list(segmentId)` y el modelo `SegmentMetric` (Tarea 5).
- Produce, en los modelos:

```ts
export interface FollowUpMetricValue { metricId: number; name: string; answerType: AnswerType; numericValue: number | null; booleanValue: boolean | null; optionValue: string | null; textValue: string | null; }
// SegmentEvaluation: rom: number | null; metrics: FollowUpMetricValue[];
export interface MetricValuePayload { metricId: number; numericValue?: number; booleanValue?: boolean; optionValue?: string; textValue?: string; }
// SegmentEvaluationPayload: sin rom; metrics: MetricValuePayload[];
export type Metric = 'edema' | 'pain' | 'strength';   // se quita 'rom' de Metric y METRIC_LABELS
```

- En `follow-up-form.ts`:
  - `EvaluationControls` pierde `rom` y suma `metrics: FormRecord<FormControl<string>>`;
  - `resetMetrics(metrics, list: SegmentMetric[])` deja un control por métrica activa;
  - `EvaluationProblems` pierde `rom` y suma `metrics: Record<number, string>`;
  - `evaluationProblems(value, template, metrics: SegmentMetric[])`: una obligatoria vacía da "Completá esta métrica." y un número inválido, "Ingresá un número.";
  - `evaluationPayload(value, template, metrics)` arma `metrics` omitiendo las vacías. El valor de NUMERIC sale de `parseDecimal`; YES_NO, de `'true'`/`'false'`; OPTION es la etiqueta; TEXT, el texto recortado;
  - `metricErrorsByEvaluation` reconoce además `evaluations[i].metrics[<id>]` y devuelve `{ fixed: Partial<Record<Metric, string>>, metrics: Record<number, string> }` por índice.
- `SegmentMetricsFields`:
  - entradas `metrics` (`SegmentMetric[]`, solo activas), `values` (`FormRecord<FormControl<string>>`), `problems` (`Record<number, string>`), `showProblems`, `idPrefix` y `canDefine` (boolean: muestra "Definir en Catálogos");
  - renderiza según el tipo, con los mismos patrones visuales de `template-questions`: input decimal, segmentado Sí/No, chips de opción (tocar la elegida la limpia) y textarea. Las métricas opcionales llevan "(opc.)".
- El editor, al cambiar `segmentId`:
  1. llama a `SegmentMetricsService.list(id)`;
  2. filtra las activas;
  3. hace `resetMetrics`;
  4. emite `metricsChange` con la lista.

  La página guarda las métricas por grupo, como hace con las plantillas (`Map<EvaluationGroup, SegmentMetric[]>`), y las usa en `evaluationProblems` y en el payload. `canDefine` sale de `AuthService.isAdminInEstablishment()` y el link apunta a `/catalogos/segmentos/<segmentId>`.

- [ ] **Paso 1: Specs.**
  - `follow-up-form.spec.ts`:
    - una obligatoria vacía produce el error;
    - el payload omite las opcionales vacías y convierte cada tipo;
    - el payload no trae `rom`;
    - `metricErrorsByEvaluation` lee `evaluations[0].metrics[7]` → `metrics[7]`.
    - Ajustar los casos viejos de rom.
  - `segment-metrics-fields.spec.ts`:
    - renderiza los 4 tipos;
    - "(opc.)" solo en las opcionales;
    - el chip elegido se des-elige al tocarlo;
    - sin métricas muestra "Este segmento no tiene métricas ROM definidas." y el link solo con `canDefine`.
  - `segment-evaluation-editor.spec.ts`: al cambiar el segmento pide las métricas del nuevo, descarta los valores cargados (foco de revisión 1) y emite `metricsChange`.
  - `follow-up-form-page.spec.ts`: no deja guardar con una obligatoria vacía, y muestra el error del servidor por métrica.
  - `follow-up-card.spec.ts`: una evaluación nueva muestra el bloque "ROM" con "Nombre: valor" (Sí/No como "Sí"/"No", números con `formatNumber`); una vieja con `rom` y sin métricas muestra la tarjeta "ROM 90".

- [ ] **Paso 2: Implementar** según `mobile-seguimiento-rom.png`, `mobile-seguimiento-sin-metricas.png`, `desktop-seguimiento-rom.png` y `mobile-tarjetas-seguimiento.png`.
  - El campo ROM del grid de métricas fijas desaparece: quedan Edema y Fuerza.
  - El bloque ROM va debajo del dolor y antes de Plantilla.
  - En la tarjeta, la grilla de tiles muestra Edema, Dolor y Fuerza (más ROM solo si `rom !== null`). El bloque ROM aparece si `metrics.length > 0`.

- [ ] **Paso 3: Compilar el frontend.**

- [ ] **Paso 4: Commit.**

```bash
git add -A frontend/src
git commit -m "feat: ROM block per segment in follow-ups and cards (E11)"
```

---

### Tarea 7: "¿Lleva inmovilización?" en el diagnóstico (frontend)

**Archivos:**
- Modificar:
  - `frontend/src/app/features/diagnoses/diagnoses.models.ts`;
  - `.../diagnoses/diagnosis-form-page/diagnosis-form-page.{ts,html,scss,spec.ts}`;
  - `.../diagnoses/diagnosis-card/diagnosis-card.{ts,html,spec.ts}`;
  - `.../diagnoses/diagnosis-page/diagnosis-page.{ts,html,spec.ts}`;
  - `.../diagnoses/diagnoses.service.spec.ts`;
  - los fixtures de `Treatment` en `follow-ups/follow-up-form-page/follow-up-form-page.spec.ts`.

**Interfaces:**
- `Treatment` suma `hasImmobilization: boolean | null`.
- `CreateDiagnosisPayload.treatment` suma `hasImmobilization: boolean | null`: null en los quirúrgicos, y en los conservadores el valor elegido.
- En `TREATMENT_OPTIONS`, el hint de Conservador pasa a "Sin cirugía. Indicá si lleva inmovilización.".
- Helper nuevo en `diagnoses.models.ts`:

```ts
export function immobilizationLabel(treatment: Treatment, format: (iso: string) => string): string {
  return treatment.hasImmobilization === false || treatment.immobilizationDate === null
    ? 'Sin inmovilización'
    : format(treatment.immobilizationDate);
}
```

- [ ] **Paso 1: Specs.**
  - `diagnosis-form-page.spec.ts`:
    - con Conservador y sin elegir Sí/No, al enviar aparece "Indicá si lleva inmovilización." y no se llama a la API;
    - con Sí aparece "Fecha de inmovilización" y es obligatoria;
    - con No la fecha se oculta y el payload trae `hasImmobilization: false, immobilizationDate: null`, aunque antes se haya escrito una fecha (foco de revisión 4).
  - `diagnosis-card.spec.ts` y `diagnosis-page.spec.ts`: un conservador sin inmovilización muestra "Sin inmovilización" y no rompe con la fecha null.
  - Actualizar los fixtures.

- [ ] **Paso 2: Implementar.**
  - El formulario suma el control `hasImmobilization: [null as boolean | null]`.
  - El control segmentado Sí/No (`role="radiogroup"`, botones con `aria-checked`) tiene estilos propios en el scss del componente, copiando `.segmented` de `follow-ups/template-questions`.
  - La fecha se muestra solo si `hasImmobilization === true`.
  - `validate()`: `problems.hasImmobilization = 'Indicá si lleva inmovilización.'` cuando es null; `problems.immobilizationDate` solo con Sí.
  - En la tarjeta y el detalle, `immobilizationLabel(...)` reemplaza a `formatDate(...!)`.
  - Diseño según `mobile-diagnostico-inmovilizacion-si.png`, `-no.png`, `desktop-diagnostico-inmovilizacion.png` y `mobile-diagnostico-sin-inmovilizacion.png`.

- [ ] **Paso 3: Compilar el frontend.**

- [ ] **Paso 4: Commit.**

```bash
git add -A frontend/src
git commit -m "feat: conservative diagnosis asks whether it has immobilization (E11)"
```

---

### Tarea 8: Chequeo de runtime, merge a test y prueba local (la hace el controlador)

- [ ] **Paso 1:** correr una vez las clases de integración clave. Es la única excepción a solo-compilación.

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Maxi/Desktop/TO Managment-rom/backend:/app" -v to-management-m2:/root/.m2 -v //var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /app maven:3.9-eclipse-temurin-21 ./mvnw -B -q test -Dtest='AnswerTypeRulesTest,SegmentMetricTest,SegmentMetricControllerTest,FollowUpControllerTest,TreatmentFactoryTest,DiagnosisControllerTest,CrossEstablishmentIsolationTest,CatalogControllerTest' -Dsurefire.failIfNoSpecifiedTests=false
```

  Esperado: todo en verde en `target/surefire-reports/*.txt`.

- [ ] **Paso 2:** correr `01_schema.sql` y `02_seed.sql` dos veces sobre un `postgres:15-alpine` vacío, con `-v app_schema=app`. Esperado: sin errores, y las tablas `segment_metric`, `segment_metric_option` y `metric_value` creadas.

- [ ] **Paso 3:** mergear a `test` en la carpeta principal: `git checkout test && git merge --no-ff feat/segment-rom` (V13 queda después de V11 y V12, que ya están en `test`). Luego `docker compose up -d --build`, y confirmar que V13 se aplicó y el backend arrancó. Nunca `down -v`.

- [ ] **Paso 4:** prueba manual por API:
  1. crear dos métricas en un segmento;
  2. cargar un seguimiento con valores: 201;
  3. dejar vacía una obligatoria: 422 `METRIC_VALUE_REQUIRED`;
  4. crear un diagnóstico conservador con `hasImmobilization: false`: 201.

## Flujo de ramas

`feat/segment-rom` → `test` para la prueba manual. A `main` llega solo la feat (nunca `test`), y **después** de que É9 y É10 estén en `main`, porque si no Flyway rechazaría V11 y V12 al desplegarlas detrás de V13. No hay variables de entorno nuevas.
