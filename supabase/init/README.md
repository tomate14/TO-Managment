# Scripts de inicialización

- `01_schema.sql`: esquema completo de la base (estado final de todas las migraciones Flyway).
- `02_seed.sql`: datos iniciales (catálogos, valores por defecto).

Se mantienen al día en el mismo cambio que cada migración (regla de CLAUDE.md §7). Crear la base desde cero:

```bash
psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -v app_schema=app -f 01_schema.sql
psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -v app_schema=app -f 02_seed.sql
```

Usar como `app_schema` el valor de `APP_DB_SCHEMA` del `.env`. En el stack de Docker, la app crea el esquema sola con Flyway; estos scripts son para instalar la base fuera de ese flujo.
