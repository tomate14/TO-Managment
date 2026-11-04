# Smoke checks del stack

Verificaciones rápidas después de `docker compose up -d --build`. Los puertos se leen del `.env` sin cargarlo en la shell (nunca `source .env`) y no se escriben secretos acá.

```bash
FP=$(grep ^FRONTEND_PORT= .env | cut -d= -f2)
BP=$(grep ^BACKEND_PORT= .env | cut -d= -f2)
```

> Por qué importa el header `Origin`: una vez, un `Host` sin puerto en el proxy de nginx hizo que las requests del navegador al mismo origen fallaran como CORS. `curl` sin `Origin` no lo detecta.

1. **Login con contraseña incorrecta, desde el origen del frontend** -> 401 con código `AUTHENTICATION_FAILED`:

   ```bash
   curl -s -o /dev/stderr -w '%{http_code}\n' -X POST "http://localhost:$FP/api/auth/login" \
     -H "Origin: http://localhost:$FP" -H 'Content-Type: application/json' \
     -d '{"email":"nadie@consultorio.com","password":"incorrecta"}'
   ```

2. **Lo mismo con un origen ajeno** -> 403:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST "http://localhost:$FP/api/auth/login" \
     -H "Origin: http://evil.example" -H 'Content-Type: application/json' \
     -d '{"email":"nadie@consultorio.com","password":"incorrecta"}'
   ```

3. **`/api/auth/me` sin token** -> 401 (con un token válido -> 200):

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:$FP/api/auth/me" -H "Origin: http://localhost:$FP"
   ```

4. **Salud del backend** (solo loopback) -> 200:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' "http://127.0.0.1:$BP/actuator/health"
   ```

5. **Ruta profunda de la SPA** -> 200 (nginx devuelve `index.html`):

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:$FP/login"
   ```
