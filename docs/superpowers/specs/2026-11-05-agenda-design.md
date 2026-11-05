# É7 — Agenda de turnos (diseño)

Fecha: 2026-11-05. Mockups: `docs/mockups/h7.1-agenda/` (calendario, turnos asignados y reserva; mobile y escritorio).
Amplía el spec general `2026-11-04-gestion-terapia-ocupacional-design.md`, que tenía "turnos o agenda" fuera de alcance.

## 1. Objetivo

Que cada terapeuta vea y asigne sus turnos, y que un admin vea y gestione los de todo el equipo, con una agenda al estilo
Google Calendar desde el celular o la computadora.

**Éxito:** reservar un turno libre en pocos toques, ver la semana de un vistazo y que nunca queden dos turnos
superpuestos para el mismo terapeuta ni para el mismo paciente.

## 2. Decisiones acordadas

| Tema | Decisión |
|---|---|
| Quién tiene turnos | Cualquier usuario activo (rol `TERAPEUTA` o `ADMIN`): un admin también atiende y tiene agenda propia. |
| Visibilidad | `ADMIN` ve los turnos de todos; `TERAPEUTA` ve solo los suyos. |
| Permisos | `TERAPEUTA` reserva, reprograma y cancela solo sus turnos (el terapeuta queda fijo en él). `ADMIN` lo hace para cualquiera. |
| Estados | No hay estados: todo turno guardado está vigente. Los badges "Confirmado / A confirmar" del mockup no se implementan. |
| Cancelar | Pide confirmación y **borra** el turno; el horario queda libre. |
| Horario | Fijo para todos: lunes a sábado, 08:00 a 20:00, inicio cada 30 min. Duración 30, 45 o 60 min; el turno termina a las 20:00 como máximo. |
| Superposición | Un terapeuta no puede tener dos turnos superpuestos; un paciente tampoco (aunque sean con terapeutas distintos). |
| Pasado | No se reservan ni se reprograman turnos que empiecen antes de "ahora" (`APP_TIME_ZONE`). |
| Enfoque | Calendario con componentes Angular propios (fieles a los mockups); el backend calcula la disponibilidad y valida todas las reglas. Sin librerías de calendario. |

## 3. Historias

**H7.1 Ver la agenda (calendario)**
Como terapeuta quiero ver mis turnos en un calendario para organizar mi semana; como admin, los de todo el equipo.
- Escritorio: semana lunes–domingo, 08:00–20:00, línea de la hora actual, mini calendario del mes, "Hoy" y ‹ ›.
- Celular: tira semanal (puntos de color por terapeuta con turnos ese día) y vista del día por hora.
- Los turnos superpuestos (de distintos terapeutas) se dividen en columnas.
- Admin: filtro "Terapeutas" con checkboxes y cantidad de turnos de cada uno en la semana visible. El terapeuta no ve el filtro.

**H7.2 Ver turnos asignados (cards)**
- Filtro Hoy / Esta semana; cards agrupadas por día ("Hoy · jueves 5 de noviembre · 6 turnos"), 3 columnas en escritorio.
- Card: horario (inicio "a" fin), paciente, diagnóstico (si tiene), chip del terapeuta; acciones Reprogramar, Ver ficha
  (ficha del paciente) y Cancelar.

**H7.3 Reservar turno**
- Paciente (buscador por nombre o DNI), diagnóstico opcional (de ese paciente), terapeuta, fecha, duración, horario,
  notas (opcional, hasta 500 caracteres) y resumen.
- La grilla de horarios muestra los libres y tacha/deshabilita los ocupados para el terapeuta elegido y los que chocan
  con otro turno del paciente.

**H7.4 Reprogramar y cancelar**
- Reprogramar abre el mismo formulario con los datos del turno; el paciente no cambia. Se pueden cambiar fecha, hora,
  duración, diagnóstico, notas y, solo admin, terapeuta.
- Cancelar pide confirmación y borra el turno.

## 4. Modelo de datos

Migración Flyway **V9**, reflejada en `supabase/init/01_schema.sql`.

```
appointment
  id             BIGSERIAL PK
  therapist_id   BIGINT    NOT NULL → app_user(id)
  patient_id     BIGINT    NOT NULL → patient(id)
  diagnosis_id   BIGINT        NULL → diagnosis(id)
  start_at       TIMESTAMP NOT NULL   -- hora local de la clínica (APP_TIME_ZONE)
  end_at         TIMESTAMP NOT NULL   -- CHECK end_at > start_at
  notes          VARCHAR(500)  NULL
  created_at     TIMESTAMPTZ NOT NULL
  EXCLUDE USING gist (therapist_id WITH =, tsrange(start_at, end_at) WITH &&)
  EXCLUDE USING gist (patient_id   WITH =, tsrange(start_at, end_at) WITH &&)
  índice (start_at)
```

- Requiere la extensión `btree_gist` (`CREATE EXTENSION IF NOT EXISTS btree_gist`), disponible en Supabase y en la
  imagen de Testcontainers. Las restricciones garantizan la regla aun con dos reservas simultáneas; el backend valida
  antes para devolver un mensaje claro y traduce la violación de la restricción a `THERAPIST_BUSY` / `PATIENT_BUSY`.
- `tsrange` es semiabierto (`[inicio, fin)`): un turno que termina 10:00 no choca con uno que empieza 10:00.
- `diagnosis_id`, si viene, tiene que pertenecer a `patient_id` (validado en el backend).

## 5. Backend (paquete `appointment`)

