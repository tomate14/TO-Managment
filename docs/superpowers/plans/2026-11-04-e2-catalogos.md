# É2 Catálogos (H2.1–H2.3) — plan

**Historias (H2.1 / H2.2 / H2.3):** Como admin quiero mantener los tipos de cirugía, los tipos de cicatriz y los segmentos corporales para que las terapeutas elijan de opciones consistentes.
- Alta con nombre único (no se permiten duplicados, sin distinguir mayúsculas).
- Edición del nombre.
- Desactivación: los inactivos no se ofrecen en altas nuevas y se siguen mostrando en el historial.

**Mockups:** `docs/mockups/h2-catalogos/mobile-segmentos.png` (con error de duplicado), `mobile-cirugias.png` y `mobile-cicatrices.png` (fuente `source/app/M-Catalogs.dc.html`). Accesos en `M-More.dc.html` y `SideNav.dc.html`.

## Base de datos
- Tres tablas con la misma forma: `surgery_type`, `scar_type` y `body_segment` (`id`, `name VARCHAR(80)`, `active`), con índice único en `lower(name)`.
  - Son tablas separadas, no una sola genérica, para que las FKs de É4/É5 (diagnóstico → cirugía, seguimiento → cicatriz, plantilla → segmento) tengan integridad referencial.
- Migraciones: `V3__create_catalogs.sql` (tablas) y `V4__seed_catalogs.sql` (datos iniciales). Ambas se reflejan en `supabase/init/01_schema.sql` y `02_seed.sql`.
- **Datos iniciales** (los del mockup, todos activos):
  - Segmentos: Hombro, Codo, Muñeca, Mano y Dedos.
  - Cirugías: Osteosíntesis con placa, Tenorrafia, Liberación del túnel carpiano y Artrodesis.
  - Cicatrices: Normotrófica, Hipertrófica, Queloide y Retráctil.

## Backend (paquete `catalog`)
- `CatalogItem` (`@MappedSuperclass`: id, name, active, `rename`, `changeActive`), con las entidades `SurgeryType`, `ScarType` y `BodySegment`.
- `CatalogRepository<E>` (`@NoRepositoryBean`) con un repositorio por entidad.
- `CatalogType` (enum con el slug de la URL: `surgery-types`, `scar-types`, `body-segments`).
- **Template Method:** la interfaz `CatalogService` la implementa `AbstractCatalogService<E>`, que tiene toda la lógica (nombre único sin distinguir mayúsculas, renombrar excluyéndose a sí mismo, activar y desactivar). Cada subclase concreta solo crea su entidad (`newItem`, factory method) y declara su `CatalogType`.
- **Strategy + registro:** `CatalogServiceRegistry` resuelve el servicio según el slug; si el slug no existe, responde `CATALOG_NOT_FOUND` (404).
- API:
  - `GET /api/catalogs/{slug}?activeOnly=` → cualquier usuario autenticado (las terapeutas los van a necesitar en altas), ordenado por nombre.
  - `POST /api/catalogs/{slug}` `{name}` → 201, solo admin.
  - `PUT /api/catalogs/{slug}/{id}` `{name}` → solo admin.
  - `PATCH /api/catalogs/{slug}/{id}/active` `{active}` → solo admin.
- `ErrorCode` nuevos: `CATALOG_NOT_FOUND`, `CATALOG_ITEM_NOT_FOUND` (404) y `CATALOG_ITEM_DUPLICATE` (409, "Ya existe con ese nombre (sin importar mayúsculas).").

## Frontend
- `/catalogos` (con `adminGuard`): `CatalogsPage` con pestañas Segmentos, Cirugías y Cicatrices (`?tipo=` en la URL).
  - Arriba, el alta: "Nuevo segmento", "Nuevo tipo de cirugía" o "Nuevo tipo de cicatriz", con un input y "Agregar". Si el nombre está duplicado, se muestra el error bajo el input.
  - Una lista con nombre, estado, editar y switch.
  - Al pie: "Los inactivos no se ofrecen en altas nuevas, pero se siguen viendo en el historial."
- **Edición:** el mockup tiene el lápiz pero no la pantalla de edición. Se resuelve inline: la fila pasa a un input con "Guardar" y "Cancelar", y el duplicado se muestra en la fila.
- `CatalogsService` (HTTP).
- Navegación (solo admin): "Catálogos" en la side nav y en "Más" ("Segmentos, cirugías y cicatrices").

## Verificación
Se escriben tests (servicio abstracto con Mockito, controller con MockMvc + Testcontainers, componentes con Vitest), pero solo se verifica que compile. Los scripts `supabase/init` se corren contra un Postgres vacío.
