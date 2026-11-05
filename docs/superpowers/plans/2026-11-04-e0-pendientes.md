# Pendientes heredados de É0 (base técnica)

Decisiones y hallazgos de las revisiones de É0 que las épicas siguientes **tienen que** tomar en cuenta.

## Para el plan de É1 (acceso)
- **Errores de Spring Security con el formato uniforme:** — **resuelto en H1.1**
  - Agregar `@ExceptionHandler` para `AuthenticationException` → `AUTHENTICATION_FAILED` y `AccessDeniedException` → `ACCESS_DENIED`, para los errores que salen de `@PreAuthorize`/`@Secured`.
  - Hacer que `AuthenticationEntryPoint` y `AccessDeniedHandler` escriban un `ErrorResponse`.
  - Agregar tests para los dos casos.
  - Si no se hace, esos errores terminan en 500.
- **Permitir `/error`** en la cadena de seguridad para los despachos de error. Si no, los 401/403 no llegan a `ApiErrorController`. — **resuelto en H1.1**
- **Sumar un código propio para token vencido** (por ejemplo `TOKEN_EXPIRED`), distinto de `AUTHENTICATION_FAILED`. El mensaje de este último ("Email o contraseña incorrectos.") no sirve para un token vencido. — **resuelto en H1.1**
- **Resolver CORS:** `CORS_ALLOWED_ORIGINS` todavía no está conectado. Hay que configurarlo junto con Spring Security. — **resuelto en H1.1**
- **Rol de base de datos:** hoy la app se conecta como `postgres`. Evaluar un rol con privilegios mínimos.
- **Título de la app:** cambiar el `<title>` de `frontend/src/index.html` ("Frontend") por un título en español cuando lleguen las primeras pantallas. — **resuelto en H1.1**

## Antes de É4 (pacientes)
- **Política de logs sin datos personales:** el manejador global registra la causa y el stack trace de los errores inesperados, y los mensajes de Hibernate o de integridad pueden incluir valores de columnas. Definir cómo enmascarar esos datos antes de guardar datos de pacientes. — **resuelto en É4** (`SafeThrowableDescriber`; nginx sin query strings).
- **Imagen de Postgres en los tests:** los tests usan `postgres:15-alpine`. Pasarlos a la imagen de Supabase antes de que alguna migración use roles, esquemas o extensiones propias de Supabase.

## Antes de un despliegue real
- El contenedor del backend corre como root: agregar un usuario sin privilegios.
- Fijar los tags de las imágenes base de los Dockerfiles, que hoy son flotantes.
- Agregar healthchecks al backend y al frontend, y usar `depends_on: condition: service_healthy`.

## Menores
- La respuesta 405 no incluye el header `Allow`.
- Las subclases de `ApplicationException` aceptan cualquier `ErrorCode`, así que compila una combinación incoherente.
- Las validaciones de parámetros no devuelven `fieldErrors` por parámetro.
- `package.json` declara `"packageManager": "npm@10.6.0"`, pero el lockfile se generó con npm 11, porque npm 10.6 falla al instalar este proyecto.

## Entorno de desarrollo
- La máquina tiene JDK 17. Los tests del backend corren en el contenedor `maven:3.9-eclipse-temurin-21`, y Testcontainers usa el socket de Docker montado:
  ```bash
  cd backend && MSYS_NO_PATHCONV=1 docker run --rm -v //var/run/docker.sock:/var/run/docker.sock -v "C:/Users/Maxi/Desktop/TO Managment/backend:/app" -v to-management-m2:/root/.m2 -w /app -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal maven:3.9-eclipse-temurin-21 ./mvnw -B test
  ```
  Si se instala JDK 21, se puede usar `./mvnw test` directo.
- **Angular 21:** se usó Angular 21 y no 22, porque Node 22.22.2 está por debajo del mínimo que exige el CLI 22. Para pasar a 22: actualizar Node y correr `ng update`.

## Pendientes heredados de H1.1
- La respuesta de CORS rechazado es texto plano en vez de `ErrorResponse`.
- Agregar CSP y headers de seguridad en nginx antes de É4 (el JWT vive en localStorage).
- Auto-hospedar Manrope (Google Fonts recibe la IP en cada carga).
- Limitar intentos de login (p. ej. `limit_req` de nginx con valores en `.env`).
- En H1.4, tests para `JsonAccessDeniedHandler` y el handler de `AuthenticationException` con el primer endpoint `@PreAuthorize`.
- Un token firmado sin claim `role` da 500 (NPE) en vez de 401.

## Pendientes heredados de H1.4
- Revalidar el JWT contra la base (usuario activo y rol actual) en cada request. Hoy, desactivar a alguien o cambiarle el rol recién tiene efecto cuando vence su token. Ajustar `SecurityIntegrationTest`, que emite tokens de usuarios que no existen en la base.

## Pendientes heredados de É3 (para É6) — resueltos en É6
- Reemplazar `NoRecordedAnswersLookup` por una implementación de `QuestionAnswerLookup` que consulte la tabla de respuestas. Hasta entonces ninguna pregunta queda bloqueada.
- Usar `ActivationConditionFactory` / `AnswerValue` para aceptar solo respuestas a preguntas activas, y replicar la misma lógica en el formulario del seguimiento.
- Definir qué pasa con las respuestas de las hijas cuando cambia la respuesta del padre. Propuesta: conservarlas mientras la pantalla esté abierta y descartar las ocultas al guardar.

## Pendientes heredados de É5 (para É6) — resueltos en É6
- Sumar el conteo de seguimientos a las tarjetas de diagnóstico ("Creado el … · N seguimientos") y el botón "Nuevo seguimiento" en el detalle.
- Usar `Treatment.requiresScarType()` para exigir u omitir el tipo de cicatriz del seguimiento.

## Pendientes de É6
- Los rangos clínicos (`ClinicalRange`: edema 0–50, dolor 0–10, ROM 0–360, fuerza 0–100) son una propuesta: confirmarlos con la terapeuta.
- No hay edición ni borrado de seguimientos (no están en el spec ni en los mockups).

## Pendientes de É7 (Agenda)
- Fuera de alcance por decisión del spec: horarios por terapeuta, estados del turno, recordatorios, turnos recurrentes, historial de cancelados y feriados.
- Accesibilidad: el panel de reserva de escritorio no tiene semántica de diálogo (Escape, foco); al confirmar la cancelación en una card se pierde el foco; las fechas de la tira semanal y del mini calendario se anuncian en formato ISO.
- El formulario de reserva móvil usa un select de duración (el mockup propone un control segmentado 30'/45'/60') y no agrupa en tarjetas ni muestra el resumen oscuro del mockup.
- "Hoy" se toma del navegador (la clínica tiene una sola zona horaria) y no se actualiza si la página queda abierta pasada la medianoche.
- Errores de búsqueda de pacientes y de carga de diagnósticos en el formulario no muestran mensaje.
- Endurecimiento del backend sin efecto visible: mapear la violación de restricción con `getMostSpecificCause()`, `forRole` sin política registrada, tests extra de bordes (paciente superpuesto entre terapeutas, 45 min al cierre).
