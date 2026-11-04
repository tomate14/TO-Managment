# Spec — Gestión de Terapia Ocupacional (MVP)

- **Fecha:** 2026-11-04
- **Estado:** pendiente de revisión
- **Reglas técnicas:** ver `CLAUDE.md` (stack, Docker/.env, diseño OO del backend, mockups obligatorios).

## 1. Objetivo

Dar a un equipo de terapeutas ocupacionales una app mobile-first para registrar pacientes, sus diagnósticos (con o sin cirugía) y la evolución en seguimientos. Cada seguimiento evalúa uno o más segmentos corporales con métricas fijas (edema, dolor, ROM, fuerza) y preguntas extra configurables, agrupadas en **plantillas con nombre reutilizables** (ej. "Fractura de muñeca").

**Éxito del MVP:** una terapeuta puede loguearse, dar de alta un paciente, crearle un diagnóstico, registrar seguimientos con evaluaciones por segmento y responder una plantilla con preguntas encadenadas, todo desde el celular.

## 2. Usuarios y permisos

- Varias terapeutas con cuentas propias. **Todas ven los mismos pacientes.**
- Roles (`enum Role`): `ADMIN` y `TERAPEUTA`.

| Acción | ADMIN | TERAPEUTA |
|---|---|---|
| Login, logout, cambiar la propia contraseña | ✔ | ✔ |
| ABM de usuarios | ✔ | ✘ |
| ABM de catálogos (tipos de cirugía, tipos de cicatriz, segmentos) | ✔ | ✘ |
| ABM de plantillas y preguntas | ✔ | ✔ |
| Pacientes, diagnósticos, seguimientos | ✔ | ✔ |

## 3. Modelo de dominio

### 3.1 Catálogos
`SurgeryType`, `ScarType`, `BodySegment`: `id`, `name` (único), `active`. Se desactivan, no se borran. Los inactivos no se ofrecen en altas nuevas, pero siguen visibles en registros históricos.

### 3.2 Plantillas de evaluación
- `EvaluationTemplate`: `id`, `name` (único dentro del segmento), `bodySegment`, `active`. Un segmento tiene 0..N plantillas.
- `Question`:
  - `id`, `template`, `text`, `answerType` (`enum AnswerType`: `NUMERIC`, `YES_NO`, `OPTION`, `TEXT`), `displayOrder` y `active`.
  - `minValue` / `maxValue`: opcionales, solo para `NUMERIC`.
  - `options`: lista de `QuestionOption`, solo para `OPTION`.
  - `parentQuestion`: opcional, de la misma plantilla.
  - `activationCondition`: obligatoria si hay padre. Tiene `operator` (`enum ConditionOperator`: `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`) y `expectedValue`.
- El operador tiene que ser compatible con el tipo del padre: `GREATER_THAN` y `LESS_THAN` solo sirven para `NUMERIC`.
- Una pregunta con respuestas registradas **no se edita ni se borra**: solo se desactiva. Las inactivas no aparecen en evaluaciones nuevas.
- No se permiten ciclos (por construcción, el padre ya tiene que existir en la plantilla).

### 3.3 Paciente
`Patient`:
- `dni`: identificador único, **no editable**.
- `firstName`, `lastName`, `birthDate`, `phone`, `occupation`.
- `healthInsurance` (obra social) y `healthInsuranceNumber`.
- `dominance`: `enum Dominance` (`LEFT`, `RIGHT`). En la UI se muestra como "Zurdo" / "Diestro".

### 3.4 Diagnóstico y tratamiento
- `Diagnosis`: `id`, `patient`, `description` (texto libre), `createdDate` (la asigna el sistema al crear) y `treatment`.
- El tratamiento se define al crear el diagnóstico y **no puede cambiar de tipo**:
  - **Quirúrgico** (`SurgicalTreatment`): `surgeryDate`, `stitchesRemovalDate` (≥ `surgeryDate`) y `surgeryType`.
  - **Conservador** (`ConservativeTreatment`): `immobilizationDate`.
- Un paciente tiene 0..N diagnósticos.

