import { vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { LoginPage } from './login-page';
import { AuthService } from '../../../core/auth/auth.service';
import { UserSummary } from '../../../core/auth/auth.models';

const user: UserSummary = { id: 1, email: 'ana@consultorio.com', fullName: 'Ana Pérez', role: 'TERAPEUTA', establishment: { id: 1, name: 'Consultorio Norte', kind: 'INSTITUTION' } };

function setup(query: Record<string, string> = {}) {
  const login = vi.fn();
  TestBed.configureTestingModule({
    imports: [LoginPage],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { login } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } },
    ],
  });
  const fixture = TestBed.createComponent(LoginPage);
  fixture.detectChanges();
  return { fixture, login, el: fixture.nativeElement as HTMLElement };
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

function submit(fixture: ComponentFixture<LoginPage>) {
  (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
  fixture.detectChanges();
}

describe('LoginPage', () => {
  it('renders the copy from the mockup', () => {
    const { el } = setup();
    expect(el.textContent).toContain('Acceso del equipo');
    expect(el.querySelector('h1')?.textContent).toContain('Hola de nuevo');
    expect(el.textContent).toContain('Ingresá con tu email y contraseña para ver a tus pacientes.');
    expect(el.textContent).toContain('¿Problemas para ingresar? Pedile ayuda al administrador de tu equipo.');
    expect(el.querySelector('input#email')?.getAttribute('placeholder')).toBe('nombre@consultorio.com');
    expect(el.querySelector('input#password')?.getAttribute('autocomplete')).toBe('current-password');
  });

  it('submitting empty fields shows the incomplete banner and does not call the API', () => {
    const { fixture, login, el } = setup();
    submit(fixture);
    expect(login).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Completá tu email y contraseña');
    expect(el.querySelector('#email')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.querySelector('#password')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('with only the password empty, only the password is marked', () => {
    const { fixture, login, el } = setup();
    type(el, '#email', 'ana@consultorio.com');
    submit(fixture);
    expect(login).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Completá tu email y contraseña');
    expect(el.querySelector('#email')?.getAttribute('aria-invalid')).not.toBe('true');
    expect(el.querySelector('#password')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('a whitespace-only email counts as missing and is marked', () => {
    const { fixture, login, el } = setup();
    typeRaw(el, '#email', '   ');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(login).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Completá tu email y contraseña');
    expect(el.querySelector('#email')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.querySelector('#password')?.getAttribute('aria-invalid')).not.toBe('true');
  });

  it('a valid email with a trailing space logs in with the trimmed email', () => {
    const { fixture, login, el } = setup();
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    login.mockReturnValue(of(user));
    typeRaw(el, '#email', 'ana@consultorio.com ');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(login).toHaveBeenCalledWith('ana@consultorio.com', 'secreta');
  });

  it('a failed submit moves focus to the error banner', async () => {
    const { fixture, el } = setup();
    document.body.appendChild(el);
    try {
      submit(fixture);
      await fixture.whenStable();
      const banner = el.querySelector('[role="alert"]');
      expect(banner).not.toBeNull();
      expect(document.activeElement).toBe(banner);
    } finally {
      el.remove();
    }
  });

  it('a malformed email shows the format banner without calling the API', () => {
    const { fixture, login, el } = setup();
    type(el, '#email', 'no-es-email');
    type(el, '#password', 'algo');
    submit(fixture);
    expect(login).not.toHaveBeenCalled();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Revisá el formato del email');
  });

  it('valid credentials log in and navigate to /pacientes', () => {
    const { fixture, login, el } = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    login.mockReturnValue(of(user));
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(login).toHaveBeenCalledWith('ana@consultorio.com', 'secreta');
    expect(navigate).toHaveBeenCalledWith('/pacientes');
  });

  it('while loading the button is disabled, busy and says "Ingresando…"', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(new Subject<UserSummary>());
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    const button = el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute('aria-busy')).toBe('true');
    expect(button.textContent).toContain('Ingresando…');
    submit(fixture);
    expect(login).toHaveBeenCalledTimes(1);
  });

  it('401 shows the invalid-credentials banner, marks both fields, and typing clears it', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401, error: { code: 'AUTHENTICATION_FAILED' } })));
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'mala');
    submit(fixture);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Email o contraseña incorrectos');
    expect(el.querySelector('#email')?.classList).toContain('field__input--invalid');
    expect(el.querySelector('#password')?.classList).toContain('field__input--invalid');

    type(el, '#password', 'mala2');
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('serverErrorShowsConnectionBannerAndReenablesButton', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos conectar con el servidor');
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('403 ESTABLISHMENT_INACTIVE shows the inactive-establishment banner, not the connection one', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { code: 'ESTABLISHMENT_INACTIVE', message: 'x' } })),
    );
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    const alert = el.querySelector('[role="alert"]')?.textContent;
    expect(alert).toContain('Tu establecimiento está inactivo');
    expect(alert).toContain('Pedile al administrador de la plataforma que lo reactive.');
    expect(alert).not.toContain('No pudimos conectar con el servidor');
    expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('explains a pending account', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { code: 'ESTABLISHMENT_PENDING_APPROVAL', message: 'x' } })),
    );
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    const alert = el.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Tu cuenta está pendiente de aprobación');
    expect(alert?.textContent).toContain('Vas a poder ingresar cuando el administrador de la plataforma la apruebe.');
    expect(alert?.classList).toContain('alert--warning');
  });

  it('the pending banner shows a clock icon, the others an exclamation', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { code: 'ESTABLISHMENT_PENDING_APPROVAL', message: 'x' } })),
    );
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(el.querySelector('[role="alert"] svg path[d="M12 7v5l3 2"]')).not.toBeNull();

    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    type(el, '#password', 'secreta2');
    submit(fixture);
    expect(el.querySelector('[role="alert"] svg path[d="M12 7v5l3 2"]')).toBeNull();
  });

  it('explains an expired trial', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { code: 'TRIAL_EXPIRED', message: 'x' } })),
    );
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    const alert = el.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Tu período de prueba venció');
    expect(alert?.textContent).toContain('Contactá al administrador de la plataforma.');
    expect(alert?.classList).toContain('alert--error');
  });

  it('the expired-trial banner shows an hourglass icon', () => {
    const { fixture, login, el } = setup();
    login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { code: 'TRIAL_EXPIRED', message: 'x' } })),
    );
    type(el, '#email', 'ana@consultorio.com');
    type(el, '#password', 'secreta');
    submit(fixture);
    expect(el.querySelector('[role="alert"] svg')?.getAttribute('data-icon')).toBe('hourglass');
  });

  it('opening /login?prueba=vencida shows the expired-trial banner', () => {
    const { el } = setup({ prueba: 'vencida' });
    const alert = el.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Tu período de prueba venció');
    expect(alert?.querySelector('svg')?.getAttribute('data-icon')).toBe('hourglass');
  });

  it('links to the registration', () => {
    const { el } = setup();
    const link = el.querySelector<HTMLAnchorElement>('a[href="/registro"]');
    expect(link?.textContent).toContain('¿Sos terapeuta independiente? Creá tu cuenta');
  });

  it('shows the expired-session notice when arriving with ?sesion=vencida', () => {
    const { el } = setup({ sesion: 'vencida' });
    const notice = el.querySelector('[role="status"]');
    expect(notice?.textContent).toContain('Tu sesión venció');
    expect(notice?.textContent).toContain('Por seguridad, ingresá de nuevo para continuar.');
  });

  it('the eye button toggles password visibility and its label', () => {
    const { fixture, el } = setup();
    const eye = el.querySelector<HTMLButtonElement>('button[aria-label="Mostrar contraseña"]')!;
    eye.click();
    fixture.detectChanges();
    expect(el.querySelector('#password')?.getAttribute('type')).toBe('text');
    expect(eye.getAttribute('aria-label')).toBe('Ocultar contraseña');
    expect(eye.getAttribute('aria-pressed')).toBe('true');
  });
});
