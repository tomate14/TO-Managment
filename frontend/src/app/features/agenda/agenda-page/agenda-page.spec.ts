import { vi } from 'vitest';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { appointmentOf } from '../agenda-test-data';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { AgendaTherapist } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AgendaPage } from './agenda-page';

describe('AgendaPage', () => {
  const therapists: AgendaTherapist[] = [
    { id: 1, fullName: 'Ana Pérez', color: 1, active: true },
    { id: 2, fullName: 'Luz Díaz', color: 2, active: true },
  ];
  let list: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<AgendaPage>;
  let page: AgendaPage;

  function setup(role: 'ADMIN' | 'TERAPEUTA') {
    list = vi.fn().mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list, therapists: () => of(therapists) } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'a@c.com', fullName: 'Ana', role }) } },
      ],
    });
    fixture = TestBed.createComponent(AgendaPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the team subtitle and filter only to admins', () => {
    const el = setup('ADMIN');
    expect(el.textContent).toContain('Turnos de todo el equipo.');
    expect(el.querySelector('.therapist-filter')).toBeTruthy();
  });

  it('shows therapists their own subtitle and no filter', () => {
    const el = setup('TERAPEUTA');
    expect(el.textContent).toContain('Tus turnos.');
    expect(el.querySelector('.therapist-filter')).toBeNull();
  });

  it('loads the visible week', () => {
    setup('TERAPEUTA');
    expect(list).toHaveBeenCalledWith('2026-11-02', '2026-11-08', expect.any(Array));
  });

  it('defaults to the calendar tab', () => {
    const el = setup('ADMIN');
    expect(el.querySelector('[role="tabpanel"]')?.getAttribute('id')).toBe('agenda-panel-calendario');
  });

  it('moves the visible week with the arrows and returns with "Hoy"', () => {
    const el = setup('TERAPEUTA');
    const click = (selector: string) => {
      el.querySelector<HTMLElement>(selector)!.click();
      TestBed.tick();
    };
    click('.calendar-toolbar [aria-label="Semana siguiente"]');
    expect(list).toHaveBeenLastCalledWith('2026-11-09', '2026-11-15', expect.any(Array));
    click('.calendar-toolbar__today');
    expect(list).toHaveBeenLastCalledWith('2026-11-02', '2026-11-08', expect.any(Array));
  });

  it('cancels an appointment, reloads the week and shows an alert on failure', () => {
    const cancel = vi.fn().mockReturnValueOnce(of(undefined)).mockReturnValueOnce(throwError(() => new Error('x')));
    list = vi.fn().mockReturnValue(of([appointmentOf({ id: 5 })]));
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list, cancel, therapists: () => of(therapists) } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'a@c.com', fullName: 'Ana', role: 'TERAPEUTA' }) } },
      ],
    });
    const fixture = TestBed.createComponent(AgendaPage);
    fixture.componentInstance['cancelAppointment'](appointmentOf({ id: 5 }));
    TestBed.tick();
    expect(cancel).toHaveBeenCalledWith(5);
    // Initial load + reload, for both the calendar's week and the "Turnos asignados" week.
    expect(list).toHaveBeenCalledTimes(4);
    fixture.componentInstance['cancelAppointment'](appointmentOf({ id: 5 }));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cancelar el turno. Intentá de nuevo.',
    );
  });

  it('shows nothing and never requests an unfiltered week when an admin deselects every therapist, keeping real counts', () => {
    list = vi.fn().mockReturnValue(of([appointmentOf({ id: 1 }), appointmentOf({ id: 2 })]));
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list, therapists: () => of(therapists) } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'a@c.com', fullName: 'Ana', role: 'ADMIN' }) } },
      ],
    });
    const fixture = TestBed.createComponent(AgendaPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    // Calendar week + current week, both for every visible therapist.
    expect(list).toHaveBeenCalledTimes(2);
    expect(list).toHaveBeenCalledWith('2026-11-02', '2026-11-08', [1, 2]);
    expect(fixture.componentInstance['appointments']()).toHaveLength(2);
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('.therapist-filter input'));
    boxes.forEach((box) => box.click());
    TestBed.tick();
    expect(list).toHaveBeenCalledTimes(2);
    expect(list).not.toHaveBeenCalledWith(expect.any(String), expect.any(String), []);
    expect(fixture.componentInstance['appointments']()).toHaveLength(0);
    expect(fixture.componentInstance['counts']().get(1)).toBe(2);
  });

  it('shows the load error when an admin\'s therapist list fails', () => {
    list = vi.fn().mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list, therapists: () => throwError(() => new Error('x')) } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'a@c.com', fullName: 'Ana', role: 'ADMIN' }) } },
      ],
    });
    const fixture = TestBed.createComponent(AgendaPage);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la agenda.',
    );
  });

  it('keeps "Turnos asignados" on the real current week after the calendar moves', async () => {
    const el = setup('TERAPEUTA');
    list.mockImplementation((from: string) => of(from === '2026-11-02' ? [appointmentOf({ id: 7 })] : []));
    await TestBed.inject(Router).navigate([], { queryParams: { vista: 'turnos' } });
    page['reload']();
    TestBed.tick();
    page['shiftWeek'](1);
    TestBed.tick();
    expect(el.querySelector('#agenda-panel-turnos')).toBeTruthy();
    // The calendar followed the arrow (and loaded another, empty week); the list still holds today's week.
    expect(list).toHaveBeenCalledWith('2026-11-09', '2026-11-15', expect.any(Array));
    expect(page['anchorDate']()).toBe('2026-11-09');
    expect(page['appointments']()).toHaveLength(0);
    fixture.detectChanges();
    expect(el.textContent).toContain('Hoy');
  });

  it('includes the current vista in the mobile navigation', async () => {
    const el = setup('TERAPEUTA');
    const router = TestBed.inject(Router);
    await router.navigate([], { queryParams: { vista: 'turnos' } });
    TestBed.tick();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    el.querySelector<HTMLElement>('.agenda-header__new')!.click();
    expect(navigate).toHaveBeenCalledWith(['/agenda/nuevo'], { queryParams: { vista: 'turnos' } });
  });
});
