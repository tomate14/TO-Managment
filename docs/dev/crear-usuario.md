# Crear un usuario a mano (hasta que exista H1.3 / H1.4)

1. Generá el hash BCrypt de la contraseña (reemplazá `TU_CONTRASEÑA`):

   ```bash
   docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 x 'TU_CONTRASEÑA' | cut -d: -f2
   ```

2. Insertá el usuario (reemplazá email, nombre, rol `ADMIN` o `TERAPEUTA`, y el hash del paso 1):

   ```bash
   docker compose exec db psql -U postgres -d postgres -c \
     "insert into app.app_user (email, password_hash, full_name, role) values ('nombre@consultorio.com', 'HASH', 'Nombre Apellido', 'TERAPEUTA');"
   ```

   El email se guarda en minúsculas. Para desactivar a alguien:
   `update app.app_user set active = false where email = '...';`

3. Ingresá en http://localhost:8090/login.
