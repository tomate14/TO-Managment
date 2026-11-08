import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Shell } from './shell';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';

describe('Shell', () => {
  const user: UserSummary = { id: 1, email: 'm@c.com', fullName: 'Mariana Gómez', role: 'ADMIN', establishment: { id: 1, name: 'Consultorio Norte', kind: 'INSTITUTION' } };
  let refresh: ReturnType<typeof vi.fn>;
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    refresh = vi.fn();
    logout = vi.fn();
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal(user), refreshCurrentUser: refresh, logout } },
      ],
    });
  });

  it('refreshes the current user once on start', () => {
    TestBed.createComponent(Shell).detectChanges();
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  function navLabels(nav: Element): (string | undefined)[] {
    return Array.from(nav.querySelectorAll('a')).map((a) => a.textContent?.trim());
  }

  it('side nav shows Pacientes, Agenda, Plantillas and, for admins, Usuarios; tab bar adds Más', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const navs = el.querySelectorAll('nav[aria-label="Navegación principal"]');
    expect(navs.length).toBe(2);
    expect(navLabels(el.querySelector('app-side-nav nav')!))
      .toEqual(['Pacientes', 'Agenda', 'Plantillas', 'Establecimiento', 'Usuarios', 'Catálogos', 'Cambiar contraseña']);
    expect(el.querySelector('app-side-nav a[href="/cambiar-contrasena"]')).toBeTruthy();
    expect(el.querySelector('app-side-nav')?.textContent).toContain('Administración');
    expect(navLabels(el.querySelector('app-tab-bar nav')!)).toEqual(['Pacientes', 'Agenda', 'Plantillas', 'Más']);
    expect(el.querySelector('app-tab-bar a[href="/mas"]')).toBeTruthy();
    navs.forEach((nav) => expect(nav.querySelector('a[href="/pacientes"]')).toBeTruthy());
  });

  it('hides the Administración section from therapists', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: { currentUser: signal({ ...user, role: 'TERAPEUTA' }), refreshCurrentUser: refresh, logout },
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(navLabels(el.querySelector('app-side-nav nav')!)).toEqual(['Pacientes', 'Agenda', 'Plantillas', 'Cambiar contraseña']);
    expect(el.querySelector('app-side-nav')?.textContent).not.toContain('Administración');
  });

  it('side nav logs out when "Cerrar sesión" is pressed', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('app-side-nav button');
    expect(button?.textContent).toContain('Cerrar sesión');
    button!.click();
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('shows the user card with initials, name and role label', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('MG');
    expect(text).toContain('Mariana Gómez');
    expect(text).toContain('Administrador/a');
  });

  it('has a main landmark with the router outlet', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('main router-outlet')).toBeTruthy();
  });

  const superadmin: UserSummary = { id: 9, email: 'root@c.com', fullName: 'Maximiliano Roselli', role: 'SUPERADMIN', establishment: null };

  function renderAs(current: UserSummary): HTMLElement {
    TestBed.overrideProvider(AuthService, {
      useValue: {
        currentUser: signal(current),
        establishment: signal(current.establishment),
        refreshCurrentUser: refresh,
        logout,
        enterEstablishment: vi.fn(),
      },
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('superadmin outside sees only Establecimientos in the menu', () => {
    const el = renderAs(superadmin);
    expect(navLabels(el.querySelector('app-side-nav nav')!)).toEqual(['Establecimientos', 'Cambiar contraseña']);
    expect(navLabels(el.querySelector('app-tab-bar nav')!)).toEqual(['Establecimientos', 'Más']);
    expect(el.querySelector('app-establishment-banner')).toBeNull();
    expect(el.querySelector('app-side-nav')?.textContent).not.toContain('Administración');
    expect(el.querySelector('app-side-nav')?.textContent).toContain('Superadmin');
  });

  it('superadmin inside sees the banner, the establishment under the logo and the admin section', () => {
    const el = renderAs({ ...superadmin, establishment: { id: 3, name: 'Norte', kind: 'INSTITUTION' } });
    expect(el.querySelector('app-establishment-banner')?.textContent).toContain('Estás en Norte');
    expect(el.querySelector('.side-nav__establishment')?.textContent).toContain('Norte');
    expect(navLabels(el.querySelector('app-side-nav nav')!)).toEqual([
      'Pacientes', 'Agenda', 'Plantillas', 'Establecimiento', 'Usuarios', 'Catálogos', 'Cambiar contraseña',
    ]);
  });

  it('shows the establishment name to the establishment admin without a banner', () => {
    const el = renderAs(user);
    expect(el.querySelector('.side-nav__establishment')?.textContent).toContain('Consultorio Norte');
    expect(el.querySelector('app-establishment-banner')).toBeNull();
  });
});
