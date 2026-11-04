import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { UsersPage } from './users-page';
import { UsersService } from '../users.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ManagedUser } from '../users.models';

describe('UsersPage', () => {
  const me: ManagedUser = { id: 1, email: 'mariana@c.com', fullName: 'Mariana Gómez', role: 'ADMIN', active: true };
  const carla: ManagedUser = { id: 2, email: 'carla@c.com', fullName: 'Carla Ríos', role: 'TERAPEUTA', active: true };
  const sofia: ManagedUser = { id: 3, email: 'sofia@c.com', fullName: 'Sofía Núñez', role: 'TERAPEUTA', active: false };
  let usersService: { list: ReturnType<typeof vi.fn>; setActive: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    usersService = { list: vi.fn(() => of([me, carla, sofia])), setActive: vi.fn() };
    TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        provideRouter([]),
        { provide: UsersService, useValue: usersService },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: me.email, fullName: me.fullName, role: 'ADMIN' }) } },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(UsersPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function card(el: HTMLElement, name: string): HTMLElement {
    return Array.from(el.querySelectorAll<HTMLElement>('.user-card')).find((c) => c.textContent?.includes(name))!;
  }

  it('lists every user with role and status badges', () => {
    const { el } = render();
    expect(el.querySelectorAll('.user-card').length).toBe(3);
    expect(card(el, 'Carla Ríos').textContent).toContain('Terapeuta');
    expect(card(el, 'Carla Ríos').textContent).toContain('Activo');
    expect(card(el, 'Sofía Núñez').textContent).toContain('Inactivo');
    expect(card(el, 'Mariana Gómez').textContent).toContain('Admin');
  });

  it('marks the signed-in admin with (vos) and offers no switch for her', () => {
    const { el } = render();
    const mine = card(el, 'Mariana Gómez');
    expect(mine.textContent).toContain('(vos)');
    expect(mine.textContent).toContain('No podés desactivarte');
    expect(mine.querySelector('[role="switch"]')).toBeNull();
  });

  it('links to the new-user form and to each edit form', () => {
    const { el } = render();
    expect(el.querySelector('a[href="/usuarios/nuevo"]')?.textContent).toContain('Nuevo');
    expect(card(el, 'Carla Ríos').querySelector('a[href="/usuarios/2"]')).toBeTruthy();
  });

  it('deactivates a user from the switch and shows the new state', () => {
    usersService.setActive.mockReturnValue(of({ ...carla, active: false }));
    const { fixture, el } = render();
    const toggle = card(el, 'Carla Ríos').querySelector<HTMLButtonElement>('[role="switch"]')!;
    expect(toggle.getAttribute('aria-checked')).toBe('true');

    toggle.click();
    fixture.detectChanges();

    expect(usersService.setActive).toHaveBeenCalledWith(2, false);
    expect(card(el, 'Carla Ríos').textContent).toContain('Inactivo');
    expect(card(el, 'Carla Ríos').querySelector('[role="switch"]')?.getAttribute('aria-checked')).toBe('false');
  });

  it('shows the backend message when the change fails', () => {
    usersService.setActive.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'SELF_DEACTIVATION_FORBIDDEN', message: 'No podés desactivarte.' },
    })));
    const { fixture, el } = render();
    card(el, 'Carla Ríos').querySelector<HTMLButtonElement>('[role="switch"]')!.click();
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No podés desactivarte.');
    expect(card(el, 'Carla Ríos').textContent).toContain('Activo');
  });

  it('shows an error when the list cannot be loaded', () => {
    usersService.list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const { el } = render();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los usuarios');
  });
});
