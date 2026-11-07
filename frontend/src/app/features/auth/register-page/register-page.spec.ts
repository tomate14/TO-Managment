import { vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterPage } from './register-page';
import { RegistrationService } from '../registration.service';

function setup() {
  const register = vi.fn();
  TestBed.configureTestingModule({
    imports: [RegisterPage],
    providers: [provideRouter([]), { provide: RegistrationService, useValue: { register } }],
  });
  const fixture = TestBed.createComponent(RegisterPage);
  fixture.detectChanges();
  return { fixture, register, el: fixture.nativeElement as HTMLElement };
}

function type(el: HTMLElement, selector: string, value: string) {
  const input = el.querySelector<HTMLInputElement>(selector)!;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

/** Bypasses jsdom's type=email value sanitization (which strips whitespace) to emit the raw value. */
function typeRaw(el: HTMLElement, selector: string, value: string) {
  const input = el.querySelector<HTMLInputElement>(selector)!;
  Object.defineProperty(input, 'value', { value, configurable: true });
  input.dispatchEvent(new Event('input'));
}

function fill(el: HTMLElement, overrides: Partial<Record<'fullName' | 'email' | 'password' | 'confirm', string>> = {}) {
  const values = { fullName: 'Ana Pérez', email: 'ana@gmail.com', password: 'secreta123', confirm: 'secreta123', ...overrides };
  type(el, '#fullName', values.fullName);
  type(el, '#email', values.email);
  type(el, '#password', values.password);
  type(el, '#confirm', values.confirm);
}

function submit(fixture: ComponentFixture<RegisterPage>) {
  (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
  fixture.detectChanges();
}

describe('RegisterPage', () => {
  it('renders the copy from the mockup', () => {
    const { el } = setup();
    expect(el.textContent).toContain('Terapeuta independiente');
    expect(el.querySelector('h1')?.textContent).toContain('Creá tu cuenta');
    expect(el.textContent).toContain('Trabajá con tu propio consultorio: tus pacientes, tus plantillas y tu agenda.');
    expect(el.textContent).toContain('Mínimo 8 caracteres.');
    expect(el.textContent).toContain('Enviar solicitud');
    expect(el.querySelector('a[href="/login"]')?.textContent).toContain('Iniciá sesión');
  });

  it('blocks submit until the form is valid and the passwords match', () => {
    const { fixture, register, el } = setup();
    submit(fixture);
    expect(register).not.toHaveBeenCalled();

    fill(el, { confirm: 'otra-cosa123' });
    submit(fixture);
    expect(register).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Las contraseñas no coinciden.');
  });

  it('trims name and email but not the password', () => {
    const { fixture, register, el } = setup();
    register.mockReturnValue(of(undefined));
    type(el, '#fullName', ' Ana ');
    typeRaw(el, '#email', ' ana@gmail.com ');
    type(el, '#password', ' secreta123 ');
    type(el, '#confirm', ' secreta123 ');
    submit(fixture);
    expect(register).toHaveBeenCalledWith({ fullName: 'Ana', email: 'ana@gmail.com', password: ' secreta123 ' });
  });

  it('rejects a short password and a blank name', () => {
    const { fixture, register, el } = setup();
    fill(el, { fullName: '   ', password: 'corta', confirm: 'corta' });
    submit(fixture);
    expect(register).not.toHaveBeenCalled();
    expect(el.querySelector('#fullName')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.querySelector('#password')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the email error on 409 USER_EMAIL_TAKEN', () => {
    const { fixture, register, el } = setup();
    register.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'USER_EMAIL_TAKEN', message: 'x' } })),
    );
    fill(el);
    submit(fixture);
    const email = el.querySelector('#email')!.closest('.field')!;
    expect(email.textContent).toContain('Ya hay una cuenta con este email.');
    expect(el.querySelector('#email')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the confirmation after 201', () => {
    const { fixture, register, el } = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    register.mockReturnValue(of(undefined));
    fill(el);
    submit(fixture);
    expect(el.textContent).toContain('Recibimos tu solicitud');
    expect(el.textContent).toContain('ana@gmail.com');
    const back = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Volver al inicio de sesión'))!;
    back.click();
    expect(navigate).toHaveBeenCalledWith('/login');
  });

  it('uses the register hero', () => {
    const { el } = setup();
    expect(el.querySelector('.hero--register')).not.toBeNull();
    expect(el.textContent).toContain('Tu consultorio,');
    expect(el.textContent).not.toContain('Cada sesión, cada segmento, cada avance.');
  });

  it('the eye button toggles visibility of both password fields', () => {
    const { fixture, el } = setup();
    const eye = el.querySelector<HTMLButtonElement>('button[aria-label="Mostrar contraseña"]')!;
    eye.click();
    fixture.detectChanges();
    expect(el.querySelector('#password')?.getAttribute('type')).toBe('text');
    expect(el.querySelector('#confirm')?.getAttribute('type')).toBe('text');
    expect(eye.getAttribute('aria-label')).toBe('Ocultar contraseña');
    expect(eye.getAttribute('aria-pressed')).toBe('true');
  });

  it('shows the too-many message on 429', () => {
    const { fixture, register, el } = setup();
    register.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 429, error: { code: 'TOO_MANY_REQUESTS' } })));
    fill(el);
    submit(fixture);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Demasiados intentos. Probá de nuevo más tarde.');
  });
});
