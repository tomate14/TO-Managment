# Mockups — Establecimientos (É8, multi-tenencia)

Spec: `docs/superpowers/specs/2026-11-06-multi-tenant-design.md`. Imágenes a 2x (390 px de ancho lógico en mobile, 1440 px en escritorio), generadas desde `source/*.html` con los mismos tokens que el resto de los mockups. Los datos son de ejemplo.

| Archivo | Historia | Qué muestra |
|---|---|---|
| `mobile-establecimientos.png` | H8.2 | Superadmin fuera de un establecimiento: lista con nombre, admin, cantidad de usuarios, estado (switch activo/inactivo), renombrar y "Entrar". La barra inferior solo tiene Establecimientos y Más. |
| `desktop-establecimientos.png` | H8.2 | La misma lista en tabla; el menú lateral del superadmin solo tiene Establecimientos, Cambiar contraseña y Cerrar sesión. Un establecimiento inactivo no se puede "Entrar". |
| `mobile-nuevo-establecimiento.png` | H8.1 | Alta: nombre del establecimiento y primer administrador (nombre, email, contraseña inicial), con el error de email ya usado y el aviso de que se cargan los catálogos por defecto. |
| `desktop-nuevo-establecimiento.png` | H8.1 | El alta como panel lateral sobre la lista. |
| `mobile-dentro-establecimiento.png` | H8.3 | Superadmin dentro de un establecimiento: aviso fijo "Estás en ‹nombre›" con "Salir", y la navegación completa. |
| `desktop-dentro-establecimiento.png` | H8.3 / H8.4 | Aviso "Estás en ‹nombre› como superadmin" con "Salir del establecimiento"; el menú lateral muestra el establecimiento activo bajo el logo y suma "Establecimiento" en Administración. |
| `mobile-establecimiento-admin.png` | H8.4 | Pantalla "Establecimiento" del admin: renombrar, datos (usuarios, fecha de creación) y cómo se ve el nombre en "Más". |

## Reglas de diseño
- El nombre del establecimiento activo se muestra bajo el logo del menú lateral (desktop) y en "Más" (mobile), para todos los usuarios del establecimiento.
- El aviso del superadmin usa el fondo oscuro de la marca (`#0E2944`) para que no se confunda con contenido de la página.
- Iconografía nueva: edificio (Establecimientos / Establecimiento).
