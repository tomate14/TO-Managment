import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { EstablishmentForm } from './establishment-form';
import { EstablishmentsService } from '../establishments.service';
import { EstablishmentSummary } from '../establishments.models';

describe('EstablishmentForm', () => {
  const created: EstablishmentSummary = { id: 9, name: 'Kinesio Centro', kind: 'INSTITUTION', active: true, pendingApproval: false, rejected: false, createdAt: '2026-11-01T12:00:00Z', activeUsers: 1, admins: [], trialEndsAt: null, trialExpired: false };
  let service: { create: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    service = { create: vi.fn(() => of(created)) };
    TestBed.configureTestingModule({
      imports: [EstablishmentForm],
      providers: [{ provide: EstablishmentsService, useValue: service }],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(EstablishmentForm);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, component: fixture.componentInstance };
  }

  type Fixture = { detectChanges(): void };

  function type(el: HTMLElement, id: string, value: string, fixture: Fixture) {
    const input = el.querySelector<HTMLInputElement>(`#${id}`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function fillValid(el: HTMLElement, fixture: Fixture) {
    type(el, 'est-name', ' Kinesio Centro ', fixture);
    type(el, 'est-admin-name', 'Sofía Núñez', fixture);
    type(el, 'est-admin-email', 'sofia@kinesiocentro.com', fixture);
    type(el, 'est-admin-password', 'clave-inicial', fixture);
  }

  function submit(el: HTMLElement, fixture: Fixture) {
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('shows required errors and does not call the backend when empty', () => {
    const { fixture, el } = render();
    submit(el, fixture);
    expect(el.textContent).toContain('Ingresá el nombre del establecimiento.');
    expect(el.textContent).toContain('Ingresá el email.');
    expect(el.textContent).toContain('Ingresá la contraseña inicial.');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('rejects an invalid email and a short password', () => {
    const { fixture, el } = render();
    fillValid(el, fixture);
    type(el, 'est-admin-email', 'no-es-email', fixture);
    type(el, 'est-admin-password', 'corta', fixture);
    submit(el, fixture);
    expect(el.textContent).toContain('Ingresá un email válido');
    expect(el.textContent).toContain('al menos 8 caracteres');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('creates the establishment with trimmed values and emits it', () => {
    const { fixture, el, component } = render();
    const emitted: EstablishmentSummary[] = [];
    component.created.subscribe((value) => emitted.push(value));
    fillValid(el, fixture);
    submit(el, fixture);
    expect(service.create).toHaveBeenCalledWith({
      name: 'Kinesio Centro',
      admin: { fullName: 'Sofía Núñez', email: 'sofia@kinesiocentro.com', password: 'clave-inicial' },
    });
    expect(emitted).toEqual([created]);
  });

  it('maps USER_EMAIL_TAKEN to the email field', () => {
    service.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'USER_EMAIL_TAKEN', message: 'x' } })),
    );
    const { fixture, el } = render();
    fillValid(el, fixture);
    submit(el, fixture);
    expect(el.querySelector('#est-admin-email-error')?.textContent).toContain('Ya hay un usuario con este email.');
  });

  it('maps ESTABLISHMENT_NAME_TAKEN to the name field', () => {
    service.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'ESTABLISHMENT_NAME_TAKEN', message: 'x' } })),
    );
    const { fixture, el } = render();
    fillValid(el, fixture);
    submit(el, fixture);
    expect(el.querySelector('#est-name-error')?.textContent).toContain('Ya hay un establecimiento con ese nombre.');
  });

  it('emits cancelled from the Cancelar button', () => {
    const { el, component } = render();
    let cancelled = 0;
    component.cancelled.subscribe(() => cancelled++);
    el.querySelector<HTMLButtonElement>('.est-form__cancel')!.click();
    expect(cancelled).toBe(1);
  });
});
