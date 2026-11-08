# Mockups — ROM por segmento e inmovilización opcional (É11)

Spec: `docs/superpowers/specs/2026-11-07-segment-rom-design.md`.

Las imágenes están a 2x: 390 px de ancho lógico en móvil y 1440 px en escritorio. Se generan desde `source/*.html` con los mismos tokens que el resto de los mockups, y los datos son de ejemplo.

| Archivo | Historia | Qué muestra |
|---|---|---|
| `mobile-catalogos-segmentos.png` | H11.1 | La pestaña Segmentos de Catálogos. Cada segmento suma el botón "Métricas ROM · n", con la cantidad de métricas. |
| `mobile-metricas.png` | H11.1 | Las métricas ROM de Codo: orden con flechas, tipo, "Obligatoria", editar y el switch de activa. La última está inactiva. |
| `mobile-metricas-vacio.png` | H11.1 | Un segmento sin métricas: el mensaje y el botón "Nueva métrica". |
| `mobile-nueva-metrica-opcion.png` | H11.2 | El alta de una métrica de tipo Opción: nombre, tipo, opciones con el error de opción repetida, y "Obligatoria". |
| `mobile-editar-metrica.png` | H11.2 | La edición de una métrica: el tipo bloqueado con su aclaración, y el nombre y "Obligatoria" editables. |
| `desktop-metricas-panel.png` | H11.1, H11.2 | Escritorio: la lista de métricas con el alta en un panel lateral. |
| `mobile-seguimiento-rom.png` | H11.3 | La evaluación de Codo en un seguimiento. El campo ROM fijo se reemplaza por el bloque "ROM" con sus métricas: número, Sí/No y opción con chips. Muestra el error de una métrica obligatoria vacía. |
| `mobile-seguimiento-sin-metricas.png` | H11.3 | Un segmento sin métricas: "Este segmento no tiene métricas ROM definidas." y el link "Definir en Catálogos" (solo administradores). |
| `desktop-seguimiento-rom.png` | H11.3 | El bloque ROM en escritorio, con las métricas en una fila. |
| `mobile-tarjetas-seguimiento.png` | H11.4 | Un seguimiento nuevo, con el bloque ROM en formato "Nombre: valor", y uno viejo con el ROM fijo ("ROM 90"). |
| `mobile-diagnostico-inmovilizacion-si.png` | H11.5 | Un diagnóstico conservador con "¿Lleva inmovilización?" en Sí, que muestra la fecha. |
| `mobile-diagnostico-inmovilizacion-no.png` | H11.5 | El mismo diagnóstico en No: la fecha se oculta. |
| `desktop-diagnostico-inmovilizacion.png` | H11.5 | El diagnóstico conservador con inmovilización, en escritorio. |
| `mobile-diagnostico-sin-inmovilizacion.png` | H11.5 | El detalle de un diagnóstico conservador sin inmovilización: muestra "Inmovilización: Sin inmovilización". |

## Reglas de diseño

- Edema, Dolor y Fuerza no cambian. El ROM pasa a ser un bloque propio, con un título en azul y fondo gris claro.
- Las métricas opcionales llevan "(opc.)", como Edema. Las obligatorias no llevan marca en el formulario.
- Las opciones se eligen con chips, igual que en las respuestas de plantillas. Sí/No usa el control segmentado.
- El tipo de una métrica se elige solo al crearla. En la edición aparece bloqueado, con un candado.
