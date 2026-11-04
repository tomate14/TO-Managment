# Crear un usuario a mano (hasta que exista H1.3 / H1.4)

1. Generá el hash BCrypt de la contraseña. El comando te la pide por teclado, así que no queda en el historial de la shell ni en los argumentos del proceso:

   ```bash
   docker run --rm -it httpd:2.4-alpine htpasswd -nBC 10 x | cut -d: -f2
   ```

2. Insertá el usuario (reemplazá email, nombre, rol `ADMIN` o `TERAPEUTA`, y el hash del paso 1):

   ```bash
   docker compose exec db psql -U postgres -d postgres -c \
     "insert into app.app_user (email, password_hash, full_name, role) values ('nombre@consultorio.com', 'HASH', 'Nombre Apellido', 'TERAPEUTA');"
   ```

   El login no distingue mayúsculas de minúsculas en el email, pero conviene escribirlo siempre en minúsculas. Para desactivar a alguien:
   `update app.app_user set active = false where email = '...';`

3. Ingresá en http://localhost:8090/login.
