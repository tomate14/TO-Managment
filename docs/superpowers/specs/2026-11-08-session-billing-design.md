# É12 — Facturación de sesiones por diagnóstico: diseño

Fecha: 2026-11-08 · Rama: `feat/session-billing` (desde `main`) · Depende de: É5 diagnósticos, É6 seguimientos, É8 multi-tenencia

## 1. Objetivo

La terapeuta factura las sesiones de un diagnóstico por grupos y necesita saber cuáles ya facturó y cuáles no. Con este cambio, cada diagnóstico muestra cuántas sesiones tiene, cuántas están facturadas y cuántas pendientes. La terapeuta registra cada facturación con las sesiones que incluye, su fecha y su monto, y puede anularla si se equivocó.

Éxito:
- En el diagnóstico se ve de un vistazo "12 sesiones registradas · 8 facturadas · 4 pendientes".
- Registrar una facturación de las pendientes lleva un toque más la fecha y el monto.
- Una facturación anulada devuelve sus sesiones a pendientes sin perder el historial.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Qué es una sesión | Cada **seguimiento** del diagnóstico (É6). |
| Datos de una facturación | Las sesiones incluidas, la **fecha de facturación** y el **monto** en pesos. |
| Selección por defecto | Todas las sesiones pendientes, ordenadas de la más antigua a la más nueva. |
| Fecha | Por defecto hoy; se puede cambiar por una anterior; nunca futura. |
| Anulación | Se anula la facturación completa; sus sesiones vuelven a pendientes; el registro queda visible como "Anulada". |
| Permisos | Cualquier usuario del establecimiento (ADMIN o TERAPEUTA), igual que los diagnósticos y los seguimientos. |

## 3. Historias

- **H12.1 Resumen de facturación.**
  - En el detalle del diagnóstico hay una sección "Facturación" con:
    - el resumen: "12 sesiones registradas", "8 facturadas · 4 pendientes";
    - el botón "Registrar facturación", visible solo si hay pendientes.
  - Sin sesiones se muestra "Todavía no hay sesiones para facturar."
  - La tarjeta del diagnóstico, en la lista del paciente, muestra "4 sesiones pendientes de facturar" cuando hay pendientes.
- **H12.2 Registrar facturación.**
  - "Registrar facturación" abre el formulario: una página en móvil y un panel en escritorio. Tiene:
    - la lista de sesiones pendientes (fecha del seguimiento y segmentos evaluados), con casillas y todas marcadas al abrir;
    - "Fecha de facturación", por defecto hoy;
    - "Monto" en pesos.
  - Botón "Registrar facturación".
  - Validaciones:
    - "Elegí al menos una sesión.";
    - "Ingresá el monto." / "El monto tiene que ser mayor a cero.";
    - "La fecha no puede ser futura.".
- **H12.3 Facturaciones anteriores.**
  - Debajo del resumen, la lista "Facturaciones anteriores", de la más reciente a la más vieja. Cada una dice "8 sesiones · $ 40.000,00 · Facturado el 2 de noviembre de 2026".
  - "Ver sesiones" despliega las fechas de los seguimientos incluidos.
  - "Anular" pide confirmación en línea: "¿Anular esta facturación? Sus sesiones vuelven a quedar pendientes." con "Sí, anular" y "Cancelar".
  - Una anulada se muestra atenuada con la marca "Anulada", sin el botón "Anular".
- **H12.4 Marca en el seguimiento.** La tarjeta de un seguimiento incluido en una facturación vigente muestra la marca "Facturada".

## 4. Modelo de datos

Migración `V14__session_billing.sql`, con su reflejo en el mismo cambio en `supabase/init/01_schema.sql` (idempotente):

- `billing`:
  - `id bigserial`;
  - `establishment_id bigint not null references establishment` (tenant);
  - `diagnosis_id bigint not null references diagnosis`;
  - `billed_on date not null`;
  - `amount numeric(12,2) not null check (amount > 0)`;
  - `created_at timestamptz not null`;
  - `voided_at timestamptz null`;
  - un índice sobre `(diagnosis_id, billed_on desc)`.
- `billing_follow_up`:
  - `establishment_id` (tenant);
  - `billing_id references billing`;
  - `follow_up_id references follow_up`;
  - `active boolean not null default true`;
  - clave primaria `(billing_id, follow_up_id)`.
  - Un **índice único parcial** sobre `follow_up_id WHERE active` impide que un seguimiento esté en dos facturaciones vigentes. Al anular se pone `active = false` en los vínculos de esa facturación.
- Estados derivados:
  - una sesión está facturada si tiene un vínculo activo; si no, está pendiente;
  - una facturación está vigente si `voided_at IS NULL`.
- No hay datos iniciales.

