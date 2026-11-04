# Pendientes heredados de É0 (base técnica)

Decisiones y hallazgos de las revisiones de É0 que las épicas siguientes **tienen que** tomar en cuenta.

## Para el plan de É1 (acceso)
- **Errores de Spring Security con el formato uniforme:**
  - Agregar `@ExceptionHandler` para `AuthenticationException` → `AUTHENTICATION_FAILED` y `AccessDeniedException` → `ACCESS_DENIED`, para los errores que salen de `@PreAuthorize`/`@Secured`.
  - Hacer que `AuthenticationEntryPoint` y `AccessDeniedHandler` escriban un `ErrorResponse`.
  - Agregar tests para los dos casos.
  - Si no se hace, esos errores terminan en 500.
- **Permitir `/error`** en la cadena de seguridad para los despachos de error. Si no, los 401/403 no llegan a `ApiErrorController`.
- **Sumar un código propio para token vencido** (por ejemplo `TOKEN_EXPIRED`), distinto de `AUTHENTICATION_FAILED`. El mensaje de este último ("Email o contraseña incorrectos.") no sirve para un token vencido.
- **Resolver CORS:** `CORS_ALLOWED_ORIGINS` todavía no está conectado. Hay que configurarlo junto con Spring Security.
- **Rol de base de datos:** hoy la app se conecta como `postgres`. Evaluar un rol con privilegios mínimos.
- **Título de la app:** cambiar el `<title>` de `frontend/src/index.html` ("Frontend") por un título en español cuando lleguen las primeras pantallas.

## Antes de É4 (pacientes)
- **Política de logs sin datos personales:** el manejador global registra la causa y el stack trace de los errores inesperados, y los mensajes de Hibernate o de integridad pueden incluir valores de columnas. Definir cómo enmascarar esos datos antes de guardar datos de pacientes.
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
