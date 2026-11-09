# Facturación por paciente a partir de turnos confirmados

Estado: diseño aprobado por el usuario el 2026-11-09, incluyendo el bloqueo de cancelación y reprogramación de turnos confirmados.

## Objetivo y decisiones del usuario

- Confirmar un turno genera una sesión pendiente de facturar para su paciente.
- Crear un seguimiento clínico nunca genera pendientes de facturación.
- Los seguimientos anteriores sin facturar dejan de aparecer como pendientes; no se borran.
- La facturación pertenece al paciente, no a un diagnóstico. Puede agrupar turnos de distintos diagnósticos o sin diagnóstico.
- La lista muestra fecha del turno, confirmación y diagnóstico únicamente cuando existe.
- Se conserva el flujo de selección de pendientes, fecha, monto y registro/anulación de facturaciones.
- Las facturaciones anteriores y sus sesiones se conservan como historial.

## Diseño de pantallas aprobado

Se reutilizan los estilos y distribución de los mockups de `docs/mockups/h7.1-agenda/` y `docs/mockups/h12-facturacion/`.

### Turnos asignados

```text
09:00 a 09:45  Paciente de ejemplo
Diagnóstico del turno (solo si existe)
Terapeuta
[Reprogramar] [Ver ficha] [✓] [×]
```

El nuevo botón de tilde queda inmediatamente antes de cancelar, con nombre accesible y tooltip «Confirmar sesión». Un clic confirma directamente. Durante la petición queda deshabilitado; ante un error se muestra el mensaje sin marcar el turno como confirmado. El servidor también evita duplicados.

```text
09:00 a 09:45  Paciente de ejemplo
✓ Confirmado · Pendiente de facturar
[Ver ficha]
```

Una vez facturado, la etiqueta cambia a «✓ Confirmado · Facturado». El estado no depende únicamente del color.

Regla aprobada: un turno confirmado no se puede cancelar ni reprogramar, tampoco mediante llamadas directas a la API. Evita borrar o cambiar una sesión que ya integra la facturación. Revertir una confirmación queda fuera de este cambio.

No se introduce una restricción de fecha no solicitada: la terapeuta decide cuándo confirmar.

### Ficha del paciente

Agregar la sección Facturación antes de Diagnósticos, manteniendo los datos personales y la distribución responsive existente.

```text
Facturación
3 turnos confirmados · 1 facturado · 2 pendientes
[Registrar facturación]

Facturaciones anteriores
...

Diagnósticos
...
```

Los contadores corresponden exclusivamente a turnos confirmados. Las facturaciones basadas en seguimientos se muestran en el historial con la etiqueta «Registro anterior», sin sumarlas a esos contadores.

El formulario mantiene página en móvil y panel en escritorio, todas las pendientes seleccionadas inicialmente y ordenadas por fecha/hora del turno e ID:

```text
☑ 09/11/2026 · 09:00   ✓ Confirmado
  Diagnóstico de ejemplo
☑ 10/11/2026 · 10:00   ✓ Confirmado

Fecha de facturación [ ... ]
Monto                 [ ... ]
[Cancelar] [Registrar facturación]
```

La segunda fila no muestra una línea vacía ni «Sin diagnóstico». Conservar además fecha/hora de confirmación en los datos para trazabilidad. No se muestran segmentos evaluados en los nuevos turnos facturables.

Eliminar la sección de facturación y el contador de pendientes del diagnóstico. Los seguimientos pueden conservar la marca histórica de facturado cuando corresponda a un registro anterior, sin generar nuevas pendientes.

## Modelo y transición

Migración nueva V15, sin editar migraciones aplicadas, reflejada en el SQL consolidado de inicialización:

- `appointment.confirmed_at`: instante nullable, inicialmente nulo. Ningún turno anterior se confirma automáticamente.
- `billing.patient_id`: rellenar desde el paciente del diagnóstico de cada registro anterior y establecer NOT NULL. `diagnosis_id` queda nullable para conservar el vínculo histórico; las facturaciones nuevas no dependen de él.
- Nueva relación `billing_appointment`, equivalente a la relación actual con seguimientos: facturación, turno, establecimiento y vínculo activo. Clave compuesta e índice único parcial por turno activo para impedir doble facturación concurrente.
- Mantener `billing_follow_up` para el historial. No crear vínculos entre seguimientos y turnos por coincidencia de fechas.
- Anular una facturación nueva libera sus turnos confirmados. Anular una histórica conserva su registro, pero no vuelve a convertir seguimientos en pendientes, de acuerdo con la nueva regla.
- Totales: confirmados = facturados por vínculo vigente + pendientes. Historial de facturación incluye registros antiguos y nuevos, identificando su origen.

## Contratos y permisos

- `POST /api/appointments/{id}/confirm`: confirmación idempotente, conserva el primer instante de confirmación. Devuelve el turno actualizado. Usa los permisos actuales de gestión de agenda y aislamiento por establecimiento.
- `AppointmentResponse`: sumar `confirmedAt` y `billed`.
- `GET /api/patients/{patientId}/billing`: resumen, pendientes y facturaciones del paciente.
- `POST /api/patients/{patientId}/billings`: `{ billedOn, amount, appointmentIds }`.
- `POST /api/billings/{id}/void`: conserva su ruta y devuelve el resumen del paciente.
- Sesión nueva: `appointmentId`, fecha/hora del turno, `confirmedAt`, diagnóstico opcional. Historial antiguo: tipo de origen y referencia al seguimiento, sin inventar turno ni confirmación.
- Formularios nuevos en `/pacientes/:id/facturacion/nueva`. Redirigir enlaces antiguos de formularios por diagnóstico a esta ruta. El frontend deja de usar endpoints de facturación por diagnóstico; retirarlos en el despliegue coordinado.
- Validar paciente, establecimiento, confirmación, IDs repetidos y facturación vigente. No aceptar turnos de otro paciente aunque pertenezcan al mismo establecimiento. Mantener errores custom, DTOs, servicios con interfaz y enums de dominio.
- Confirmar, cancelar, reprogramar y facturar deben coordinar bloqueos transaccionales sobre el turno, además del índice único de facturación, para evitar carreras.

## Verificación y entrega

- TDD de confirmación idempotente, permisos de agenda, aislamiento entre establecimientos y restricción de cambios de turnos confirmados.
- Pruebas de pendientes de varios diagnósticos y sin diagnóstico, independencia de los seguimientos y conservación del historial.
- Registro parcial, doble facturación/concurrencia, anulación y vuelta a pendientes de turnos; anulación histórica sin pendientes de seguimientos.
- Verificar migración desde V14 con registros vigentes y anulados y SQL consolidado sobre una base vacía.
- Frontend: clic en tilde, estados de carga/error, etiquetas, selección, actualización de contadores, ficha del paciente y compatibilidad de navegación.
- Revisar móvil desde 360 px y escritorio conforme al diseño aprobado.
- Implementar en rama aislada con subagentes y revisión de código según CLAUDE.md. No incluir archivos ajenos ni secretos locales. El despliegue remoto no forma parte de este cambio hasta que se solicite.
