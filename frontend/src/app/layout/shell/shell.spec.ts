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

  beforeEach(() => {
    refresh = vi.fn();
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal(user), refreshCurrentUser: refresh } },
      ],
    });
  });

  it('refreshes the current user once on start', () => {
    TestBed.createComponent(Shell).detectChanges();
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('shows only the Pacientes and Plantillas destinations in both navs', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const navs = el.querySelectorAll('nav[aria-label="Navegación principal"]');
    expect(navs.length).toBe(2);
    navs.forEach((nav) => {
      const labels = Array.from(nav.querySelectorAll('a')).map((a) => a.textContent?.trim());
      expect(labels).toEqual(['Pacientes', 'Plantillas']);
      expect(nav.querySelector('a[href="/pacientes"]')).toBeTruthy();
    });
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
