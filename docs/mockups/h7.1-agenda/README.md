# Mockups — Agenda de turnos

> Épica É7, diseñada en `docs/superpowers/specs/2026-11-05-agenda-design.md`. Decisiones que cambian estos mockups: **no hay estados** (no se muestran los badges "Confirmado / A confirmar") y cancelar borra el turno.

Imágenes a 2x (390 px de ancho lógico en mobile, 1440 px en escritorio). Los datos son de ejemplo; "hoy" es el jueves 5 de noviembre de 2026 a las 11:40.

## Pantallas

| Archivo | Qué muestra |
|---|---|
| `mobile-calendario.png` | Pestaña **Calendario**: tira semanal (puntos de color por terapeuta) y vista del día por hora, de 08:00 a 20:00, con línea de hora actual. Los turnos superpuestos se dividen en columnas. |
| `mobile-turnos-asignados.png` | Pestaña **Turnos asignados**: cards agrupadas por día, filtro Hoy / Esta semana; acciones Reprogramar, Ver ficha y Cancelar. |
| `mobile-nuevo-turno.png` | Reserva: paciente, diagnóstico (opcional), terapeuta, fecha, duración (30/45/60 min), grilla de horarios cada 30 min (ocupados tachados y deshabilitados), notas y resumen. |
| `desktop-calendario.png` | Vista semanal tipo Google Calendar (lunes a domingo), mini calendario del mes y filtro por terapeuta. |
| `desktop-turnos-asignados.png` | Las mismas cards en grilla de 3 columnas. |
| `desktop-reservar-turno.png` | Panel lateral de reserva sobre el calendario (se abre con "Nuevo turno" o "Reprogramar"). |

## Reglas de diseño

- **Pestañas:** control segmentado con `role="tablist"`; la pestaña activa va en blanco sobre fondo `#F0F4F8` (mobile) / `#E9EEF4` (escritorio).
- **Navegación:** se agrega "Agenda" a la barra inferior mobile (Pacientes · Agenda · Plantillas · Más) y al menú lateral de escritorio.
- **Grilla horaria:** 64 px por hora en mobile y 52 px en escritorio; la línea de hora actual es `#B42318`.
- **Estados del turno (propuesta):** Confirmado (`#E3F3EA` / `#1B6B42`) y A confirmar (`#FFF6E5` / `#6B4600`).

## Colores por terapeuta

| Terapeuta | Fondo del bloque | Texto | Punto / marca |
|---|---|---|---|
| 1 | `#DCE8F7` | `#0E3A6E` | `#1D5FA8` |
| 2 | `#D3EEEA` | `#0B4F4C` | `#0F7B78` |
| 3 | `#FBE7CF` | `#6B3A06` | `#B45309` |
| 4 | `#E8E0F6` | `#45297A` | `#6D4AB0` |

El color se asigna a cada terapeuta; el nombre también aparece en texto, así que no se depende solo del color.

## Carpeta `source/`

Fuente editable de cada pantalla (`.dc.html`, formato del editor de diseño de Claude). Es referencia de estructura, textos y estilos; no es código Angular.
