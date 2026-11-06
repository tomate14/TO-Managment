# Mockups — TO Managment

Mockups de pantallas por historia del spec `docs/superpowers/specs/2026-11-04-gestion-terapia-ocupacional-design.md`.
Las imágenes están a 2x (390 px de ancho lógico en mobile, 1440 px en escritorio). Los datos que aparecen (pacientes, terapeutas, obras sociales) son de ejemplo.

## Índice

| Historia | Carpeta | Pantallas |
|---|---|---|
| H1.1 Login | `h1.1-login/` | mobile, credenciales inválidas, sesión vencida, desktop |
| H1.2 Logout | `h1.2-logout/` | menú "Más" con cerrar sesión |
| H1.4 Gestión de usuarios | `h1.4-usuarios/` | listado, nuevo usuario (con error de email duplicado) |
| H1.5 Cambiar contraseña | `h1.5-cambiar-contrasena/` | con error de contraseña actual |
| H2.1–H2.3 Catálogos | `h2-catalogos/` | segmentos (con error de duplicado), cirugías, cicatrices |
| H3.1 Plantillas | `h3.1-plantillas/` | listado con filtro por segmento |
| H3.2 / H3.4 Editar plantilla | `h3.2-h3.4-editar-plantilla/` | preguntas, encadenadas, desactivación |
| H3.2 / H3.3 Nueva pregunta | `h3.2-h3.3-nueva-pregunta/` | numérica encadenada, de opción, error de operador incompatible |
| H4.1 Alta de paciente | `h4.1-alta-paciente/` | con error de DNI duplicado (editar = mismo formulario con DNI bloqueado) |
| H4.2 Buscar pacientes | `h4.2-buscar-pacientes/` | mobile, desktop |
| H4.3 Ver paciente | `h4.3-ficha-paciente/` | mobile, desktop |
| H5.1 Crear diagnóstico | `h5.1-crear-diagnostico/` | quirúrgico, conservador |
| H5.2 / H6.4 Diagnóstico e historial | `h5.2-h6.4-diagnostico-y-seguimientos/` | historial con respuestas de plantilla |
| H6.1–H6.3 Nuevo seguimiento | `h6.1-h6.3-nuevo-seguimiento/` | con férula (pregunta hija visible), sin férula, desktop |
| H7.1–H7.4 Agenda de turnos | `h7.1-agenda/` | calendario, turnos asignados y reserva (mobile y desktop) |
| H8.1–H8.4 Establecimientos | `h8-establecimientos/` | lista y alta de establecimientos, superadmin dentro de un establecimiento, pantalla Establecimiento del admin |

## Tokens de diseño

| Token | Valor | Uso |
|---|---|---|
| `--color-primary-dark` | `#0E2944` | Encabezados oscuros, menú lateral, badge "Quirúrgico" |
| `--color-primary` | `#1D5FA8` | Botones primarios, links, foco |
| `--color-primary-hover` | `#184F8C` | Hover del botón primario |
| `--color-primary-soft` | `#E4EEF8` | Chips seleccionados, avatares, banner informativo |
| `--color-accent` | `#8FC2F2` | Detalles sobre fondo oscuro |
| `--color-bg` | `#F5F8FB` | Fondo de la app |
| `--color-surface` | `#FFFFFF` | Tarjetas |
| `--color-border` | `#D6E0EA` | Bordes de tarjetas e inputs |
| `--color-text` | `#121A24` | Texto principal |
| `--color-text-muted` | `#4F5D6B` | Texto secundario |
| `--color-error` | `#B42318` (fondo `#FDECEA`) | Errores de campo y alertas |
| `--color-success` | `#1B6B42` (fondo `#E3F3EA`) | Estado "Activo" |
| `--color-warning-bg` | `#FFF6E5` (texto `#6B4600`) | Avisos |

- **Tipografía:** Manrope (Google Fonts), pesos 400–800. Títulos de pantalla 28 px/800, títulos de barra 17 px/700, cuerpo 15–16 px, etiquetas 14 px/600, ayudas 12–13 px.
- **Radios:** inputs y botones 12–14 px, tarjetas 18–20 px, chips 999 px.
- **Alturas:** inputs 48–52 px, botones primarios 52–54 px; ningún objetivo táctil mide menos de 44 px.
- **Inputs en mobile:** 16 px de tamaño de fuente (evita el zoom automático de iOS).
- **Navegación:** en mobile, barra inferior (Pacientes · Plantillas · Más) y barra superior con botón de volver. En escritorio, menú lateral de 248 px.

## Carpeta `source/`

Fuente editable de cada pantalla (`.dc.html`, formato del editor de diseño de Claude). Sirve como referencia de estructura, textos y estilos; no es código Angular y no se abre sola en el navegador.