## 5. Backend

- **Paquete nuevo `billing`**, por feature: `controller`, `service` (interfaz + `Impl`), `repository`, `dto`, `entity`. Entidades `Billing` y `BillingFollowUp`, con `@TenantId`.
- **Endpoints** (cualquier usuario autenticado del establecimiento):
  - `GET /api/diagnoses/{diagnosisId}/billing` devuelve el resumen:
    - `{ totalSessions, billedSessions, pendingSessions, pending: [FollowUpRef], billings: [BillingResponse] }`;
    - `FollowUpRef` es `{ id, date, segments: string[] }`;
    - `BillingResponse` es `{ id, billedOn, amount, sessionCount, voided, voidedAt, createdAt, sessions: [FollowUpRef] }`.
  - `POST /api/diagnoses/{diagnosisId}/billings` recibe `{ billedOn, amount, followUpIds: number[] }` y responde 201 con el resumen actualizado.
  - `POST /api/billings/{id}/void` responde con el resumen actualizado. Anular dos veces es idempotente.
- **Validaciones (`ErrorCode`):**
  - `BILLING_SESSIONS_REQUIRED` (422, "Elegí al menos una sesión.");
  - `BILLING_AMOUNT_INVALID` (422, "El monto tiene que ser mayor a cero."), además de `@NotNull` con "Ingresá el monto.";
  - `BILLING_DATE_IN_FUTURE` (422, "La fecha no puede ser futura."), comparada con `APP_TIME_ZONE` mediante el `Clock`;
  - `BILLING_SESSION_INVALID` (422, "Hay sesiones que no son de este diagnóstico o ya están facturadas."): seguimiento de otro diagnóstico, repetido o con vínculo activo. La carrera con el índice único se traduce al mismo código;
  - `BILLING_NOT_FOUND` (404, "La facturación no existe.").
- **Seguimientos:** la respuesta de cada seguimiento suma `billed: boolean`.
- **Diagnóstico:** la respuesta suma `pendingBillingCount`, para la tarjeta de la lista.
- El monto nunca se loguea.

## 6. Frontend

- **Detalle del diagnóstico** (`diagnosis-page`): sección "Facturación" con el resumen, el botón y las facturaciones anteriores (H12.1 y H12.3). El componente es nuevo: `billing-section`.
- **Formulario** (`billing-form`): el componente se reutiliza como página en móvil (`/pacientes/:id/diagnosticos/:diagnosisId/facturacion/nueva`) y como panel lateral en escritorio, con el patrón de `EstablishmentsPage`. Usa formularios reactivos.
  - El monto acepta coma o punto decimal (como `parseDecimal`) y se muestra con formato es-AR.
- **Tarjetas:** la del diagnóstico muestra "n sesiones pendientes de facturar"; la del seguimiento, la marca "Facturada".
- **Mockups** (requisito para codificar) en `docs/mockups/h12-facturacion/`, en móvil y escritorio:
  - la sección Facturación con pendientes y facturaciones anteriores (una desplegada y una anulada);
  - el formulario de registro, con un error de validación;
  - la confirmación de anulación;
  - las tarjetas con sus marcas.

## 7. Pruebas

Se escriben los tests; la verificación es solo de compilación (instrucción vigente), más una corrida única de las clases de integración clave antes del merge a `test`.

- **Backend:**
  - el resumen cuenta bien total, facturadas y pendientes;
  - registrar con las sesiones pendientes da 201 y deja esas sesiones como facturadas;
  - sin sesiones → 422; monto 0 o negativo → 422; fecha futura → 422;
  - un seguimiento de otro diagnóstico o ya facturado → 422 `BILLING_SESSION_INVALID`;
  - anular devuelve las sesiones a pendientes, la facturación queda `voided`, y anular otra vez es idempotente;
  - una sesión de una facturación anulada se puede volver a facturar;
  - aislamiento entre establecimientos (se extiende `CrossEstablishmentIsolationTest`).
- **Frontend:**
  - el resumen y sus textos;
  - el formulario con todas las sesiones marcadas por defecto y las validaciones;
  - el error del servidor;
  - "Ver sesiones" y la anulación con confirmación;
  - las marcas en las tarjetas.

## 8. Flujo de ramas y despliegue

Rama desde `main` → merge a `test` → prueba manual → merge de la feat a `main` (nunca `test`). V14 no borra datos y no hay variables de entorno nuevas. El despliegue a producción usa los comandos de `docs/cloud-run.md` (backend y frontend).

## 9. Fuera de alcance

Emitir facturas o PDF, integración con ARCA/AFIP, precio por sesión o tarifas, obra social o pagador, reportes de facturación entre pacientes o por período, y editar una facturación (se anula y se registra de nuevo).
