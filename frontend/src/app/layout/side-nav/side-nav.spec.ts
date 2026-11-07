import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { SideNav } from './side-nav';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';
import { EstablishmentsService } from '../../features/establishments/establishments.service';

describe('SideNav', () => {
  const superadmin: UserSummary = { id: 1, email: 'm@c.com', fullName: 'Maximiliano Roselli', role: 'SUPERADMIN', establishment: null };
  const pendingCount = signal(0);

  beforeEach(() => {
    pendingCount.set(0);
    TestBed.configureTestingModule({
      imports: [SideNav],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal(superadmin), logout: () => undefined } },
        { provide: EstablishmentsService, useValue: { pendingCount } },
      ],
    });
  });

  function link(el: HTMLElement): HTMLElement {
    return el.querySelector<HTMLElement>('a[href="/establecimientos"]')!;
  }

  it('shows the pending count next to Establecimientos', () => {
    pendingCount.set(2);
    const fixture = TestBed.createComponent(SideNav);
    fixture.detectChanges();
    const badge = link(fixture.nativeElement).querySelector('.side-nav__badge');
    expect(badge?.querySelector('[aria-hidden="true"]')?.textContent?.trim()).toBe('2');
    expect(badge?.querySelector('.visually-hidden')?.textContent).toContain('2 pendientes');
  });

  it('shows no badge when nothing is pending', () => {
    const fixture = TestBed.createComponent(SideNav);
    fixture.detectChanges();
    expect(link(fixture.nativeElement).querySelector('.side-nav__badge')).toBeNull();
  });

  const institutionAdmin: UserSummary = {
    id: 2, email: 'm@c.com', fullName: 'Mariana Gómez', role: 'ADMIN',
    establishment: { id: 1, name: 'Consultorio Norte', kind: 'INSTITUTION' },
  };
  const independent: UserSummary = {
    id: 3, email: 'ana@gmail.com', fullName: 'Ana Pérez', role: 'ADMIN',
    establishment: { id: 7, name: 'Ana Pérez', kind: 'INDIVIDUAL' },
  };

  function renderAs(user: UserSummary): HTMLElement {
    TestBed.overrideProvider(AuthService, { useValue: { currentUser: signal(user), logout: () => undefined } });
    const fixture = TestBed.createComponent(SideNav);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function labels(el: HTMLElement): (string | undefined)[] {
    return Array.from(el.querySelectorAll('a')).map((a) => a.textContent?.trim());
  }

  it('shows an independent therapist "Mi consultorio", no Usuarios and the role "Terapeuta independiente"', () => {
    const el = renderAs(independent);
    expect(el.querySelector('.side-nav__establishment-label')?.textContent?.trim()).toBe('Mi consultorio');
    expect(el.querySelector('.side-nav__establishment-name')?.textContent?.trim()).toBe('Ana Pérez');
    expect(labels(el)).toEqual(['Pacientes', 'Agenda', 'Plantillas', 'Mi consultorio', 'Catálogos', 'Cambiar contraseña']);
    expect(el.querySelector('a[href="/usuarios"]')).toBeNull();
    expect(el.querySelector('.side-nav__user-role')?.textContent?.trim()).toBe('Terapeuta independiente');
  });

  it('keeps an institution admin unchanged', () => {
    const el = renderAs(institutionAdmin);
    expect(el.querySelector('.side-nav__establishment-label')?.textContent?.trim()).toBe('Establecimiento');
    expect(labels(el)).toEqual([
      'Pacientes', 'Agenda', 'Plantillas', 'Establecimiento', 'Usuarios', 'Catálogos', 'Cambiar contraseña',
    ]);
    expect(el.querySelector('.side-nav__user-role')?.textContent?.trim()).toBe('Administrador/a');
  });

  it('keeps a therapist unchanged', () => {
    const el = renderAs({ ...institutionAdmin, role: 'TERAPEUTA' });
    expect(el.querySelector('.side-nav__establishment-label')?.textContent?.trim()).toBe('Establecimiento');
    expect(labels(el)).toEqual(['Pacientes', 'Agenda', 'Plantillas', 'Cambiar contraseña']);
    expect(el.querySelector('.side-nav__user-role')?.textContent?.trim()).toBe('Terapeuta');
  });

  it('keeps a superadmin unchanged', () => {
    const el = renderAs(superadmin);
    expect(el.querySelector('.side-nav__establishment')).toBeNull();
    expect(el.querySelector('.side-nav__user-role')?.textContent?.trim()).toBe('Superadmin');
  });
});
