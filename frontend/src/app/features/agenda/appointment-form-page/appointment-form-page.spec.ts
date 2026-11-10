import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { vi } from 'vitest';
import { Appointment } from '../agenda.models';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { appointmentOf } from '../agenda-test-data';
import { AgendaService } from '../agenda.service';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { PatientsService } from '../../patients/patients.service';
import { AppointmentFormPage } from './appointment-form-page';

describe('AppointmentFormPage confirmed appointment', () => {
  it('shows the confirmed card with only Ver ficha when a reschedule URL is opened directly', () => {
    TestBed.configureTestingModule({
      imports: [AppointmentFormPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }), queryParamMap: convertToParamMap({}) } } },
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: {
          therapists: () => of([]),
          get: () => of({ ...appointmentOf({ id: 1 }), confirmedAt: '2026-11-05T12:00:00Z', billed: false }),
          availability: () => of([]),
        } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, fullName: 'Ana', role: 'TERAPEUTA' }) } },
        { provide: DiagnosesService, useValue: { listForPatient: () => of([]) } },
        { provide: PatientsService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-appointment-form')).toBeNull();
    expect(el.textContent).toContain('Confirmado · Pendiente de facturar');
    expect(el.querySelector('.appointment-card__actions')?.textContent?.trim()).toBe('Ver ficha');
  });

  for (const date of ['2026-11-04', '2026-10-29']) {
    it(`confirms calendar details for ${date} with original date, one request and recoverable retry`, () => {
      const original = appointmentOf({ id: 9, date });
      const response = new Subject<Appointment>();
      const confirm = vi.fn().mockReturnValue(response);
      TestBed.configureTestingModule({
        imports: [AppointmentFormPage],
        providers: [
          provideRouter([]),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '9' }), queryParamMap: convertToParamMap({ vista: 'calendario', detalle: '1' }) } } },
          { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
          { provide: AgendaService, useValue: { therapists: () => of([]), get: () => of(original), confirm, availability: () => of([]) } },
          { provide: AuthService, useValue: { currentUser: signal({ id: 1, fullName: 'Ana', role: 'TERAPEUTA' }) } },
          { provide: DiagnosesService, useValue: { listForPatient: () => of([]) } },
          { provide: PatientsService, useValue: {} },
        ],
      });
      const fixture = TestBed.createComponent(AppointmentFormPage);
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      const button = () => el.querySelector<HTMLButtonElement>('[aria-label="Confirmar sesión"]')!;
      expect(button()).toBeTruthy();
      expect(el.querySelector('time')?.getAttribute('datetime')).toBe(date);
      expect(el.querySelector('app-appointment-form')).toBeNull();
      button().click();
      button().click();
      fixture.detectChanges();
      expect(confirm).toHaveBeenCalledExactlyOnceWith(9);
      expect(button().disabled).toBe(true);
      expect(el.querySelector<HTMLButtonElement>('.appointment-card__btn')?.disabled).toBe(true);
      response.error(new Error('offline'));
      fixture.detectChanges();
      expect(el.querySelector('[role="alert"]')).toBeTruthy();
      expect(button().disabled).toBe(false);
      confirm.mockReturnValue(of({ ...original, confirmedAt: '2026-11-05T12:00:00Z' }));
      button().click();
      fixture.detectChanges();
      expect(el.textContent).toContain('Confirmado · Pendiente de facturar');
      expect(el.querySelector('.appointment-card__actions')?.textContent?.trim()).toBe('Ver ficha');
      expect(el.querySelector('time')?.getAttribute('datetime')).toBe(date);
      expect(el.querySelector('app-appointment-form')).toBeNull();
    });
  }
});
