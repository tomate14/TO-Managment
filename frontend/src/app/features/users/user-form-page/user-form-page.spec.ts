import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { UserFormPage } from './user-form-page';
import { UsersService } from '../users.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ManagedUser } from '../users.models';

describe('UserFormPage', () => {
  const carla: ManagedUser = { id: 2, email: 'carla@c.com', fullName: 'Carla Ríos', role: 'TERAPEUTA', active: true };
  let usersService: {
    get: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  function setup(id: string | null) {
    usersService = { get: vi.fn(() => of(carla)), create: vi.fn(() => of(carla)), update: vi.fn(() => of(carla)) };
    TestBed.configureTestingModule({
      imports: [UserFormPage],
      providers: [
        provideRouter([]),
        { provide: UsersService, useValue: usersService },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'm@c.com', fullName: 'Mariana', role: 'ADMIN' }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(UserFormPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, navigate };
  }

  function type(el: HTMLElement, selector: string, value: string) {
    const input = el.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(fixture: ReturnType<typeof setup>['fixture']) {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  describe('new user', () => {
    it('shows name, email, initial password and both roles, with Terapeuta preselected', () => {
      const { el } = setup(null);
      expect(el.querySelector('h1')?.textContent).toContain('Nuevo usuario');
      expect(el.querySelector('#user-password')).toBeTruthy();
      const radios = el.querySelectorAll<HTMLInputElement>('input[type="radio"]');
      expect(radios.length).toBe(2);
      expect(radios[0].checked).toBe(true);
      expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Crear usuario');
    });

    it('does not submit and shows field errors when data is missing or invalid', () => {
      const { fixture, el } = setup(null);
      type(el, '#user-email', 'no-es-email');
      type(el, '#user-password', 'corta');
      submit(fixture);

      expect(usersService.create).not.toHaveBeenCalled();
      expect(el.textContent).toContain('Ingresá el nombre.');
      expect(el.textContent).toContain('Ingresá un email válido');
      expect(el.textContent).toContain('al menos 8 caracteres');
    });

    it('creates the user with trimmed values and goes back to the list', () => {
      const { fixture, el, navigate } = setup(null);
      type(el, '#user-name', ' Carla Ríos ');
      type(el, '#user-email', ' carla@c.com ');
      type(el, '#user-password', 'clave-inicial');
      el.querySelectorAll<HTMLInputElement>('input[type="radio"]')[1].click();
      submit(fixture);

      expect(usersService.create).toHaveBeenCalledWith({
        email: 'carla@c.com', fullName: 'Carla Ríos', password: 'clave-inicial', role: 'ADMIN',
      });
      expect(navigate).toHaveBeenCalledWith('/usuarios');
    });

    it('shows "Ya hay un usuario con este email." under the email on a duplicate', () => {
      const { fixture, el, navigate } = setup(null);
      usersService.create.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 409, error: { code: 'USER_EMAIL_TAKEN', message: 'Ya hay un usuario con este email.' },
      })));
      type(el, '#user-name', 'Carla Ríos');
      type(el, '#user-email', 'carla@c.com');
      type(el, '#user-password', 'clave-inicial');
      submit(fixture);

      expect(el.querySelector('#user-email-error')?.textContent).toContain('Ya hay un usuario con este email.');
      expect(el.querySelector('#user-email')?.getAttribute('aria-invalid')).toBe('true');
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('edit user', () => {
    it('loads the user, locks the email and hides the password', () => {
      const { el } = setup('2');
      expect(usersService.get).toHaveBeenCalledWith(2);
      expect(el.querySelector('h1')?.textContent).toContain('Editar usuario');
      expect(el.querySelector<HTMLInputElement>('#user-name')?.value).toBe('Carla Ríos');
      expect(el.querySelector<HTMLInputElement>('#user-email')?.disabled).toBe(true);
      expect(el.querySelector('#user-password')).toBeNull();
      expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Guardar cambios');
    });

    it('saves name and role', () => {
      const { fixture, el, navigate } = setup('2');
      type(el, '#user-name', 'Carla R. Ríos');
      submit(fixture);

      expect(usersService.update).toHaveBeenCalledWith(2, { fullName: 'Carla R. Ríos', role: 'TERAPEUTA' });
      expect(navigate).toHaveBeenCalledWith('/usuarios');
    });

    it('does not let the admin change her own role', () => {
      const { el } = setup('1');
      el.querySelectorAll<HTMLInputElement>('input[type="radio"]').forEach((radio) => expect(radio.disabled).toBe(true));
      expect(el.textContent).toContain('No podés cambiar tu propio rol.');
    });
  });
});
