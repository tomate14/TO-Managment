const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const out = path.resolve(__dirname, 'screenshots');
fs.mkdirSync(out, { recursive: true });
const patient = { id: 7, firstName: 'Paciente', lastName: 'Sintetico', dni: '10000007', birthDate: '1990-01-01', phone: '0000000000', occupation: 'Fixture', healthInsurance: 'Fixture', healthInsuranceNumber: '000', dominance: 'RIGHT' };
const user = { id: 1, email: 'fixture@example.test', fullName: 'Terapeuta Sintetica', role: 'TERAPEUTA', establishment: { id: 1, name: 'Consultorio sintetico', kind: 'INDIVIDUAL' } };
const diagnosis = { id: 3, patient, description: 'Diagnostico sintetico', createdDate: '2026-11-01', followUpCount: 0, treatment: { type: 'CONSERVATIVE', hasImmobilization: false, immobilizationDate: null, surgeryDate: null, stitchesRemovalDate: null, surgeryType: null, requiresScarType: false } };
const token = 'e30.' + Buffer.from(JSON.stringify({ exp: 4102444800 })).toString('base64url') + '.fixture';
async function visible(locator) { await locator.first().waitFor({ state: 'visible', timeout: 15000 }); }
async function counters(page, expected) {
  await page.waitForFunction(wanted => JSON.stringify([...document.querySelectorAll('.counter__value')].map(el => el.textContent)) === JSON.stringify(wanted), expected);
  assert.deepEqual(await page.locator('.counter__value').allTextContents(), expected);
}
async function keyboardActivate(page, locator) {
  await locator.waitFor({ state: 'visible' });
  await page.waitForFunction(el => !el.disabled, await locator.elementHandle());
  await locator.focus();
  assert.equal(await locator.evaluate(el => document.activeElement === el), true);
  await page.keyboard.press('Enter');
}
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_EXECUTABLE || undefined });
  const results = [];
  try {
    for (const width of [360, 1366]) {
      const context = await browser.newContext({ viewport: { width, height: 900 }, timezoneId: 'America/Buenos_Aires' });
      await context.addInitScript(session => localStorage.setItem('to-management.session', JSON.stringify(session)), { token, user });
      const page = await context.newPage();
      await page.clock.install({ time: new Date('2026-11-10T12:00:00Z') });
      const errors = [], requests = [], unknown = [];
      let appointment = null, billed = false, newBilling = null, followUps = [];
      let legacyVoided = false;
      let confirmFailuresRemaining = 0, confirmAttempts = 0;
      page.on('pageerror', e => errors.push(e.message));
      function summary() {
        const session = appointment && { origin: 'APPOINTMENT', appointmentId: appointment.id, followUpId: null, date: appointment.date, startTime: appointment.startTime, confirmedAt: appointment.confirmedAt, diagnosis: null, segments: [] };
        const legacy = { id: 88, origin: 'LEGACY_FOLLOW_UP', billedOn: '2026-10-10', amount: 500, sessionCount: 1, voided: legacyVoided, voidedAt: legacyVoided ? '2026-11-09T15:00:00Z' : null, createdAt: '2026-10-10T12:00:00Z', sessions: [{ origin: 'LEGACY_FOLLOW_UP', appointmentId: null, followUpId: 55, date: '2026-10-09', startTime: null, confirmedAt: null, diagnosis: { id: 3, description: diagnosis.description }, segments: ['Codo sintetico'] }] };
        return { totalSessions: appointment?.confirmedAt ? 1 : 0, billedSessions: billed ? 1 : 0, pendingSessions: appointment?.confirmedAt && !billed ? 1 : 0, pending: appointment?.confirmedAt && !billed ? [session] : [], billings: [...(newBilling ? [{ ...newBilling, sessions: [session] }] : []), legacy] };
      }
      await page.route('**/api/**', async route => {
        const request = route.request(), url = new URL(request.url()), p = url.pathname, method = request.method();
        requests.push({ method, path: p });
        const send = (json, status = 200) => route.fulfill({ status, json });
        if (p === '/api/auth/me') return send(user);
        if (p === '/api/agenda/therapists') return send([{ id: 1, fullName: user.fullName, active: true, color: 0 }]);
        if (p === '/api/appointments/availability') return send([{ startTime: '12:00:00', available: true, reason: null }]);
        if (p === '/api/appointments' && method === 'GET') {
          const inRange = appointment && appointment.date >= url.searchParams.get('from') && appointment.date <= url.searchParams.get('to');
          return send(inRange ? [{ ...appointment, billed }] : []);
        }
        if (p === '/api/appointments' && method === 'POST') {
          const body = request.postDataJSON(); assert.equal(body.diagnosisId, null);
          appointment = { id: 21, therapist: { id: 1, fullName: user.fullName, color: 0 }, patient, diagnosis: null, date: body.date, startTime: '12:00:00', endTime: '12:45:00', durationMinutes: 45, notes: null, confirmedAt: null, billed: false };
          return send(appointment, 201);
        }
        if (p === '/api/appointments/21' && method === 'GET') return send({ ...appointment, billed });
        if (p === '/api/appointments/21' && method === 'PUT') {
          const body = request.postDataJSON(); assert.equal(body.date, appointment.date);
          appointment = { ...appointment, ...body }; return send(appointment);
        }
        if (p === '/api/appointments/21' && method === 'DELETE') { appointment = null; return route.fulfill({ status: 204 }); }
        if (p === '/api/appointments/21/confirm') {
          assert.equal(method, 'POST'); assert.deepEqual(request.postDataJSON(), {}); confirmAttempts++;
          if (confirmFailuresRemaining > 0) { confirmFailuresRemaining--; return send({ code: 'FIXTURE_RETRY', message: 'Error sintético: intentá de nuevo.' }, 500); }
          appointment.confirmedAt = '2026-11-10T12:00:00Z'; return send(appointment);
        }
        if (p === '/api/patients/7/billing') return send(summary());
        if (p === '/api/patients/7/billings') {
          const body = request.postDataJSON(); assert.deepEqual(body.appointmentIds, [21]); assert.equal(body.amount, 1000);
          billed = true; newBilling = { id: 99, origin: 'APPOINTMENT', billedOn: body.billedOn, amount: body.amount, sessionCount: 1, voided: false, voidedAt: null, createdAt: '2026-11-09T15:00:00Z' };
          return send(summary(), 201);
        }
        if (p === '/api/billings/99/void') { billed = false; newBilling.voided = true; newBilling.voidedAt = '2026-11-09T16:00:00Z'; return send(summary()); }
        if (p === '/api/billings/88/void') { legacyVoided = true; return send(summary()); }
        if (p === '/api/patients/7') return send(patient);
        if (p === '/api/patients') return send({ content: [patient], totalElements: 1, totalPages: 1, number: 0, size: 8 });
        if (p === '/api/patients/7/diagnoses') return send([diagnosis]);
        if (p === '/api/diagnoses/3') return send({ ...diagnosis, followUpCount: followUps.length });
        if (p === '/api/diagnoses/3/follow-ups') {
          if (method === 'POST') { followUps.push({ id: 65, ...request.postDataJSON(), billed: false }); return send(followUps[0], 201); }
          return send(followUps);
        }
        if (p === '/api/segments/4/metrics' || p.includes('/body-segments/4/templates')) return send([]);
        if (p.includes('body-segments')) return send([{ id: 4, name: 'Codo sintetico', active: true }]);
        if (p.includes('scar-types') || p.includes('templates') || p.includes('segment-metrics')) return send([]);
        unknown.push(method + ' ' + p); return send({ code: 'FIXTURE_UNEXPECTED_ENDPOINT' }, 404);
      });
      await page.goto('http://127.0.0.1:4217/agenda/nuevo?vista=turnos');
      await page.getByLabel('Paciente', { exact: true }).fill('10000007');
      await page.getByRole('button', { name: /Sintetico.*Paciente|Paciente.*Sintetico/ }).click();
      await page.getByRole('button', { name: '12:00', exact: true }).click();
      await keyboardActivate(page, page.getByRole('button', { name: 'Reservar turno' }));
      await visible(page.getByRole('button', { name: 'Confirmar sesión' }));
      await page.getByRole('button', { name: 'Reprogramar', exact: true }).click();
      await visible(page.locator('#appointment-notes'));
      await page.locator('#appointment-notes').fill('Nota sintética');
      await keyboardActivate(page, page.getByRole('button', { name: 'Guardar cambios', exact: true }));
      await visible(page.getByRole('button', { name: 'Confirmar sesión' }));
      await keyboardActivate(page, page.getByRole('button', { name: 'Confirmar sesión' }));
      await visible(page.getByText('✓ Confirmado · Pendiente de facturar', { exact: true }));
      assert.equal(await page.getByRole('button', { name: 'Cancelar turno' }).count(), 0);
      await page.screenshot({ path: path.join(out, `${width}-agenda-confirmed.png`), fullPage: true });
      await page.getByRole('link', { name: 'Ver ficha', exact: true }).click();
      await visible(page.getByRole('heading', { name: 'Facturación', exact: true }));
      await counters(page, ['1', '0', '1']);
      await keyboardActivate(page, page.getByRole('button', { name: 'Registrar facturación', exact: true }));
      await visible(page.locator('#billing-amount'));
      const checkbox = page.getByRole('checkbox'); assert.equal(await checkbox.count(), 1); assert.equal(await checkbox.isChecked(), true);
      await checkbox.focus(); await page.keyboard.press('Space'); assert.equal(await checkbox.isChecked(), false); await page.keyboard.press('Space');
      await page.locator('#billing-amount').fill('1000');
      const layout = await page.locator('.billing-form__actions').evaluate(footer => {
        const boundary = footer.getBoundingClientRect();
        return [...footer.querySelectorAll('button')].map(button => {
          const rect = button.getBoundingClientRect();
          return { label: button.textContent.trim(), inside: rect.left >= boundary.left && rect.right <= boundary.right && rect.top >= boundary.top && rect.bottom <= boundary.bottom };
        });
      });
      assert.equal(layout.length, 2);
      for (const button of layout) assert.equal(button.inside, true, `${width}px footer clips ${button.label}`);
      await page.screenshot({ path: path.join(out, `${width}-billing-form.png`), fullPage: true });
      await keyboardActivate(page, page.getByRole('button', { name: 'Registrar facturación', exact: true }).last());
      await visible(page.locator('.billing-row').first());
      await counters(page, ['1', '1', '0']);
      await page.goto('http://127.0.0.1:4217/agenda?vista=turnos');
      await visible(page.getByText('✓ Confirmado · Facturado', { exact: true }));
      await page.screenshot({ path: path.join(out, `${width}-agenda-billed.png`), fullPage: true });
      await page.getByRole('link', { name: 'Ver ficha', exact: true }).click();
      await page.locator('.billing-row').first().getByRole('button', { name: 'Anular', exact: true }).click();
      await keyboardActivate(page, page.getByRole('button', { name: 'Sí, anular' }));
      await visible(page.getByRole('button', { name: 'Registrar facturación', exact: true }));
      await counters(page, ['1', '0', '1']);
      const legacy = page.locator('.billing-row').filter({ hasText: 'Registro anterior' });
      await legacy.getByRole('button', { name: 'Ver sesiones' }).click();
      await visible(page.getByText('Codo sintetico', { exact: true }));
      await legacy.getByRole('button', { name: 'Anular', exact: true }).click();
      await page.getByRole('button', { name: 'Sí, anular' }).click();
      await visible(legacy.getByText('Anulada', { exact: true }));
      await counters(page, ['1', '0', '1']);
      await page.screenshot({ path: path.join(out, `${width}-voided-legacy-history.png`), fullPage: true });
      await page.goto('http://127.0.0.1:4217/pacientes/7/diagnosticos/3/facturacion/nueva');
      await visible(page.locator('#billing-amount'));
      assert.equal(new URL(page.url()).pathname, '/pacientes/7/facturacion/nueva');
      assert.equal(await page.getByRole('checkbox').count(), 1);
      await page.goto('http://127.0.0.1:4217/pacientes/7/diagnosticos/3/seguimientos/nuevo');
      await visible(page.locator('#evaluation-0-segment'));
      await page.locator('#evaluation-0-segment').selectOption({ label: 'Codo sintetico' });
      await page.locator('#evaluation-0-strength').fill('5');
      await keyboardActivate(page, page.getByRole('button', { name: 'Guardar seguimiento' }));
      await page.waitForURL('**/pacientes/7/diagnosticos/3');
      assert.equal(followUps.length, 1);
      await page.goto('http://127.0.0.1:4217/pacientes/7');
      await visible(page.getByRole('heading', { name: 'Facturación', exact: true }));
      await counters(page, ['1', '0', '1']);

      async function openCalendarAppointment(date) {
        await page.goto('http://127.0.0.1:4217/agenda?vista=calendario');
        if (date === '2026-11-03') {
          const navigation = width < 1024 ? page.locator('.calendar-mobile') : page.locator('.calendar-toolbar');
          await navigation.getByRole('button', { name: 'Semana anterior', exact: true }).click();
        }
        if (width < 1024) await page.getByRole('button', { name: date, exact: true }).click();
        const calendar = width < 1024 ? page.locator('.calendar-mobile .agenda-block') : page.locator('.calendar-desktop .agenda-block');
        await keyboardActivate(page, calendar);
        await visible(page.getByRole('button', { name: 'Confirmar sesión' }));
      }
      for (const date of ['2026-11-09', '2026-11-03']) {
        appointment = { ...appointment, date, confirmedAt: null, billed: false };
        billed = false; newBilling = null; confirmAttempts = 0;
        confirmFailuresRemaining = date === '2026-11-09' ? 1 : 0;
        await page.goto('http://127.0.0.1:4217/agenda?vista=turnos');
        await visible(page.getByText('No hay turnos en este período.', { exact: true }));
        await openCalendarAppointment(date);
        if (date === '2026-11-09') {
          await page.getByRole('button', { name: 'Reprogramar', exact: true }).click();
          await visible(page.locator('#appointment-date'));
          assert.equal(await page.locator('#appointment-date').inputValue(), date);
          await keyboardActivate(page, page.locator('.booking__cancel'));
          await openCalendarAppointment(date);
        }
        const card = page.locator('app-appointment-card');
        await visible(card.getByRole('button', { name: 'Confirmar sesión' }));
        assert.equal(await card.locator('time').getAttribute('datetime'), date);
        assert.match(await card.locator('time').innerText(), /2026/);
        assert.match(await card.innerText(), /12:00/);
        assert.equal(await page.locator('app-appointment-form').count(), 0);
        const dimensions = await card.getByRole('button', { name: 'Confirmar sesión' }).boundingBox();
        assert.ok(dimensions.width >= 44 && dimensions.height >= 44);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, 'detail viewport overflow');
        if (confirmFailuresRemaining) {
          await card.getByRole('button', { name: 'Confirmar sesión' }).evaluate(button => { button.click(); button.click(); });
          await visible(card.getByRole('alert'));
          assert.equal(confirmAttempts, 1, 'double click sends one POST');
          assert.equal(await card.getByRole('button', { name: 'Reprogramar', exact: true }).isEnabled(), true);
        }
        await keyboardActivate(page, card.getByRole('button', { name: 'Confirmar sesión' }));
        await visible(card.getByText('✓ Confirmado · Pendiente de facturar', { exact: true }));
        assert.equal(confirmAttempts, date === '2026-11-09' ? 2 : 1);
        assert.equal(appointment.date, date, 'confirmation preserves original date');
        assert.equal(await card.getByRole('button').count(), 0, 'confirmed card has no editing actions');
        await page.screenshot({ path: path.join(out, `${width}-calendar-${date}-confirmed.png`), fullPage: true });
        await keyboardActivate(page, card.getByRole('link', { name: 'Ver ficha', exact: true }));
        await visible(page.getByRole('heading', { name: 'Facturación', exact: true }));
        await counters(page, ['1', '0', '1']);
        await keyboardActivate(page, page.getByRole('button', { name: 'Registrar facturación', exact: true }));
        await visible(page.getByRole('checkbox'));
        assert.equal(await page.getByRole('checkbox').count(), 1);
        assert.equal(summary().pending[0].date, date);
        const pendingDate = date === '2026-11-09' ? '09/11/2026' : '03/11/2026';
        assert.match(await page.locator('.billing-form').innerText(), new RegExp(pendingDate));
        await page.screenshot({ path: path.join(out, `${width}-calendar-${date}-pending.png`), fullPage: true });
      }
      appointment = { ...appointment, confirmedAt: null };
      await openCalendarAppointment('2026-11-03');
      await page.getByRole('button', { name: 'Cancelar turno', exact: true }).click();
      await keyboardActivate(page, page.getByRole('button', { name: 'Sí, cancelar', exact: true }));
      await page.waitForFunction(() => !document.querySelector('app-appointment-card'));
      assert.equal(appointment, null, 'calendar detail cancellation sends DELETE');
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, 'horizontal viewport overflow');
      assert.deepEqual(errors, []); assert.deepEqual(unknown, []);
      results.push({ width, passed: ['book without diagnosis', 'existing reschedule form', 'confirm with keyboard', 'patient pending', 'select keyboard space', 'bill', 'agenda billed', 'void restores pending', 'legacy sessions and void preserve count', 'old route redirect', 'follow-up preserves billing counters', 'detail reschedule opens original form', 'detail cancel works', 'yesterday calendar confirmation and retry', 'previous-week calendar confirmation', 'original dates in patient pending', 'viewport fits'], requests });
      fs.writeFileSync(path.join(out, 'results.json'), JSON.stringify(results, null, 2));
      console.log(`PASS width=${width}`); await context.close();
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
