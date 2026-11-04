import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MorePage } from './more-page';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';

describe('MorePage', () => {
  const user: UserSummary = { id: 1, email: 'mariana@consultorio.com', fullName: 'Mariana Gómez', role: 'ADMIN' };
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    logout = vi.fn();
    TestBed.configureTestingModule({
      imports: [MorePage],
      providers: [{ provide: AuthService, useValue: { currentUser: signal(user), logout } }],
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

  it('does not show links whose stories are pending', () => {
    const el = render();
    expect(el.querySelectorAll('a').length).toBe(0);
  });
});
