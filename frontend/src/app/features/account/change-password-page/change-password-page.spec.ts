import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ChangePasswordPage } from './change-password-page';
import { AuthService } from '../../../core/auth/auth.service';

describe('ChangePasswordPage', () => {
  let changePassword: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    changePassword = vi.fn(() => of(undefined));
    TestBed.configureTestingModule({
      imports: [ChangePasswordPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { changePassword } }],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(ChangePasswordPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { fixture, el, type, submit };
  }

  it('shows both password fields, the length hint and the save button', () => {
    const { el } = render();
    expect(el.querySelector('h1')?.textContent).toContain('Cambiar contraseña');
    expect(el.querySelector('#current-password')?.getAttribute('autocomplete')).toBe('current-password');
    expect(el.querySelector('#new-password')?.getAttribute('autocomplete')).toBe('new-password');
    expect(el.textContent).toContain('Mínimo 8 caracteres (0/8)');
    expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Guardar nueva contraseña');
  });

  it('counts characters and fills the strength bars as the new password grows', () => {
    const { el, type } = render();
    type('#new-password', 'abcd');
    expect(el.textContent).toContain('(4/8)');
    expect(el.querySelectorAll('.strength__bar--short').length).toBe(2);

    type('#new-password', 'nueva2026');
    expect(el.textContent).toContain('(8/8)');
    expect(el.querySelectorAll('.strength__bar--ok').length).toBe(3);
  });

  it('does not call the backend when a field is empty or the new password is short', () => {
    const { el, type, submit } = render();
    submit();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Completá las dos contraseñas');

    type('#current-password', 'actual-1234');
    type('#new-password', 'corta');
    submit();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('muy corta');
    expect(changePassword).not.toHaveBeenCalled();
  });

  it('saves the new password, confirms and clears the form', () => {
    const { el, type, submit } = render();
    type('#current-password', 'actual-1234');
    type('#new-password', 'nueva-12345');
    submit();

    expect(changePassword).toHaveBeenCalledWith('actual-1234', 'nueva-12345');
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Tu contraseña se actualizó');
    expect(el.querySelector<HTMLInputElement>('#current-password')?.value).toBe('');
    expect(el.querySelector<HTMLInputElement>('#new-password')?.value).toBe('');
  });

  it('shows the mockup error and marks the current password when it is wrong', () => {
    changePassword.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'CURRENT_PASSWORD_INCORRECT', message: 'La contraseña actual no es correcta.' },
    })));
    const { el, type, submit } = render();
    type('#current-password', 'equivocada');
    type('#new-password', 'nueva-12345');
    submit();

    const alert = el.querySelector('[role="alert"]')?.textContent ?? '';
    expect(alert).toContain('La contraseña actual no es correcta');
    expect(alert).toContain('No se guardó ningún cambio.');
    expect(el.querySelector('#current-password')?.getAttribute('aria-invalid')).toBe('true');
  });
});
