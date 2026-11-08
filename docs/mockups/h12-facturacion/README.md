# Mockups — Facturación de sesiones (É12)

Spec: `docs/superpowers/specs/2026-11-08-session-billing-design.md`.

Las imágenes están a 2x: 390 px de ancho lógico en móvil y 1440 px en escritorio. Se generan desde `source/*.html` con los mismos tokens que el resto de los mockups. Los datos son de ejemplo.

| Archivo | Historia | Qué muestra |
|---|---|---|
| `mobile-facturacion.png` | H12.1, H12.3 | La sección "Facturación" del diagnóstico. Muestra los contadores (sesiones, facturadas y pendientes), el botón "Registrar facturación" y las facturaciones anteriores: una con las sesiones desplegadas, otra plegada y una anulada. |
| `mobile-registrar.png` | H12.2 | El formulario de registro: sesiones pendientes con casillas (una destildada), fecha de facturación y monto, con el error "Ingresá el monto.". |
| `mobile-anular.png` | H12.3 | La confirmación en línea de la anulación ("¿Anular esta facturación? Sus sesiones vuelven a quedar pendientes."). |
| `mobile-tarjetas.png` | H12.1, H12.4 | La tarjeta del diagnóstico con "4 sesiones pendientes de facturar" y la tarjeta de un seguimiento con la marca "Facturada". |
| `desktop-facturacion.png` | H12.1, H12.3 | La sección en escritorio: el botón junto al título y cada facturación en una fila, con monto, "Ver sesiones" y "Anular". |
| `desktop-registrar-panel.png` | H12.2 | El registro en escritorio, como panel lateral, con todas las pendientes marcadas por defecto. |

## Reglas de diseño

- Las pendientes usan el ámbar del sistema (`#FFF4E0` / `#7A4B00`) y las facturadas, el verde (`#E3F3EA` / `#1B6B42`).
- "Anular" es un link rojo y pide confirmación en línea, igual que "Rechazar" en Establecimientos.
- Una facturación anulada se atenúa: el título va tachado, lleva el badge "Anulada" y no tiene acciones salvo "Ver sesiones".
- Los montos van en formato es-AR: "$ 25.000,00".
