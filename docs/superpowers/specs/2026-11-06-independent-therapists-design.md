# É9 — Terapeutas independientes: diseño

Fecha: 2026-11-06 · Rama: `feat/independent-therapists` (desde `main`, con É8 incluido) · Depende de: [É8 multi-tenencia](2026-11-06-multi-tenant-design.md)

## 1. Objetivo

Hoy todo usuario pertenece a un establecimiento creado por el superadmin. Con este cambio, una terapeuta **sin establecimiento** puede **anotarse sola** desde el login y trabajar en su **consultorio propio**: sus pacientes, catálogos, plantillas, diagnósticos, seguimientos y agenda, invisibles para cualquier otro establecimiento.

Éxito:
- Una terapeuta completa el registro en menos de un minuto sin ayuda.
- No puede ingresar hasta que el superadmin la aprueba.
- Una vez aprobada, trabaja igual que el admin de un establecimiento, sin ver datos ajenos.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Alcance | Consultorio propio. No se une después a otro establecimiento ni lleva sus pacientes. |
| Activación | Pendiente hasta que el superadmin la aprueba. No se envían emails. |
| Modelo | El consultorio propio es un **establecimiento de una persona** de tipo `INDIVIDUAL`. El aislamiento de É8 (`@TenantId`, resolver, interceptor, JWT) no cambia. |
| Rol | La terapeuta es `ADMIN` de su consultorio (administra catálogos y plantillas). La UI la llama "Terapeuta independiente". |
| Nombre del consultorio | Al registrarse se usa el nombre completo de la terapeuta; puede renombrarlo desde "Mi consultorio". Si ese nombre ya existe (es único global ignorando mayúsculas), se le agrega un sufijo numérico: "Ana Pérez (2)". |
| Catálogos | Se copian los valores por defecto al registrarse (mismo sembrador que É8). |
| Rechazo | Rechazar = desactivar el establecimiento con el endpoint existente. |

## 3. Historias

- **H9.1 Registro.** En el login, un link "¿Sos terapeuta independiente? Creá tu cuenta" lleva a `/registro`. El formulario pide nombre completo, email, contraseña y su confirmación. Al enviar, se ve "Recibimos tu solicitud" con el texto "Vas a poder ingresar cuando el administrador de la plataforma la apruebe."
- **H9.2 Login pendiente.** Si una terapeuta pendiente intenta ingresar, el login muestra "Tu cuenta está pendiente de aprobación" y "Vas a poder ingresar cuando el administrador de la plataforma la apruebe."
- **H9.3 Aprobación.** En Establecimientos, el superadmin ve primero la sección "Pendientes de aprobación" (nombre, email, fecha de solicitud) con los botones "Aprobar" y "Rechazar". Los consultorios individuales llevan el badge "Independiente". Una solicitud rechazada pasa a la lista general como inactiva con la etiqueta "Rechazada".
- **H9.4 Mi consultorio.** Para una terapeuta independiente, el menú lateral y "Más" muestran "Mi consultorio" en lugar de "Establecimiento" y ocultan "Usuarios". La pantalla es la misma de É8 (renombrar, datos).

## 4. Modelo de datos

Migración `V11__independent_therapists.sql` y reflejo en `supabase/init/01_schema.sql` (idempotente):
- `establishment.kind varchar(20) NOT NULL DEFAULT 'INSTITUTION'` con `CHECK (kind IN ('INSTITUTION','INDIVIDUAL'))`.
- `establishment.approved_at timestamptz NULL`, con `UPDATE establishment SET approved_at = created_at` para los existentes.
- Estados derivados:
  - pendiente: `approved_at IS NULL AND active`;
  - rechazado: `approved_at IS NULL AND NOT active`;
  - aprobado: `approved_at IS NOT NULL`.

Los establecimientos que crea el superadmin nacen aprobados (`approved_at = now()`).

## 5. Backend

- **Enums:** `EstablishmentKind { INSTITUTION, INDIVIDUAL }`. `ErrorCode.ESTABLISHMENT_PENDING_APPROVAL` (403, "Tu cuenta está pendiente de aprobación."). `ErrorCode.TOO_MANY_REQUESTS` (429, "Demasiados intentos. Probá de nuevo más tarde.").
- **Entidad `Establishment`:** campos `kind` y `approvedAt`, y los métodos `approve(Instant)` e `isPendingApproval()`.
- **Creación compartida:** se extrae de `EstablishmentServiceImpl.create` un componente con la creación de establecimiento, admin y catálogos. Lo usan el alta del superadmin (`INSTITUTION`, aprobado) y el registro (`INDIVIDUAL`, pendiente). El componente recibe una especificación inmutable con el tipo y si nace aprobado (Builder o record).
- **Registro:**
  - `POST /api/auth/register` (nuevo valor `REGISTER` en `PublicEndpoint`) con `RegisterTherapistRequest { fullName, email, password }`.
  - Validaciones iguales a `EstablishmentAdminRequest`: email normalizado, password `@NotNull` y de 8 a 72 caracteres; `toString` enmascarado.
  - Servicio `TherapistRegistrationService` / `Impl`. Responde 201 sin cuerpo sensible ni token.
  - Email existente → 409 `USER_EMAIL_TAKEN`.
