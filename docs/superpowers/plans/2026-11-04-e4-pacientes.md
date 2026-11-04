# É4 Pacientes (H4.1–H4.3) — plan

**Historias:** spec §6 É4 (H4.1 alta, H4.2 búsqueda paginada, H4.3 ver y editar). Modelo: spec §3.3.
**Mockups:**
- `h4.1-alta-paciente/mobile.png` (`M-PatientNew.dc.html`): DNI duplicado. Editar es el mismo formulario con el DNI bloqueado.
- `h4.2-buscar-pacientes/mobile.png` y `desktop.png` (`M-Patients`, `D-Patients`).
- `h4.3-ficha-paciente/mobile.png` y `desktop.png` (`M-Patient`, `D-Patient`).

## Pendientes de É0 que se resuelven acá (antes de guardar datos de pacientes)
- **Logs sin datos personales:**
  - El handler global deja de loguear el mensaje y el stack trace crudo de los errores inesperados, porque los mensajes de Postgres o Hibernate pueden traer valores como el DNI.
  - `SafeThrowableDescriber` loguea solo la cadena de tipos de excepción y el primer frame del código de la app.
- **nginx:** el access log deja de registrar los query strings (`?q=` puede ser un DNI). Se usa un `log_format` con `$uri`.

## Decisiones (lo que los mockups no resuelven)
- **DNI:** se guarda solo con dígitos (se sacan puntos y espacios), entre 6 y 9. Se muestra con puntos (30.123.456).
- **Dominancia:** "Diestro" / "Zurdo" como dice el spec. El mockup alterna "Diestra/Diestro" según la persona, pero el modelo no tiene género.
- **Búsqueda:**
  - Cada palabra tiene que aparecer en el DNI, el nombre o el apellido (parcial, sin mayúsculas). Así "lucía fernández" también encuentra.
  - Orden: apellido y nombre.
  - Tamaño de página: 20 (máximo 50).
  - **Celular:** "Cargar más" agrega la página siguiente.
  - **Escritorio:** "Anterior" / "Siguiente" con "Página N".
- **Diagnósticos en la ficha:** É5 todavía no existe. La sección muestra "Todavía no hay diagnósticos.", sin botón "Nuevo" (llega con É5), y en escritorio sin el panel de seguimientos.

## Base de datos (`V6__create_patients.sql` + `01_schema.sql`)
- `patient`: `id`, `dni` VARCHAR(12) único, `first_name`, `last_name`, `birth_date`, `phone`, `occupation`, `health_insurance`, `health_insurance_number`, `dominance` (CHECK `LEFT`/`RIGHT`), `created_at`.

## Backend (paquete `patient`)
- `Dominance` (enum).
- `Patient` (Builder; `update(...)` cambia todo salvo el DNI).
- **Specification:** `PatientSearchSpecification` (JPA `Specification`) para la búsqueda por palabras.
- `PatientService` + `Impl`.
- `PageResponse<T>` (común).
- `PaginationLimit` (enum: 20 por defecto, 50 máximo).
- API (autenticada):
  - `GET /api/patients?q=&page=&size=`
  - `POST /api/patients` (201)
  - `GET /api/patients/{id}`
  - `PUT /api/patients/{id}` (sin DNI)
- `ErrorCode`: `PATIENT_NOT_FOUND` (404) y `PATIENT_DNI_DUPLICATE` (409, "Ya existe un paciente con este DNI.").
- Validación:
  - Todos los campos obligatorios.
  - DNI con 6 a 9 dígitos.
  - Fecha de nacimiento que no sea futura (`@PastOrPresent`).

## Frontend
- `/pacientes` (`PatientsPage`):
  - **Celular:** buscador con debounce, contador ("Ordenados por apellido" / "N resultados"), filas con iniciales, "Apellido, Nombre" y "DNI · Obra social", "Cargar más", estado vacío y botón flotante.
  - **Escritorio:** título y subtítulo, tabla, paginación y "Nuevo paciente". El layout se elige con `matchMedia('(min-width: 1024px)')`.
- `/pacientes/nuevo` y `/pacientes/:id/editar` (`PatientFormPage`):
  - "Datos personales", "Cobertura" y dominancia segmentada.
  - El DNI duplicado se muestra bajo el campo.
  - Al editar, el DNI queda bloqueado.
- `/pacientes/:id` (`PatientPage`):
  - Encabezado oscuro con iniciales, nombre y "DNI · edad · dominancia", y la lista de datos (con link `tel:` al teléfono).
  - Sección "Diagnósticos".
  - En escritorio: dos columnas, breadcrumb y "Editar datos".
- Se elimina `EmptySection`, que queda sin uso.

## Verificación
Se escriben tests (describer, servicio, controller con Testcontainers, componentes), pero solo se verifica que compile. `supabase/init` se corre contra un Postgres vacío.