**Enums**
- `AppointmentDuration`: `MINUTES_30`, `MINUTES_45`, `MINUTES_60`.
- `AgendaSchedule`: apertura 08:00, cierre 20:00, paso de 30 min, días hábiles lunes a sábado.
- `TherapistColor`: los 4 colores del README de mockups; cada terapeuta recibe `id % 4` (estable).

**Patrones**
- **Strategy** — `AppointmentVisibilityPolicy` por rol: `AdminVisibilityPolicy` (todo) y `TherapistVisibilityPolicy`
  (solo propios; gestionar turnos de otro → `ACCESS_DENIED`). Se elige con una factory por `Role`.
- **Chain of Responsibility** — `AppointmentValidationChain`: horario permitido y no pasado → permiso del rol →
  terapeuta activo → diagnóstico del paciente → terapeuta libre → paciente libre.
- **Specification** — consulta por rango y terapeutas visibles.
- Servicios con interfaz e implementación: `AppointmentService` (consultar, reservar, reprogramar, cancelar) y
  `AvailabilityService` (horarios de un día).

**API** (todo autenticado; DTOs con `jakarta.validation`)

| Método y ruta | Descripción |
|---|---|
| `GET /api/appointments?from=YYYY-MM-DD&to=YYYY-MM-DD&therapistIds=1,2` | Turnos que empiezan en el rango (inclusive), ordenados por inicio, filtrados por la política del rol. `therapistIds` opcional: para un terapeuta se ignora todo id que no sea el suyo. Rango máximo 42 días. |
| `GET /api/appointments/availability?therapistId=&patientId=&date=&durationMinutes=&excludeAppointmentId=` | Todos los inicios del día (08:00 a cierre − duración, cada 30 min) con `available` y, si no, el motivo (`PAST`, `THERAPIST_BUSY`, `PATIENT_BUSY`). Domingo → lista vacía. `excludeAppointmentId` para reprogramar. |
| `POST /api/appointments` | Reserva: `therapistId`, `patientId`, `diagnosisId?`, `date`, `startTime`, `durationMinutes`, `notes?`. 201 con el turno. |
| `PUT /api/appointments/{id}` | Reprograma: `therapistId`, `diagnosisId?`, `date`, `startTime`, `durationMinutes`, `notes?` (el paciente no cambia). |
| `DELETE /api/appointments/{id}` | Cancela (borra). 204. |
| `GET /api/agenda/therapists` | Usuarios que pueden tener turnos: `id`, `fullName`, `color`, `active`. Admin: todos (activos primero); terapeuta: solo él. |

Respuesta de un turno: `id`, `therapist {id, fullName, color}`, `patient {id, firstName, lastName, dni}`,
`diagnosis {id, description} | null`, `date`, `startTime`, `endTime`, `durationMinutes`, `notes`.

**Errores nuevos (`ErrorCode`)**
`APPOINTMENT_NOT_FOUND` (404), `APPOINTMENT_OUTSIDE_HOURS` (422), `APPOINTMENT_IN_PAST` (422),
`APPOINTMENT_RANGE_TOO_LONG` (422), `THERAPIST_BUSY` (409), `PATIENT_BUSY` (409), `DIAGNOSIS_NOT_OF_PATIENT` (422),
`THERAPIST_INACTIVE` (422). Gestionar un turno ajeno siendo terapeuta → `ACCESS_DENIED` (403).

**Privacidad:** no se loguean datos de pacientes ni las notas; los DTOs enmascaran `toString`.

## 6. Frontend (`features/agenda`)

- Ruta `/agenda` (lazy) con `?vista=calendario|turnos`; reserva en celular `/agenda/nuevo` y
  `/agenda/:id/reprogramar`; en escritorio, panel lateral sobre la agenda.
- Navegación: "Agenda" en la barra inferior (Pacientes · Agenda · Plantillas · Más) y en el menú lateral, debajo de
  Pacientes.
- Pestañas: control segmentado accesible (`role="tablist"`, flechas del teclado), según el mockup.
- Subtítulo: admin "Turnos de todo el equipo."; terapeuta "Tus turnos."
- Componentes: `AgendaPage`, `AgendaTabs`, `WeekCalendar` (escritorio), `DayTimeline` + `WeekStrip` (celular),
  `MiniMonth`, `TherapistFilter` (solo admin), `AppointmentCard`, `AppointmentForm` (panel y pantalla), `SlotGrid`.
- Lógica pura y testeable aparte: posición vertical por hora (64 px/h celular, 52 px/h escritorio), columnas para
  turnos superpuestos, agrupación por día, fechas de la semana.
- Al guardar o cancelar se recargan los turnos del rango visible. Si el backend responde `THERAPIST_BUSY` o
  `PATIENT_BUSY`, se muestra el mensaje y se refresca la grilla de horarios.
- Mobile-first; la vista semanal y el panel lateral aparecen desde 1024 px.

## 7. Pruebas

- Backend: integración con Testcontainers (visibilidad por rol, permisos, superposición de terapeuta y de paciente,
  restricción de la base con reservas simultáneas, horario, domingo, pasado, diagnóstico de otro paciente, rango
  máximo) y unitarias de la generación de horarios y de cada eslabón de la cadena.
- Frontend: specs de columnas para superposición, agrupación por día, pestañas, filtro por rol, grilla de horarios y
  formulario de reserva.
- Verificación: se escriben los tests y se compila backend y frontend (instrucción vigente del usuario).

## 8. Flujo de ramas

`feat/agenda` sale de `main` → implementación → merge a `test` para la prueba manual → con aprobación, merge de
`feat/agenda` a `main` (nunca `test` a `main`).

## 9. Fuera de alcance

Horarios por terapeuta, estados del turno, recordatorios o notificaciones, turnos recurrentes, historial de
cancelados y bloqueo de feriados.
