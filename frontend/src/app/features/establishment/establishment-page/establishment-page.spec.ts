import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EstablishmentPage } from './establishment-page';
import { CurrentEstablishmentService } from '../current-establishment.service';
import { AuthService } from '../../../core/auth/auth.service';
import { UserSummary } from '../../../core/auth/auth.models';
import { signal } from '@angular/core';

describe('EstablishmentPage', () => {
  const establishment = { id: 3, name: 'Consultorio Norte', activeUsers: 4, createdDate: '2026-11-06' };
  const admin: UserSummary = {
    id: 1, email: 'm@c.com', fullName: 'Mariana Gómez', role: 'ADMIN',
    establishment: { id: 3, name: 'Consultorio Norte', kind: 'INSTITUTION' },
  };
  let get: ReturnType<typeof vi.fn>;
  let rename: ReturnType<typeof vi.fn>;
  let refreshCurrentUser: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    get = vi.fn().mockReturnValue(of(establishment));
    rename = vi.fn().mockReturnValue(of({ ...establishment, name: 'Consultorio Sur' }));
    refreshCurrentUser = vi.fn();
    TestBed.configureTestingModule({
      imports: [EstablishmentPage],
      providers: [
        provideRouter([]),
        { provide: CurrentEstablishmentService, useValue: { get, rename } },
        { provide: AuthService, useValue: { refreshCurrentUser, currentUser: signal(admin) } },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(EstablishmentPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function input(el: HTMLElement): HTMLInputElement {
    return el.querySelector<HTMLInputElement>('#establishment-name')!;
  }

  function type(el: HTMLElement, value: string): void {
    const field = input(el);
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  function submit(fixture: ReturnType<typeof render>['fixture']): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('loads the name, the active users and the creation date', () => {
    const { el } = render();
    expect(input(el).value).toBe('Consultorio Norte');
    expect(el.textContent).toContain('4 activos');
    expect(el.textContent).toContain('06/11/2026');
  });

  it('validates an empty name and does not save', () => {
    const { fixture, el } = render();
    type(el, '   ');
    submit(fixture);
    expect(el.textContent).toContain('Ingresá el nombre del establecimiento.');
    expect(rename).not.toHaveBeenCalled();
  });

  it('saves the trimmed name and refreshes the current user', () => {
    const { fixture, el } = render();
    type(el, '  Consultorio Sur ');
    submit(fixture);
    expect(rename).toHaveBeenCalledWith('Consultorio Sur');
    expect(refreshCurrentUser).toHaveBeenCalledTimes(1);
    expect(el.textContent).toContain('Cambios guardados.');
  });

  it('maps ESTABLISHMENT_NAME_TAKEN to the name field', () => {
    rename.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'ESTABLISHMENT_NAME_TAKEN', message: 'x' } })),
    );
    const { fixture, el } = render();
    type(el, 'Otro');
    submit(fixture);
    expect(el.querySelector('#establishment-name-error')?.textContent).toContain('Ya hay un establecimiento con ese nombre.');
    expect(refreshCurrentUser).not.toHaveBeenCalled();
  });

  it('titles the page "Establecimiento" for an institution admin', () => {
    const { el } = render();
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Establecimiento');
  });

  it('titles the page "Mi consultorio" for an independent therapist', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: {
        refreshCurrentUser,
        currentUser: signal({ ...admin, establishment: { id: 7, name: 'Ana Pérez', kind: 'INDIVIDUAL' } }),
      },
    });
    const { el } = render();
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Mi consultorio');
  });
});
