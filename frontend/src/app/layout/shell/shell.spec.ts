import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Shell } from './shell';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';

describe('Shell', () => {
  const user: UserSummary = { id: 1, email: 'm@c.com', fullName: 'Mariana Gómez', role: 'ADMIN' };
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

  it('side nav shows Pacientes and Plantillas; tab bar adds Más', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const navs = el.querySelectorAll('nav[aria-label="Navegación principal"]');
    expect(navs.length).toBe(2);
    expect(navLabels(el.querySelector('app-side-nav nav')!)).toEqual(['Pacientes', 'Plantillas']);
    expect(navLabels(el.querySelector('app-tab-bar nav')!)).toEqual(['Pacientes', 'Plantillas', 'Más']);
    expect(el.querySelector('app-tab-bar a[href="/mas"]')).toBeTruthy();
    navs.forEach((nav) => expect(nav.querySelector('a[href="/pacientes"]')).toBeTruthy());
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
});
