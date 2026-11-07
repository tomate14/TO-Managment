# Mockups — Período de prueba (É10)

Spec: `docs/superpowers/specs/2026-11-07-trial-period-design.md`.

Imágenes a 2x: 390 px de ancho lógico en mobile y 1440 px en escritorio. Se generan desde `source/*.html` con los mismos tokens que el resto de los mockups. Los datos son de ejemplo; "hoy" es el 07/11/2026.

| Archivo | Historia | Qué muestra |
|---|---|---|
| `mobile-login-vencida.png` | H10.2 | Login con el aviso rojo "Tu período de prueba venció" y "Contactá al administrador de la plataforma.", con ícono de reloj de arena. |
| `desktop-login-vencida.png` | H10.2 | El mismo aviso en el layout partido del login. |
| `mobile-vencimientos.png` | H10.3 | Tarjetas de Establecimientos con los tres estados bajo el nombre: "Venció el …" con el badge "Vencida", "Prueba hasta el …" y "Sin vencimiento". Botones "Vencimiento" y "Entrar". |
| `desktop-vencimientos.png` | H10.3 | Tabla con la columna nueva "Prueba" (vencida, hasta una fecha, sin vencimiento) y la acción "Vencimiento". |
| `mobile-dialogo-vencimiento.png` | H10.3 | Hoja inferior "Vencimiento de la prueba": campo "Último día de prueba" con el error de fecha anterior a hoy, "Guardar", "Quitar vencimiento" y "Cancelar". |
| `desktop-dialogo-vencimiento.png` | H10.3 | El diálogo como modal: fecha nueva, aclaración de que se bloquea a la medianoche, "Quitar vencimiento" (texto rojo), "Cancelar" y "Guardar". |

## Reglas de diseño

- Lo vencido usa el rojo de error (`#FDECEA` / `#8A1C12`); "Sin vencimiento" usa el gris neutro.
- La fecha que elige el superadmin es el último día de prueba; el texto de ayuda lo aclara siempre.
- El superadmin no ve avisos de vencimiento propios: nunca se bloquea.
