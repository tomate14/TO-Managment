import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { EstablishmentsPage } from './establishments-page';
import { EstablishmentsService } from '../establishments.service';
import { EstablishmentSummary } from '../establishments.models';
import { AuthService } from '../../../core/auth/auth.service';

describe('EstablishmentsPage', () => {
  const norte: EstablishmentSummary = {
    id: 1,
    name: 'Consultorio Norte',
    active: true,
    activeUsers: 4,
    admins: [{ id: 10, fullName: 'Mariana Gómez', email: 'mariana.gomez@cnorte.com' }],
  };
  const mano: EstablishmentSummary = {
    id: 2,
    name: 'Clínica de la Mano',
    active: false,
    activeUsers: 3,
    admins: [{ id: 11, fullName: 'Diego Medina', email: 'diego@clinicamano.com' }],
  };
  let service: { list: ReturnType<typeof vi.fn>; rename: ReturnType<typeof vi.fn>; setActive: ReturnType<typeof vi.fn> };
  let auth: { enterEstablishment: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    service = { list: vi.fn(() => of([norte, mano])), rename: vi.fn(), setActive: vi.fn() };
    auth = { enterEstablishment: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({
      imports: [EstablishmentsPage],
      providers: [
        provideRouter([]),
        { provide: EstablishmentsService, useValue: service },
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(EstablishmentsPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function card(el: HTMLElement, name: string): HTMLElement {
    return Array.from(el.querySelectorAll<HTMLElement>('.est-card')).find((c) => c.textContent?.includes(name))!;
  }

  it('lists every establishment with admin, users and status', () => {
    const { el } = render();
    expect(el.querySelectorAll('.est-card').length).toBe(2);
    const norteCard = card(el, 'Consultorio Norte');
    expect(norteCard.textContent).toContain('Mariana Gómez');
    expect(norteCard.textContent).toContain('mariana.gomez@cnorte.com');
    expect(norteCard.textContent).toContain('4 usuarios');
    expect(norteCard.textContent).toContain('Activo');
    expect(card(el, 'Clínica de la Mano').textContent).toContain('Inactivo');
  });

  it('renames inline from the pencil', () => {
    service.rename.mockReturnValue(of({ ...norte, name: 'Norte Kinesio' }));
    const { fixture, el } = render();
    card(el, 'Consultorio Norte').querySelector<HTMLButtonElement>('.est-icon-button')!.click();
    fixture.detectChanges();
    const input = el.querySelector<HTMLInputElement>('#est-edit-1')!;
    expect(input.value).toBe('Consultorio Norte');
    input.value = '  Norte Kinesio ';
    input.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('.est-edit')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(service.rename).toHaveBeenCalledWith(1, 'Norte Kinesio');
    expect(el.textContent).toContain('Norte Kinesio');
    expect(el.querySelector('.est-edit')).toBeNull();
  });

  it('deactivates from the switch and shows the new state', () => {
    service.setActive.mockReturnValue(of({ ...norte, active: false }));
    const { fixture, el } = render();
    const toggle = card(el, 'Consultorio Norte').querySelector<HTMLButtonElement>('[role="switch"]')!;
    expect(toggle.getAttribute('aria-checked')).toBe('true');
    toggle.click();
    fixture.detectChanges();
    expect(service.setActive).toHaveBeenCalledWith(1, false);
    expect(card(el, 'Consultorio Norte').textContent).toContain('Inactivo');
  });

  it('has no Entrar button for an inactive establishment', () => {
    const { el } = render();
    expect(card(el, 'Clínica de la Mano').querySelector('.est-enter')).toBeNull();
    expect(card(el, 'Clínica de la Mano').textContent).toContain('Sus usuarios no pueden ingresar');
  });

  it('enters an active establishment and goes to the patients list', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const { el } = render();
    card(el, 'Consultorio Norte').querySelector<HTMLButtonElement>('.est-enter')!.click();
    expect(auth.enterEstablishment).toHaveBeenCalledWith(1);
    expect(navigate).toHaveBeenCalledWith('/pacientes');
  });

  it('opens the new-establishment form as a page on mobile', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const { el } = render();
    el.querySelector<HTMLButtonElement>('.est-header__new')!.click();
    expect(navigate).toHaveBeenCalledWith('/establecimientos/nuevo');
  });
});
