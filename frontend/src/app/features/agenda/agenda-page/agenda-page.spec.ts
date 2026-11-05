import { vi } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
    const fixture = TestBed.createComponent(AgendaPage);
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
    expect(list).toHaveBeenCalledTimes(2);
    fixture.componentInstance['cancelAppointment'](appointmentOf({ id: 5 }));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cancelar el turno. Intentá de nuevo.',
    );
  });

  it('does not request anything for an admin who deselects every therapist', () => {
    const el = setup('ADMIN');
    expect(list).toHaveBeenCalledTimes(1);
    expect(list).toHaveBeenCalledWith('2026-11-02', '2026-11-08', [1, 2]);
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('.therapist-filter input'));
    boxes.forEach((box) => box.click());
    TestBed.tick();
    expect(list).toHaveBeenCalledTimes(1);
    expect(list).not.toHaveBeenCalledWith(expect.any(String), expect.any(String), []);
  });
});
