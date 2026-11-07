import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { MorePage } from './more-page';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';

describe('MorePage', () => {
  const user: UserSummary = { id: 1, email: 'mariana@consultorio.com', fullName: 'Mariana Gómez', role: 'ADMIN', establishment: { id: 1, name: 'Consultorio Norte', kind: 'INSTITUTION' } };
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    logout = vi.fn();
    TestBed.configureTestingModule({
      imports: [MorePage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { currentUser: signal(user), logout } }],
    });
  });

  function render(): HTMLElement {
    const fixture = TestBed.createComponent(MorePage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the heading and the account card with initials, name, email and role', () => {
    const el = render();
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Más');
    const text = el.textContent ?? '';
    expect(text).toContain('MG');
    expect(text).toContain('Mariana Gómez');
    expect(text).toContain('mariana@consultorio.com');
    expect(text).toContain('Administrador/a');
  });

  it('logs out when "Cerrar sesión" is pressed', () => {
    const el = render();
    const button = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Cerrar sesión'));
    expect(button).toBeTruthy();
    button!.click();
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('shows admins the Administración section with links to Establecimiento and Usuarios', () => {
    const el = render();
    expect(el.textContent).toContain('Administración');
    expect(el.textContent).toContain('Esta sección solo la ven las administradoras.');
    const links = Array.from(el.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(links).toEqual(['/establecimiento', '/usuarios', '/catalogos', '/cambiar-contrasena']);
  });

  it('offers "Cambiar contraseña" in Mi cuenta', () => {
    const el = render();
    const link = el.querySelector<HTMLAnchorElement>('a[href="/cambiar-contrasena"]');
    expect(link?.textContent).toContain('Cambiar contraseña');
  });

  it('hides the Administración section from therapists', () => {
    TestBed.overrideProvider(AuthService, { useValue: { currentUser: signal({ ...user, role: 'TERAPEUTA' }), logout } });
    const el = render();
    expect(el.textContent).not.toContain('Administración');
    const links = Array.from(el.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(links).toEqual(['/cambiar-contrasena']);
  });

  it('shows the establishment card at the top for users with an establishment', () => {
    const el = render();
    const card = el.querySelector('.more__establishment');
    expect(card?.textContent).toContain('Establecimiento');
    expect(card?.textContent).toContain('Consultorio Norte');
    expect(card?.textContent).toContain('CN');
    expect(el.querySelector('.more__body')?.firstElementChild).toBe(card);
  });

  it('shows no establishment card nor Administración to a superadmin outside an establishment', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: { currentUser: signal({ ...user, role: 'SUPERADMIN', establishment: null }), logout },
    });
    const el = render();
    expect(el.querySelector('.more__establishment')).toBeNull();
    expect(el.textContent).not.toContain('Administración');
  });

  it('shows the Establecimiento row to a superadmin inside an establishment', () => {
    TestBed.overrideProvider(AuthService, { useValue: { currentUser: signal({ ...user, role: 'SUPERADMIN' }), logout } });
    const el = render();
    expect(el.querySelector('a[href="/establecimiento"]')).toBeTruthy();
    expect(el.querySelector('.more__establishment')).toBeTruthy();
  });

  it('shows therapists the establishment card but not the Establecimiento row', () => {
    TestBed.overrideProvider(AuthService, { useValue: { currentUser: signal({ ...user, role: 'TERAPEUTA' }), logout } });
    const el = render();
    expect(el.querySelector('.more__establishment')).toBeTruthy();
    expect(el.querySelector('a[href="/establecimiento"]')).toBeNull();
  });

  it('shows an independent therapist "Mi consultorio", no Usuarios and the role "Terapeuta independiente"', () => {
    const independent: UserSummary = {
      ...user, email: 'ana@gmail.com', fullName: 'Ana Pérez',
      establishment: { id: 7, name: 'Ana Pérez', kind: 'INDIVIDUAL' },
    };
    TestBed.overrideProvider(AuthService, { useValue: { currentUser: signal(independent), logout } });
    const el = render();
    expect(el.querySelector('.more__establishment-label')?.textContent?.trim()).toBe('Mi consultorio');
    expect(el.querySelector('.more__establishment-name')?.textContent?.trim()).toBe('Ana Pérez');
    expect(el.querySelector('.more__account-email')?.textContent).toContain('ana@gmail.com');
    expect(el.querySelector('.more__role')?.textContent?.trim()).toBe('Terapeuta independiente');
    const links = Array.from(el.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(links).toEqual(['/establecimiento', '/catalogos', '/cambiar-contrasena']);
    expect(el.querySelector('a[href="/establecimiento"]')?.textContent).toContain('Mi consultorio');
    expect(el.textContent).not.toContain('Usuarios');
    expect(el.textContent).toContain('Nombre y datos');
    expect(el.textContent).toContain('Segmentos, cirugías y cicatrices');
  });

  it('keeps an institution admin unchanged', () => {
    const el = render();
    expect(el.querySelector('.more__establishment-label')?.textContent?.trim()).toBe('Establecimiento');
    expect(el.querySelector('.more__role')?.textContent?.trim()).toBe('Administrador/a');
    expect(el.querySelector('a[href="/establecimiento"]')?.textContent).toContain('Establecimiento');
    expect(el.querySelector('a[href="/usuarios"]')).toBeTruthy();
  });
});