### 3.5 Seguimiento
- `FollowUp`: `id`, `diagnosis`, `date` y `scarType`. El tipo de cicatriz es **obligatorio si el tratamiento es quirúrgico y prohibido si es conservador**.
- `SegmentEvaluation` (1..N por seguimiento):
  - `bodySegment`.
  - `edema`: decimal o nulo.
  - `pain`: entero de 0 a 10.
  - `rom` y `strength`: decimales.
  - `template`: opcional, **una como máximo**, y tiene que pertenecer a ese segmento.
- `Answer`: `segmentEvaluation`, `question` y el valor tipado (`numericValue`, `booleanValue`, `optionValue` o `textValue`, según el tipo).
- Reglas de respuestas:
  - Una pregunta está **activa** si no tiene padre, o si la respuesta del padre cumple la condición.
  - Solo se aceptan respuestas a preguntas activas.
  - Las preguntas activas sin respuesta quedan vacías: ninguna pregunta de plantilla es obligatoria en el MVP.

## 4. Diseño del backend

Paquetes por feature: `auth`, `user`, `patient`, `diagnosis`, `followup`, `catalog`, `template`, `common`. Cada uno con `controller`, `service` (interfaz + `Impl`), `repository`, `dto`, `entity` y `mapper`.

| Patrón | Tipo | Aplicación |
|---|---|---|
| State | Comportamiento | `Treatment` (abstracto) → `SurgicalTreatment`, `ConservativeTreatment`: validan sus datos y responden `requiresScarType()`. |
| Factory Method | Creacional | `TreatmentFactory` crea el tratamiento a partir del DTO de entrada según `TreatmentType`. |
| Builder | Creacional | Construcción de `Patient`, `FollowUp`, `ErrorResponse` y DTOs grandes. |
| Composite | Estructural | `QuestionNode` representa el árbol de preguntas de una plantilla y su recorrido. |
| Specification | Comportamiento | `ActivationCondition` → una implementación por `ConditionOperator`; `isSatisfiedBy(parentAnswer)`. |
| Strategy + Factory | Comportamiento / Creacional | `AnswerValidator` por `AnswerType`, obtenido de `AnswerValidatorFactory`. |
| Chain of Responsibility | Comportamiento | `FollowUpValidationChain`: tratamiento → rangos clínicos (`enum ClinicalRange`) → respuestas de plantilla. |
| Facade | Estructural | `FollowUpRegistrationService` coordina validación, mapeo y persistencia de un seguimiento completo. |
| Adapter | Estructural | `TokenProvider` (interfaz) ← `JwtTokenProvider`, que aísla la librería de JWT. |

