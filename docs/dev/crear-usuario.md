# Crear un usuario a mano (hasta que exista H1.4)

## Superadmin inicial (H1.3, É8)
Al arrancar, si no hay ningún usuario `SUPERADMIN`, el backend crea uno (sin establecimiento) con `ADMIN_EMAIL`, `ADMIN_PASSWORD` y `ADMIN_NAME` del `.env`. El superadmin da de alta los establecimientos (con su primer admin) desde `/api/establishments`.
- Si ya existe un superadmin, no hace nada. Cambiar esas variables después **no** modifica al superadmin existente.
- Si el email ya lo usa otro usuario, no se crea el superadmin y el log del backend lo avisa (`docker compose logs backend`).

Antes del primer arranque, poné una contraseña real en `ADMIN_PASSWORD` (nunca `change-me`).

## Otros usuarios por SQL

1. Generá el hash BCrypt de la contraseña. El comando te la pide por teclado, así que no queda en el historial de la shell ni en los argumentos del proceso:

   ```bash
   docker run --rm -it httpd:2.4-alpine htpasswd -nBC 10 x | cut -d: -f2
   ```

2. Insertá el usuario (reemplazá email, nombre, rol `ADMIN` o `TERAPEUTA`, el `establishment_id` del establecimiento y el hash del paso 1):

   ```bash
   docker compose exec db psql -U postgres -d postgres -c \
     "insert into app.app_user (email, password_hash, full_name, role, establishment_id) values ('nombre@consultorio.com', 'HASH', 'Nombre Apellido', 'TERAPEUTA', 1);"
   ```

   El login no distingue mayúsculas de minúsculas en el email, pero conviene escribirlo siempre en minúsculas. Para desactivar a alguien:
   `update app.app_user set active = false where email = '...';`

3. Ingresá en http://localhost:8090/login.
