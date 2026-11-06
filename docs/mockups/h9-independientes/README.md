# Mockups — Terapeutas independientes (É9)

Spec: `docs/superpowers/specs/2026-11-06-independent-therapists-design.md`.

Imágenes a 2x: 390 px de ancho lógico en mobile y 1440 px en escritorio. Se generan desde `source/*.html` con los mismos tokens que el resto de los mockups. Los datos son de ejemplo.

| Archivo | Historia | Qué muestra |
|---|---|---|
| `mobile-registro.png` | H9.1 | Formulario "Creá tu cuenta": nombre y apellido, email, contraseña y su confirmación. Incluye el error de email ya usado y el link para volver a iniciar sesión. |
| `desktop-registro.png` | H9.1 | El registro en el layout partido del login. Incluye el error de contraseñas que no coinciden. |
| `mobile-solicitud-enviada.png` | H9.1 | Confirmación "Recibimos tu solicitud", con el email usado y el botón para volver al inicio de sesión. |
| `mobile-login-pendiente.png` | H9.2 | Login con el aviso "Tu cuenta está pendiente de aprobación" y el link "¿Sos terapeuta independiente? Creá tu cuenta" que se suma al login. |
| `mobile-pendientes.png` | H9.3 | El superadmin ve "Pendientes de aprobación" arriba de la lista, con "Aprobar" y "Rechazar". La segunda tarjeta muestra la confirmación en línea del rechazo. |
| `desktop-pendientes.png` | H9.3 | La sección de pendientes sobre la tabla. El menú lateral muestra la cantidad de pendientes. Una independiente aprobada lleva el badge "Independiente"; una rechazada, el estado "Rechazada". |
| `desktop-independiente.png` | H9.4 | Menú lateral de una terapeuta independiente: "Mi consultorio" bajo el logo y en Administración, sin "Usuarios". Su rol se ve como "Terapeuta independiente". |
| `mobile-mas-independiente.png` | H9.4 | "Más" de una independiente: tarjeta "Mi consultorio" y Administración con Mi consultorio y Catálogos, sin Usuarios. |

## Reglas de diseño

- Lo pendiente usa el ámbar del sistema (`#FFF4E0` / `#7A4B00`). El rechazo usa el rojo de error.
- El link de registro aparece debajo del botón "Ingresar" en todos los estados del login.
- Un consultorio individual se llama "Mi consultorio" en toda la navegación.