- **Enums:** `Role`, `Dominance`, `AnswerType`, `ConditionOperator`, `TreatmentType`, `ClinicalRange` y `ErrorCode` (mensaje + estado HTTP).
- **Excepciones:**
  - Base abstracta `ApplicationException`, con `ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`, `AuthenticationFailedException`, `AccessDeniedApplicationException` e `InternalSystemException`.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`) responde siempre con un `ErrorResponse` (`code`, `message`, `timestamp`, `fieldErrors` opcional).
  - Cualquier excepción no controlada se traduce a `InternalSystemException`, sin exponer el stack trace.
- **Persistencia:**
  - Flyway.
  - Tratamientos en tablas separadas por tipo, unidas al diagnóstico (herencia JPA `JOINED`).
  - Respuestas en una tabla con una columna por tipo de valor.
- **API REST** bajo `/api/**`, autenticada con JWT, salvo `POST /api/auth/login`.

## 5. Frontend (lineamientos)

- Mobile-first. Cada pantalla se implementa recién cuando su mockup está en `docs/mockups/<historia>/`.
- El formulario de plantilla se dibuja a partir del árbol que manda el backend. Al cambiar una respuesta, se muestran u ocultan las preguntas hijas con la misma lógica de condiciones que usa el backend.
- Un interceptor HTTP agrega el JWT; con un 401, lleva al login.

## 6. Épicas e historias

**Criterio común a toda historia con pantallas:** el mockup está recibido en `docs/mockups/<historia>/` antes de empezar a codificar.

### É0 — Base técnica

**H0.1 Stack con Docker**
Como desarrollador quiero levantar todo el sistema con un comando para trabajar y desplegar igual en cualquier máquina.
- `docker compose up -d --build` levanta frontend, backend, Postgres de Supabase, postgres-meta y Studio.
- Todas las variables salen del `.env`, y `.env.example` está actualizado.
- Los datos de Postgres persisten en el volumen definido en el `.env` y sobreviven a `docker compose down`.
- Flyway corre las migraciones al arrancar el backend.

### É1 — Acceso

**H1.1 Login**
Como terapeuta quiero ingresar con mi email y contraseña para acceder a la información de los pacientes.
- Con credenciales válidas de un usuario activo, se recibe un JWT y se entra a la pantalla principal.
- Con credenciales inválidas, aparece un mensaje genérico ("Email o contraseña incorrectos") que no revela cuál de los dos falló.
- Un usuario desactivado no puede ingresar.
- La expiración del token se toma de `JWT_EXPIRATION_MS`. Con el token vencido, cualquier acción redirige al login.
- Sin token, las rutas del frontend y la API (salvo el login) están protegidas.
- La contraseña viaja solo en el body del login y nunca se loguea.

**H1.2 Logout**
Como usuario quiero cerrar sesión para que nadie use mi cuenta en el dispositivo.
- Al cerrar sesión se borra el token del cliente y se vuelve al login.

**H1.3 Admin inicial**
Como responsable del sistema quiero que exista un admin al primer arranque para poder crear las demás cuentas.
- Al arrancar, si no existe ningún `ADMIN`, se crea uno con `ADMIN_EMAIL`, `ADMIN_PASSWORD` y `ADMIN_NAME` del `.env`.
- Si ya existe un admin, no se hace nada.

**H1.4 Gestión de terapeutas**
Como admin quiero crear, editar y desactivar usuarios para controlar quién accede.
- Alta con email (único), nombre, contraseña inicial y rol.
- Edición de nombre y rol. El email no se puede repetir.
- Desactivar a un usuario le impide loguearse. La admin no puede desactivarse a sí misma.

**H1.5 Cambiar mi contraseña**
Como usuario quiero cambiar mi contraseña para mantener mi cuenta segura.
- Pide la contraseña actual y la nueva (mínimo 8 caracteres).
- Si la actual es incorrecta, se rechaza el cambio.

### É2 — Catálogos (admin)

**H2.1 / H2.2 / H2.3 ABM de tipos de cirugía, tipos de cicatriz y segmentos corporales**
Como admin quiero mantener estas listas para que las terapeutas elijan de opciones consistentes.
- Alta con nombre único (no se permiten duplicados, sin distinguir mayúsculas).
- Edición del nombre.
- Desactivación: los inactivos no se ofrecen en altas nuevas y se siguen mostrando en el historial.

### É3 — Plantillas de evaluación

**H3.1 Crear plantilla**
Como terapeuta quiero crear una plantilla con nombre para un segmento y reutilizarla con distintos pacientes.
- Requiere nombre y segmento activo. El nombre es único dentro del segmento.

**H3.2 Agregar preguntas**
Como terapeuta quiero definir las preguntas de una plantilla para evaluar lo específico de cada caso.
- Cada pregunta lleva texto, tipo (numérica, sí/no, opción o texto) y orden.
- Las numéricas pueden tener mínimo y máximo (mín ≤ máx).
- Las de opción requieren al menos 2 opciones.

**H3.3 Encadenar preguntas**
Como terapeuta quiero que una pregunta aparezca solo según la respuesta de otra para no ver preguntas que no aplican.
- Se elige una pregunta padre de la misma plantilla y una condición (operador + valor esperado).
- El operador tiene que ser compatible con el tipo del padre; si no lo es, se rechaza con un error claro.

**H3.4 Desactivar preguntas o plantillas**
Como terapeuta quiero retirar preguntas o plantillas sin perder los seguimientos anteriores.
- Las preguntas con respuestas no se pueden editar ni borrar: solo desactivar.
- Las preguntas o plantillas inactivas no aparecen en seguimientos nuevos, y las respuestas viejas siguen visibles.

### É4 — Pacientes

**H4.1 Alta de paciente**
Como terapeuta quiero registrar un paciente con sus datos personales para tener su ficha.
- Todos los campos de 3.3 son obligatorios.
- Si el DNI está duplicado, se rechaza con un mensaje claro.
- La fecha de nacimiento no puede ser futura.

**H4.2 Buscar pacientes**
Como terapeuta quiero buscar pacientes por DNI, nombre o apellido para encontrarlos rápido desde el celular.
- La búsqueda es parcial y no distingue mayúsculas; el listado viene paginado.

**H4.3 Ver y editar paciente**
Como terapeuta quiero ver y corregir los datos de un paciente.
- Se pueden editar todos los campos salvo el DNI.
- La ficha muestra la lista de diagnósticos del paciente.

### É5 — Diagnósticos

**H5.1 Crear diagnóstico**
Como terapeuta quiero registrar un diagnóstico e indicar si hubo cirugía para que el sistema pida los datos que corresponden.
- La fecha del diagnóstico la asigna el sistema al crearlo.
- La descripción, en texto libre, es obligatoria.
- **Con cirugía:** pide fecha de cirugía, fecha de retiro de puntos (≥ fecha de cirugía) y tipo de cirugía (de los activos).
- **Sin cirugía:** pide fecha de inmovilización.
- El tipo de tratamiento no se puede cambiar después.

**H5.2 Ver diagnósticos**
Como terapeuta quiero ver los diagnósticos de un paciente, del más reciente al más antiguo, con su tratamiento.

### É6 — Seguimientos

**H6.1 Registrar seguimiento**
Como terapeuta quiero registrar un seguimiento de un diagnóstico para documentar la evolución.
- Pide la fecha del seguimiento.
- Si el diagnóstico es quirúrgico, el tipo de cicatriz es obligatorio (de los activos). Si es conservador, el campo no se muestra y se rechaza si viene.
- Requiere al menos una evaluación de segmento.
- Todo el seguimiento se guarda en una sola operación (transaccional).

**H6.2 Evaluar segmentos**
Como terapeuta quiero evaluar uno o varios segmentos en el mismo seguimiento.
- Por segmento: edema (decimal u omitido), dolor (entero de 0 a 10), ROM y fuerza (decimales, obligatorios).
- Fuera de rango, aparece un error por campo.

**H6.3 Responder plantilla**
Como terapeuta quiero elegir una plantilla del segmento y responder sus preguntas para registrar evaluaciones específicas.
- Solo se ofrecen las plantillas activas del segmento elegido.
- Las preguntas hijas aparecen u ocultan al instante según la respuesta del padre.
- El backend vuelve a validar: rechaza respuestas a preguntas inactivas y valores inválidos para su tipo o rango.

**H6.4 Historial de seguimientos**
Como terapeuta quiero ver los seguimientos de un diagnóstico, con sus evaluaciones y respuestas, para analizar la evolución.
- Orden cronológico descendente.
- Las respuestas muestran el texto de la pregunta, aunque la pregunta esté inactiva.

## 7. Orden de implementación sugerido

H0.1 → É1 → É2 → É4 → É5 → É3 → É6. Cada historia con pantallas queda bloqueada hasta tener su mockup.

## 8. Fuera de alcance (MVP)

Recuperar la contraseña por email, gráficos de evolución, reportes y exportación, turnos o agenda, adjuntos (imágenes o PDFs) y auditoría detallada de cambios.

## 9. Pruebas

- **Backend:**
  - Tests unitarios de cada patrón: tratamientos, condiciones, validadores y la cadena de validación.
  - Tests de integración con Testcontainers para cada endpoint, con los casos de error de cada criterio de aceptación.
- **Frontend:** tests de servicios, guards e interceptor, y del componente de formulario dinámico (mostrar u ocultar preguntas hijas).
- Cada criterio de aceptación tiene al menos un test.
