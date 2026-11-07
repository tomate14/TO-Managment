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
    kind: 'INSTITUTION', pendingApproval: false, rejected: false, createdAt: '2026-11-01T15:00:00Z',
    active: true,
    activeUsers: 4,
    admins: [{ id: 10, fullName: 'Mariana Gómez', email: 'mariana.gomez@cnorte.com' }],
  };
  const mano: EstablishmentSummary = {
    id: 2,
    name: 'Clínica de la Mano',
    kind: 'INSTITUTION', pendingApproval: false, rejected: false, createdAt: '2026-11-01T15:00:00Z',
    active: false,
    activeUsers: 3,
    admins: [{ id: 11, fullName: 'Diego Medina', email: 'diego@clinicamano.com' }],
  };
  const ana: EstablishmentSummary = {
    id: 3,
    name: 'Ana Pérez',
    kind: 'INDIVIDUAL',
    pendingApproval: true,
    rejected: false,
    createdAt: '2026-11-06T15:00:00Z',
    active: false,
    activeUsers: 1,
    admins: [{ id: 12, fullName: 'Ana Pérez', email: 'ana.perez@gmail.com' }],
  };
  const julieta: EstablishmentSummary = {
    ...ana,
    id: 4,
    name: 'Julieta Ríos',
    createdAt: '2026-11-05T15:00:00Z',
    admins: [{ id: 13, fullName: 'Julieta Ríos', email: 'julieta.rios@outlook.com' }],
  };
  let service: {
    list: ReturnType<typeof vi.fn>;
    rename: ReturnType<typeof vi.fn>;
    setActive: ReturnType<typeof vi.fn>;
    approve: ReturnType<typeof vi.fn>;
    setPendingCount: ReturnType<typeof vi.fn>;
  };
  let auth: { enterEstablishment: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    service = { list: vi.fn(() => of([norte, mano])), rename: vi.fn(), setActive: vi.fn(), approve: vi.fn(), setPendingCount: vi.fn() };
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

  describe('pending practices', () => {
    beforeEach(() => service.list.mockReturnValue(of([ana, julieta, norte])));

    function request(el: HTMLElement, name: string): HTMLElement {
      return Array.from(el.querySelectorAll<HTMLElement>('.est-request')).find((c) => c.textContent?.includes(name))!;
    }

    function button(root: HTMLElement, label: string): HTMLButtonElement {
      return Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.trim() === label)!;
    }

    it('renders the pending section only when there are pending practices', () => {
      const { el } = render();
      expect(el.querySelector('.est-pending')!.textContent).toContain('Pendientes de aprobación · 2');
      expect(request(el, 'Ana Pérez').textContent).toContain('ana.perez@gmail.com');
      expect(request(el, 'Ana Pérez').textContent).toMatch(/pidió el \d{2}\/\d{2}\/2026/);
      expect(service.setPendingCount).toHaveBeenCalledWith(2);

      service.list.mockReturnValue(of([norte, mano]));
      const { el: plain } = render();
      expect(plain.querySelector('.est-pending')).toBeNull();
      expect(service.setPendingCount).toHaveBeenLastCalledWith(0);
    });

    it('approves a request and moves it to the list', () => {
      service.approve.mockReturnValue(of({ ...ana, pendingApproval: false, active: true }));
      const { fixture, el } = render();
      button(request(el, 'Ana Pérez'), 'Aprobar').click();
      fixture.detectChanges();
      expect(service.approve).toHaveBeenCalledWith(3);
      expect(el.querySelector('.est-pending')!.textContent).toContain('Pendientes de aprobación · 1');
      expect(el.querySelectorAll('.est-request').length).toBe(1);
      expect(card(el, 'Ana Pérez').textContent).toContain('Independiente');
      expect(card(el, 'Ana Pérez').textContent).toContain('Activo');
      expect(service.setPendingCount).toHaveBeenLastCalledWith(1);
    });

    it('asks before rejecting', () => {
      service.setActive.mockReturnValue(of({ ...julieta, pendingApproval: false, rejected: true }));
      const { fixture, el } = render();
      button(request(el, 'Julieta Ríos'), 'Rechazar').click();
      fixture.detectChanges();
      expect(request(el, 'Julieta Ríos').textContent).toContain('¿Rechazar la solicitud? No va a poder ingresar.');
      expect(service.setActive).not.toHaveBeenCalled();

      button(request(el, 'Julieta Ríos'), 'Cancelar').click();
      fixture.detectChanges();
      expect(request(el, 'Julieta Ríos').textContent).not.toContain('¿Rechazar la solicitud?');

      button(request(el, 'Julieta Ríos'), 'Rechazar').click();
      fixture.detectChanges();
      button(request(el, 'Julieta Ríos'), 'Sí, rechazar').click();
      fixture.detectChanges();
      expect(service.setActive).toHaveBeenCalledWith(4, false);
      expect(card(el, 'Julieta Ríos').textContent).toContain('Rechazada');
    });

    it('shows the Independiente badge and the Rechazada state in the list', () => {
      service.list.mockReturnValue(of([{ ...julieta, pendingApproval: false, rejected: true }, norte]));
      const { el } = render();
      const rejected = card(el, 'Julieta Ríos');
      expect(rejected.textContent).toContain('Independiente');
      expect(rejected.textContent).toContain('Rechazada');
      expect(rejected.querySelector('.est-enter')).toBeNull();
      expect(card(el, 'Consultorio Norte').textContent).not.toContain('Independiente');
    });
  });
});