- **Login:** `AuthServiceImpl`, junto al chequeo de inactivo. El chequeo de **inactivo va primero**, así una solicitud rechazada ve "inactivo". Después, si el establecimiento está pendiente → 403 `ESTABLISHMENT_PENDING_APPROVAL`.
- **Superadmin:**
  - `EstablishmentSummaryResponse` suma `kind`, `pendingApproval`, `rejected` y `createdAt`.
  - Nuevo `POST /api/establishments/{id}/approval` (SUPERADMIN): aprueba y además activa. Aprobar un establecimiento ya aprobado es idempotente.
  - La lista ordena primero los pendientes.
- **`UserSummary.establishment`** (`EstablishmentRef`) suma `kind`, para que el frontend decida "Mi consultorio".
- **Límite de intentos:** como el endpoint es público, se limita por IP. La IP sale del primer valor de `X-Forwarded-For` si existe (Cloud Run); si no, de la dirección remota.
  - Usa una ventana fija en memoria por instancia: suficiente porque la aprobación manual es el control real.
  - El máximo por hora viene de la nueva variable `REGISTRATION_MAX_PER_HOUR`, agregada a `.env` y `.env.example`.
  - Superado el máximo → 429 `TOO_MANY_REQUESTS`.
  - Es un componente propio (`RegistrationRateLimiter`, interfaz + implementación) con un reloj inyectable para los tests.
  - Nunca se loguean ni el email ni el nombre.

## 6. Frontend

- **Login:** debajo del botón, el link a `/registro`. `classify()` suma el tipo `'pending'` con los textos de H9.2. El 429 lo maneja solo la pantalla de registro, porque el login no tiene límite de intentos.
- **`/registro`** (ruta pública, lazy):
  - Formulario reactivo: nombre (máx. 120), email (válido, máx. 254), contraseña (8–72) y confirmación (tiene que coincidir).
  - `USER_EMAIL_TAKEN` se muestra en el campo email: "Ya hay una cuenta con este email."
  - Al terminar, se ve la confirmación de H9.1 con un botón "Volver al inicio de sesión".
- **Establecimientos:**
  - Sección "Pendientes de aprobación" arriba de la lista. Si no hay pendientes, la sección no se muestra.
  - "Aprobar" y "Rechazar"; este último pide confirmación en línea.
  - Badges "Independiente" y "Rechazada".
- **Navegación:**
  - Si `establishment.kind === 'INDIVIDUAL'`, el texto pasa a "Mi consultorio" y se oculta "Usuarios" en el menú lateral y en "Más".
  - La ruta `/usuarios` no se bloquea: el backend lo permite y no hay riesgo, solo se oculta.
- **Mockups** (requisito para codificar) en `docs/mockups/h9-independientes/`, mobile y desktop:
  - registro;
  - solicitud enviada;
  - login pendiente;
  - Establecimientos con pendientes;
  - menú de una independiente.

## 7. Pruebas

Se escriben los tests; la verificación es compile-only (instrucción vigente).
- Integración, registro: crea un establecimiento `INDIVIDUAL` pendiente, un usuario ADMIN y los 4 + 4 + 5 catálogos. Email duplicado da 409. Nombre repetido recibe sufijo.
- Login: pendiente da 403 `ESTABLISHMENT_PENDING_APPROVAL`; rechazada da 403 `ESTABLISHMENT_INACTIVE`; aprobada da 200.
- Aprobación: solo SUPERADMIN (ADMIN recibe 403); es idempotente.
- Aislamiento: se extiende `CrossEstablishmentIsolationTest` con una independiente aprobada que no ve nada de A ni de B.
- Límite: el intento siguiente al máximo da 429 y después de la ventana vuelve a permitir (reloj fijo).
- Frontend:
  - registro: validación, coincidencia de contraseñas, error de email, pantalla de confirmación;
  - login: tipo `'pending'`;
  - lista: sección de pendientes, aprobar y rechazar;
  - navegación: "Mi consultorio" y "Usuarios" oculto.

## 8. Flujo de ramas y despliegue

Rama desde `main` → merge a `test` → prueba manual → merge de la feat a `main` (nunca `test`). V11 no borra datos. Producción necesita la variable nueva `REGISTRATION_MAX_PER_HOUR` en Cloud Run.

## 9. Fuera de alcance

Emails (verificación y avisos), unirse luego a un establecimiento o transferir pacientes, convertir un consultorio individual en institución, recuperar contraseña, y un límite de intentos distribuido entre instancias.

El límite de intentos se puede esquivar llamando directo a la URL de Cloud Run con un `X-Forwarded-For` falsificado; restringir el ingreso al proxy queda como endurecimiento posterior, y la aprobación manual sigue siendo el control.
