# Diseño aprobado: turnos confirmados y facturación por paciente

El usuario aprobó el diseño textual de `docs/superpowers/specs/2026-11-09-patient-appointment-billing-design.md` el 2026-11-09.

Referencias visuales existentes: `../h7.1-agenda/` (tarjetas de turnos) y `../h12-facturacion/` (contadores, formulario, historial y anulación).

- Turno sin confirmar: Reprogramar · Ver ficha · ✓ Confirmar sesión · × Cancelar.
- Turno confirmado: etiqueta Confirmado/Pendiente de facturar o Confirmado/Facturado; solo Ver ficha.
- Facturación antes de Diagnósticos en la ficha del paciente.
- Pendientes: fecha y hora, confirmación y diagnóstico opcional; sin segmentos clínicos en turnos nuevos.
- Historia anterior conservada con etiqueta Registro anterior.
- Mantener diseño móvil desde 360 px y panel lateral en escritorio.
